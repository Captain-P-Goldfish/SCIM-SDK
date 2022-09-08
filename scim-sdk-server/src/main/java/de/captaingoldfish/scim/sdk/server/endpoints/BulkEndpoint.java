package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseGetOperation;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.GetResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.response.UpdateResponse;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.bulkget.BulkGetResolver;
import de.captaingoldfish.scim.sdk.server.endpoints.bulkid.BulkIdResolver;
import de.captaingoldfish.scim.sdk.server.endpoints.bulkid.BulkIdResolverAbstract;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.schemas.SchemaFactory;
import de.captaingoldfish.scim.sdk.server.schemas.validation.RequestSchemaValidator;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import de.captaingoldfish.scim.sdk.server.utils.UriInfos;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 07.11.2019 - 23:48 <br>
 * <br>
 * the bulk endpoint implementation
 */
@Slf4j
class BulkEndpoint
{

  /**
   * the resource endpoint that is triggered for each bulk-operation
   */
  private final ResourceEndpoint resourceEndpoint;

  /**
   * the service provider configuration to check if the bulk-request meets the requirements of the configuration
   */
  @Getter
  private final ServiceProvider serviceProvider;

  /**
   * necessary to get access to the resource-types
   */
  @Getter
  private final ResourceTypeFactory resourceTypeFactory;

  /**
   * arbitary code that is executed before the endpoint is called. This might be used to execute authentication
   * on dedicated resource types
   */
  private final Consumer<ResourceType> doBeforeExecution;

  private final Map<String, String> originalHttpHeaders;

  private final Map<String, String> originalQueryParams;

  /**
   * this implementation is used to modify the bulk-operations by replacing the bulkId references for the ids of
   * the actual references resource after the resource was created
   */
  private final BulkIdResolver bulkIdResolver = new BulkIdResolver();

  public BulkEndpoint(ResourceEndpoint resourceEndpoint,
                      ServiceProvider serviceProvider,
                      ResourceTypeFactory resourceTypeFactory,
                      Map<String, String> originalHttpHeaders,
                      Map<String, String> originalQueryParams,
                      Consumer<ResourceType> doBeforeExecution)
  {
    this.resourceEndpoint = resourceEndpoint;
    this.serviceProvider = serviceProvider;
    this.resourceTypeFactory = resourceTypeFactory;
    this.originalHttpHeaders = originalHttpHeaders;
    this.originalQueryParams = originalQueryParams;
    this.doBeforeExecution = doBeforeExecution;
  }

  /**
   * resolves a bulk request
   *
   * @param requestBody the bulk request body
   * @param context the current context of the request that might hold authorization details and other context
   *          based information
   * @return the response of the bulk request
   */
  public BulkResponse bulk(String baseUri, String requestBody, Context context)
  {
    BulkRequest bulkRequest = parseAndValidateBulkRequest(requestBody);
    List<BulkRequestOperation> operations = bulkRequest.getBulkRequestOperations();
    List<BulkResponseOperation> responseOperations = new ArrayList<>();
    final int failOnErrors = RequestUtils.getEffectiveFailOnErrors(bulkRequest);
    int httpStatus = handleBulkOperationList(baseUri, operations, responseOperations, failOnErrors, context);
    return BulkResponse.builder().httpStatus(httpStatus).bulkResponseOperation(responseOperations).build();
  }

  /**
   * handles a list of bulk request operations and will verify that the failOnErrors value is not exceeded
   *
   * @param baseUri the base uri of all SCIM endpoints
   * @param operations the list of request operations
   * @param responseOperations a predefined list of response operations that will get its elements from this
   *          method
   * @param failOnErrors the failOnErrors value that must not be exceeded
   * @param context the current context of the request that might hold authorization details and other context
   *          based information
   * @return the http status code of the response
   */
  private int handleBulkOperationList(String baseUri,
                                      List<BulkRequestOperation> operations,
                                      List<BulkResponseOperation> responseOperations,
                                      int failOnErrors,
                                      Context context)
  {
    int errorCounter = 0;
    // this is a security switch in case a bad crafted bulk request will end in an infinite loop this switch is
    // used to break the infinite loop
    long maxIterations = serviceProvider.getBulkConfig().getMaxOperations() * 2L;
    int iterations = 0;
    while (!operations.isEmpty())
    {
      if (iterations >= maxIterations)
      {
        break;
      }
      iterations++;
      BulkRequestOperation requestOperation = operations.get(0);
      if (errorCounter >= failOnErrors)
      {
        // The service provider stops processing the bulk operation and immediately returns a response to the client
        break;
      }
      try
      {
        validateOperation(requestOperation);
      }
      catch (BadRequestException ex)
      {
        errorCounter++;
        BulkResponseOperation.BulkResponseOperationBuilder responseBuilder = BulkResponseOperation.builder();
        responseOperations.add(responseBuilder.status(ex.getStatus()).response(new ErrorResponse(ex)).build());
        operations.remove(0);
        continue;
      }
      BulkResponseOperation bulkResponseOperation = handleSingleBulkOperation(baseUri, requestOperation, context);
      if (bulkResponseOperation == null)
      {
        // mark this operation as already handled once
        String operationIdentifier = UUID.randomUUID().toString();
        requestOperation.setUniqueIdentifier(operationIdentifier);
        // the bulk operation references another operation that was not resolved yet, so we move the operation for
        // another run to the end of the line
        operations.remove(0);
        operations.add(requestOperation);
        continue;
      }
      else
      {
        operations.remove(0);
      }
      boolean isSuccessfulResponseCode = isSuccessResponseCode(requestOperation, bulkResponseOperation);
      if (!isSuccessfulResponseCode)
      {
        errorCounter++;
      }
      responseOperations.add(bulkResponseOperation);
    }
    int httpStatus = HttpStatus.OK;
    if (errorCounter >= failOnErrors)
    {
      // The service returns an appropriate response status code if too many errors occurred
      httpStatus = HttpStatus.PRECONDITION_FAILED;
    }
    else if (iterations >= maxIterations)
    {
      httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return httpStatus;
  }

  /**
   * verifies that the correct response code is returned based on the given http method
   *
   * @param requestOperation the operation that contains the method to execute
   * @param responseOperation the response operation that contains the result of the processed operation
   * @return true if the success-response code was returned, false else
   */
  private boolean isSuccessResponseCode(BulkRequestOperation requestOperation, BulkResponseOperation responseOperation)
  {
    switch (requestOperation.getMethod())
    {
      case POST:
        return HttpStatus.CREATED == responseOperation.getStatus();
      case DELETE:
        return HttpStatus.NO_CONTENT == responseOperation.getStatus();
      default:
        return HttpStatus.OK == responseOperation.getStatus();
    }
  }

  /**
   * this method handles a single bulk request operation and will also resolve bulkIds if such references do
   * exist in the request
   *
   * @param baseUri the base uri of all SCIM endpoints
   * @param operation the operation that should be handled
   * @param context the current context of the request that might hold authorization details and other context
   *          based information
   * @return the response for the single bulk request
   */
  private BulkResponseOperation handleSingleBulkOperation(String baseUri,
                                                          BulkRequestOperation operation,
                                                          Context context)
  {
    HttpMethod httpMethod = operation.getMethod();
    Map<String, String> httpHeaders = getHttpHeadersForBulk(operation);
    UriInfos operationUriInfo = UriInfos.getRequestUrlInfos(getResourceTypeFactory(),
                                                            baseUri + operation.getPath(),
                                                            httpMethod,
                                                            httpHeaders);
    operationUriInfo.getQueryParameters().putAll(originalQueryParams);
    String id = Optional.ofNullable(operationUriInfo.getResourceId()).map(resourceId -> "/" + resourceId).orElse("");
    String location = baseUri + operationUriInfo.getResourceEndpoint() + id;
    String bulkId = operation.getBulkId().orElse(null);
    BulkResponseOperation.BulkResponseOperationBuilder responseBuilder = BulkResponseOperation.builder()
                                                                                              .bulkId(bulkId)
                                                                                              .method(httpMethod)
                                                                                              .location(location);
    try
    {
      // this method call will modify the data within the operation-object if it contains bulkId-references
      Optional<BulkIdResolverAbstract> bulkIdResolver = resolveBulkIds(operation, httpMethod, operationUriInfo, bulkId);
      // override the operation uri infos in case that a bulkId within the uri was resolved
      operationUriInfo = bulkIdResolver.map(BulkIdResolverAbstract::getUriInfos).orElse(operationUriInfo);
      if (bulkIdResolver.map(BulkIdResolverAbstract::hasAnyBulkIdReferences).orElse(false))
      {
        // will cause this operation to be moved to the end of the request operations list. This happens if the
        // operation contains a bulkId-reference that has not been resolved yet
        return null;
      }
    }
    catch (ScimException ex)
    {
      return responseBuilder.status(ex.getStatus()).response(new ErrorResponse(ex)).build();
    }
    responseBuilder.bulkId(operation.getBulkId().orElse(null)).method(operation.getMethod()).location(location);
    ScimResponse scimResponse = resourceEndpoint.resolveRequest(httpMethod,
                                                                operation.getData().orElse(null),
                                                                operationUriInfo,
                                                                doBeforeExecution,
                                                                context);
    boolean isResourceResponse = scimResponse instanceof CreateResponse || scimResponse instanceof UpdateResponse;
    if (isResourceResponse)
    {
      Optional.ofNullable(scimResponse.get(AttributeNames.RFC7643.ID))
              .map(JsonNode::textValue)
              .ifPresent(resourceId -> bulkIdResolver.addResolvedBulkId(bulkId, resourceId));
    }
    final boolean isErrorResponse = ErrorResponse.class.isAssignableFrom(scimResponse.getClass());
    responseBuilder.status(scimResponse.getHttpStatus());
    if (isErrorResponse)
    {
      responseBuilder.response(scimResponse);
    }
    else
    {
      // if we are currently processing a bulk-get response we need to resolve the transitive resources
      if (scimResponse instanceof GetResponse && serviceProvider.getBulkConfig().isSupportBulkGet())
      {
        // get all transitive children of the retrieved resource
        final String resourceTypeName = operationUriInfo.getResourceType().getName();
        BiFunction<String, ResourceType, ScimResponse> bulkGetOpCaller = getTransitiveBulkGetResolver(baseUri,
                                                                                                      httpHeaders,
                                                                                                      context);
        BulkGetResolver bulkGetResolver = BulkGetResolver.builder()
                                                         .maxResourceLevel(operation.getMaxResourceLevel())
                                                         .parentResourceResponse(scimResponse)
                                                         .resourceTypeFactory(resourceTypeFactory)
                                                         .resourceType(operationUriInfo.getResourceType())
                                                         .callResourceEndpoint(bulkGetOpCaller)
                                                         .build();
        List<BulkResponseGetOperation> children = bulkGetResolver.getTransitiveResources();
        BulkResponseGetOperation bulkResponseGetOperation = BulkResponseGetOperation.builder()
                                                                                    .status(HttpStatus.OK)
                                                                                    .resourceId(operationUriInfo.getResourceId())
                                                                                    .resourceType(resourceTypeName)
                                                                                    .resource(scimResponse)
                                                                                    .children(children)
                                                                                    .build();
        responseBuilder.response(bulkResponseGetOperation);
      }
      else
      {
        addResponse(operation, scimResponse, operationUriInfo.getResourceType(), responseBuilder);
      }
    }

    if (isErrorResponse)
    {
      if (HttpMethod.POST.equals(operation.getMethod()))
      {
        // A "location" attribute that includes the resource's endpoint MUST be returned for all operations
        // except for failed POST operations (which have no location)
        responseBuilder.location(null);
      }
    }
    else
    {
      id = Optional.ofNullable(scimResponse.get(AttributeNames.RFC7643.ID))
                   .map(jsonNode -> "/" + jsonNode.textValue())
                   .orElse("");
      location = baseUri + operationUriInfo.getResourceEndpoint() + id;
      responseBuilder.location(location);
      final boolean isResourceDeleted = HttpMethod.DELETE.equals(operationUriInfo.getHttpMethod());
      if (isResourceDeleted)
      {
        responseBuilder.resourceId(operationUriInfo.getResourceId());
      }
      else
      {
        final ETag resourceVersion = Optional.ofNullable(scimResponse.get(AttributeNames.RFC7643.META))
                                             .map(JsonNode::toString)
                                             .map(metaResource -> {
                                               Meta meta = JsonHelper.readJsonDocument(metaResource, Meta.class);
                                               return meta.getVersion().orElse(null);
                                             })
                                             .orElse(null);
        responseBuilder.version(resourceVersion);
        final String resourceId = Optional.ofNullable(scimResponse.get(AttributeNames.RFC7643.ID))
                                          .map(JsonNode::textValue)
                                          .orElse(null);
        responseBuilder.resourceId(resourceId);
      }
    }
    return responseBuilder.build();
  }

  /**
   * calls the resource endpoint with a get call for the bulk-get-feature. This call will be executed for each
   * child resource that is being extracted
   *
   * @param baseUri the base uri of this server
   * @param httpHeaders the http headers from the current request
   * @param context the current request context
   * @return a function to call the {@link ResourceEndpoint} from {@link BulkGetResolver}
   */
  private BiFunction<String, ResourceType, ScimResponse> getTransitiveBulkGetResolver(String baseUri,
                                                                                      Map<String, String> httpHeaders,
                                                                                      Context context)
  {
    return (resourceId, resourceType) -> {
      UriInfos uriInfos = UriInfos.getRequestUrlInfos(getResourceTypeFactory(),
                                                      String.format("%s%s/%s",
                                                                    baseUri,
                                                                    resourceType.getEndpoint(),
                                                                    resourceId),
                                                      HttpMethod.GET,
                                                      httpHeaders);
      return resourceEndpoint.resolveRequest(uriInfos.getHttpMethod(), null, uriInfos, doBeforeExecution, context);
    };
  }

  /**
   * this method will try to resolve all currently resolved bulkIds within the given bulk-request-operation
   *
   * @param operation the bulk request operation that might contain some bulkId references
   * @param httpMethod the http method that will let us know which type of bulk-request we need to handle
   * @param operationUriInfo the url information of the current request operation. This object might also
   *          contain a bulkId-reference within the url
   * @param bulkId the bulkId-reference of the bulk-request-operation
   * @return true if all bulkId-references of the operation could be resolved, false else
   */
  private Optional<BulkIdResolverAbstract> resolveBulkIds(BulkRequestOperation operation,
                                                          HttpMethod httpMethod,
                                                          UriInfos operationUriInfo,
                                                          String bulkId)
  {
    if (bulkIdResolver.isDuplicateBulkId(bulkId))
    {
      throw new BadRequestException(String.format("Found duplicate %s '%s' in bulk request operations",
                                                  AttributeNames.RFC7643.BULK_ID,
                                                  bulkId));
    }

    if (HttpMethod.DELETE.equals(httpMethod))
    {
      return Optional.empty();
    }

    BulkIdResolverAbstract resolverForBulkIds = bulkIdResolver.getBulkIdResolver(bulkId).orElseGet(() -> {
      return bulkIdResolver.createNewBulkIdResolver(bulkId, operationUriInfo, operation.getData().orElse("{}"));
    });
    boolean allBulkIdReferencesResolved = !resolverForBulkIds.hasAnyBulkIdReferences();
    // the BulkIdResolverAbstract contains the modified data with the resolved bulkIds
    if (allBulkIdReferencesResolved)
    {
      operation.setData(resolverForBulkIds.getResource().toString());
    }

    boolean hadSuccessInLastRun = resolverForBulkIds.isHadSuccessInLastRun();
    boolean isSecondTryToResolveIds = operation.getUniqueIdentifier() != null;
    if (isSecondTryToResolveIds && !allBulkIdReferencesResolved && !hadSuccessInLastRun)
    {
      String unresolvedBulkIds = (String)resolverForBulkIds.getUnresolvedBulkIds()
                                                           .stream()
                                                           .map(id -> String.format("%s:%s",
                                                                                    AttributeNames.RFC7643.BULK_ID,
                                                                                    id))
                                                           .collect(Collectors.joining(", "));
      throw new ConflictException(String.format("the operation failed because the following "
                                                + "bulkId-references could not be resolved [%s]",
                                                unresolvedBulkIds));
    }
    return Optional.of(resolverForBulkIds);
  }

  /**
   * adds the response to the bulk response builder if allowed based on the service-provider and resource type
   * configuration
   *
   * @param operation the request operation that might have an attribute that tells us that the user wants the
   *          resource to be returned
   * @param scimResponse the response object from the {@link ResourceEndpointHandler}
   * @param resourceType the specific configuration for the current resource type
   * @param responseBuilder the response object that is being extended by the response if the configuration
   *          allows it
   */
  private void addResponse(BulkRequestOperation operation,
                           ScimResponse scimResponse,
                           ResourceType resourceType,
                           BulkResponseOperation.BulkResponseOperationBuilder responseBuilder)
  {

    if (!serviceProvider.getBulkConfig().isReturnResourcesEnabled())
    {
      return;
    }

    if (resourceType.getFeatures().isDenyReturnResourcesOnBulk())
    {
      return;
    }

    final boolean returnResourceByDefault = serviceProvider.getBulkConfig().isReturnResourcesByDefault();
    final boolean doesClientWantResourceBack = operation.isReturnResource().orElse(returnResourceByDefault);
    if (doesClientWantResourceBack)
    {
      responseBuilder.response(scimResponse);
    }
  }

  /**
   * gets the http headers for a single bulk request operation. This method is explicitly used to add the
   * etags-value into the http request headers if present within the request
   *
   * @param operation the current operation for which the request headers should be built
   * @return a map with the necessary http request headers
   */
  private Map<String, String> getHttpHeadersForBulk(BulkRequestOperation operation)
  {
    Map<String, String> httpHeaders = new HashMap<>(originalHttpHeaders);
    httpHeaders.put(EndpointPaths.BULK, "true");
    operation.getVersion().ifPresent(eTag -> httpHeaders.put(HttpHeader.IF_MATCH_HEADER, eTag.getEntityTag()));
    return httpHeaders;
  }

  /**
   * tries to parse the bulk request and validates it eventually
   *
   * @param requestBody the request body that shall represent the bulk request
   * @return the parsed bulk request
   */
  private BulkRequest parseAndValidateBulkRequest(String requestBody)
  {
    BulkConfig bulkConfig = getServiceProvider().getBulkConfig();
    if (!bulkConfig.isSupported())
    {
      throw new NotImplementedException("bulk is not supported by this service provider");
    }
    try
    {
      JsonNode jsonNode = JsonHelper.readJsonDocument(requestBody);
      SchemaFactory schemaFactory = getResourceTypeFactory().getSchemaFactory();
      Schema bulkRequestSchema = schemaFactory.getMetaSchema(SchemaUris.BULK_REQUEST_URI);
      JsonNode validatedRequest = new RequestSchemaValidator(serviceProvider, ScimObjectNode.class,
                                                             HttpMethod.POST).validateDocument(bulkRequestSchema,
                                                                                               jsonNode);
      BulkRequest bulkRequest = JsonHelper.copyResourceToObject(validatedRequest, BulkRequest.class);
      if (bulkConfig.getMaxOperations() < bulkRequest.getBulkRequestOperations().size())
      {
        throw new BadRequestException("too many operations maximum number of operations is '"
                                      + bulkConfig.getMaxOperations() + "' but got '"
                                      + bulkRequest.getBulkRequestOperations().size() + "'", null,
                                      ScimType.RFC7644.TOO_MANY);
      }
      if (bulkConfig.getMaxPayloadSize() < requestBody.getBytes().length)
      {
        throw new BadRequestException("request body too large with '" + requestBody.getBytes().length
                                      + "'-bytes maximum payload size is '" + bulkConfig.getMaxPayloadSize() + "'",
                                      null, ScimType.Custom.TOO_LARGE);
      }
      return bulkRequest;
    }
    catch (ScimException ex)
    {
      throw new BadRequestException(ex.getMessage(), ex, ScimType.Custom.UNPARSEABLE_REQUEST);
    }
  }

  /**
   * verifies that the bulk operation is valid<br>
   * <br>
   * e.g. not all http methods are allowed on the bulk endpoint
   *
   * <pre>
   *    The body of a bulk operation contains a set of HTTP resource operations
   *    using one of the HTTP methods supported by the API, i.e., POST, PUT,
   *    PATCH, or DELETE.
   * </pre>
   *
   * @param operation the operation to validate
   */
  private void validateOperation(BulkRequestOperation operation)
  {
    List<HttpMethod> validMethods = new ArrayList<>(Arrays.asList(HttpMethod.POST,
                                                                  HttpMethod.PUT,
                                                                  HttpMethod.PATCH,
                                                                  HttpMethod.DELETE));
    if (serviceProvider.getBulkConfig().isSupportBulkGet())
    {
      validMethods.add(HttpMethod.GET);
    }
    if (!validMethods.contains(operation.getMethod()))
    {
      throw new BadRequestException("bulk request used invalid http method. Only the following methods are allowed "
                                    + "for bulk: " + validMethods, null, ScimType.Custom.UNPARSEABLE_REQUEST);
    }
    if (HttpMethod.POST.equals(operation.getMethod())
        && (operation.getBulkId().isPresent() && StringUtils.isBlank(operation.getBulkId().get())
            || !operation.getBulkId().isPresent()))
    {
      throw new BadRequestException("missing 'bulkId' on BULK-POST request", null, ScimType.Custom.UNPARSEABLE_REQUEST);
    }
  }
}
