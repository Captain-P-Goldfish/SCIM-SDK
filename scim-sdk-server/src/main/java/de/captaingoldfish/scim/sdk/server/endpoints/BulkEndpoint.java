package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

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
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
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

  /**
   * this map is used to map the ids of newly created resources to bulkIds
   */
  private final Map<String, String> resolvedBulkIds = new HashMap<>();

  /**
   * this map is used to detect circular references by storing bulkId references within it
   */
  private final Map<String, Set<String>> circularReferenceDetectorMap = new HashMap<>();

  private final Map<String, String> originalHttpHeaders;

  private final Map<String, String> originalQueryParams;

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
      BulkRequestOperation operation = operations.get(0);
      if (errorCounter >= failOnErrors)
      {
        // The service provider stops processing the bulk operation and immediately returns a response to the client
        break;
      }
      try
      {
        validateOperation(operation);
      }
      catch (BadRequestException ex)
      {
        errorCounter++;
        BulkResponseOperation.BulkResponseOperationBuilder responseBuilder = BulkResponseOperation.builder();
        responseOperations.add(responseBuilder.status(ex.getStatus()).response(new ErrorResponse(ex)).build());
        operations.remove(0);
        continue;
      }
      BulkResponseOperation bulkResponseOperation = handleSingleBulkOperation(baseUri, operation, context);
      if (bulkResponseOperation == null)
      {
        // mark this operation as already handled once
        String operationIdentifier = UUID.randomUUID().toString();
        operation.setUniqueIdentifier(operationIdentifier);
        // the bulk operation references another operation that was not resolved yet so we move the operation for
        // another run to the end of the line
        operations.remove(0);
        operations.add(operation);
        continue;
      }
      else
      {
        operations.remove(0);
      }
      if (bulkResponseOperation.getResponse().isPresent())
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
      if (!resolveBulkIds(operation, operationUriInfo.getResourceType())
          || !resolveBulkIdInResourceId(operation, operationUriInfo))
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
    final boolean isErrorResponse = ErrorResponse.class.isAssignableFrom(scimResponse.getClass());
    responseBuilder.status(scimResponse.getHttpStatus()).response(isErrorResponse ? (ErrorResponse)scimResponse : null);

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
    if (StringUtils.isNotBlank(id) && operation.getBulkId().isPresent())
    {
      resolvedBulkIds.put(operation.getBulkId().get(), id.substring(1));
    }
    return responseBuilder.build();
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
   * resolves a bulkId within the resourceUri or throws an exception if not present
   *
   * @param operationUriInfo the operation-uri-info that may contain a bulkId-reference within the resource uri
   * @return false if a bulkId-reference exists that could not be resolved, true else
   */
  private boolean resolveBulkIdInResourceId(BulkRequestOperation operation, UriInfos operationUriInfo)
  {
    switch (operationUriInfo.getHttpMethod())
    {
      case PUT:
      case PATCH:
      case DELETE:
        if (StringUtils.startsWithIgnoreCase(operationUriInfo.getResourceId(), AttributeNames.RFC7643.BULK_ID + ":"))
        {
          String resourceId = operationUriInfo.getResourceId();
          String[] bulkIdParts = resourceId.split(":");
          if (bulkIdParts.length != 2)
          {
            throw new BadRequestException("the value '" + resourceId + "' is not a valid bulkId reference", null,
                                          ScimType.RFC7644.INVALID_VALUE);
          }
          String bulkId = bulkIdParts[1];
          String resolvedId = resolvedBulkIds.get(bulkId);
          if (StringUtils.isBlank(resolvedId))
          {
            if (StringUtils.isNotBlank(operation.getUniqueIdentifier()))
            {
              throw new BadRequestException("the operation could not be resolved because the following bulkId-"
                                            + "reference could not be resolved" + " '" + resourceId + "'", null,
                                            ScimType.RFC7644.INVALID_VALUE);
            }
            return false;
          }
          else
          {
            operationUriInfo.setResourceId(resolvedId);
          }
        }
        break;
      default:
        // do nothing
    }
    return true;
  }

  /**
   * this method checks if bulkId references have been used within the bulk request operation and will try to
   * resolve them if possible
   *
   * @param operation the request operation that may contain bulkId references
   * @param resourceType the resource type of the current resource to check
   * @return true if the bulkId was successfully resolved or no bulkIds have been present, false if a bulkId
   *         reference was found but could not be resolved
   */
  private boolean resolveBulkIds(BulkRequestOperation operation, ResourceType resourceType)
  {
    List<JsonNode> bulkIdNodes;
    String resourceData = operation.getData().orElse(null);
    ScimObjectNode resource = null;
    String bulkId = operation.getBulkId().orElse(null);
    if (!HttpMethod.PATCH.equals(operation.getMethod()))
    {
      if (StringUtils.isBlank(resourceData))
      {
        return true;
      }
      resource = JsonHelper.readJsonDocument(resourceData, ScimObjectNode.class);
      bulkIdNodes = getBulkIdNodes(resource, resourceType);
      checkForSelfOrCircularReference(bulkId, bulkIdNodes);
      bulkIdNodes = setBulkIds(bulkIdNodes);
    }
    else
    {
      PatchOpRequest patchOpRequest = JsonHelper.readJsonDocument(operation.getData().orElse(null),
                                                                  PatchOpRequest.class);
      bulkIdNodes = getBulkIdNodesForPatch(patchOpRequest, resourceType, bulkId);
      checkForSelfOrCircularReference(bulkId, bulkIdNodes);
      resource = patchOpRequest;
    }
    operation.setData(resource == null ? null : resource.toString());
    if (bulkIdNodes.isEmpty())
    {
      return true;
    }
    else
    {
      if (StringUtils.isNotBlank(operation.getUniqueIdentifier()))
      {
        throw new BadRequestException("the operation could not be resolved because the following bulkId-"
                                      + "references could not be resolved" + " '"
                                      + bulkIdNodes.stream()
                                                   .map(jsonNode -> jsonNode.get(AttributeNames.RFC7643.VALUE)
                                                                            .textValue())
                                                   .collect(Collectors.joining(", "))
                                      + "'", null, ScimType.RFC7644.INVALID_VALUE);
      }
      return false;
    }
  }

  /**
   * will try to get the nodes within a patch request operation that do contain bulkId-references
   *
   * @param patchOpRequest the patch request operation that may contain bulkId-references
   * @param resourceType the resource type that describes the resource that should be patched
   * @param bulkId the bulkId of the bulk operation that represents the given patch request
   * @return a list of the nodes the contain bulkId-references within the patch request operation
   */
  private List<JsonNode> getBulkIdNodesForPatch(PatchOpRequest patchOpRequest, ResourceType resourceType, String bulkId)
  {
    List<PatchRequestOperation> operationList = patchOpRequest.getOperations();
    for ( PatchRequestOperation operation : operationList )
    {
      switch (operation.getOp())
      {
        case ADD:
        case REPLACE:
          if (operation.getPath().isPresent())
          {
            return getBulkIdsForPatchWithPath(resourceType, operation);
          }
          else
          {
            return getBulkIdsForPatchAddOnResource(operation, resourceType, bulkId);
          }
        default:
          // do nothing
      }
    }
    return Collections.emptyList();
  }

  /**
   * this method handles a patch request operation that contains a path-value which changes the way the
   * values-attribute within the patch request operation must be handled
   *
   * @param resourceType the resource type that describes the resource that should be patched
   * @param operation the patch request operation
   * @return the nodes the contain a bulkId reference
   */
  private List<JsonNode> getBulkIdsForPatchWithPath(ResourceType resourceType, PatchRequestOperation operation)
  {
    boolean containsBulkId = operation.getValues().stream().anyMatch(s -> {
      return StringUtils.containsIgnoreCase(s, AttributeNames.RFC7643.BULK_ID + ":");
    });
    if (containsBulkId)
    {
      AttributePathRoot pathRoot = RequestUtils.parsePatchPath(resourceType, operation.getPath().get());
      String attributeName = pathRoot.getFullName()
                             + (pathRoot.getSubAttributeName() == null ? "" : "." + pathRoot.getSubAttributeName());
      SchemaAttribute attribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, attributeName);
      if (attribute.getSchema()
                   .getBulkIdCandidates()
                   .contains(attribute.getParent() == null ? attribute : attribute.getParent()))
      {
        List<JsonNode> unresolvedAttributes = resolvePatchValuesWithPath(attributeName, operation);
        return unresolvedAttributes;
      }
      else
      {
        return Collections.emptyList();
      }
    }
    else
    {
      return Collections.emptyList();
    }
  }

  /**
   * gets the nodes that contain bulkId references in a patch request operation that has a path-attribute
   *
   * @param attributeName the fully qualified attribute name that is referenced e.g.: "manager" or
   *          "manager.value"
   * @param operation the patch request operation
   * @return the nodes the contain a bulkId reference
   */
  private List<JsonNode> resolvePatchValuesWithPath(String attributeName, PatchRequestOperation operation)
  {
    String[] attributeNameParts = attributeName.split("\\.");
    if (attributeNameParts.length == 2)
    {
      return resolvePatchValuesWithPathForSimpleValue(attributeNameParts[1], operation);
    }
    else
    {
      return resolvePatchValuesWithPathForComplexValue(operation);
    }
  }

  /**
   * gets the nodes that contain bulkId references in a patch request that has a path value with a
   * complex-attribute reference like "manager" or "members"
   *
   * @param operation the patch request operation
   * @return the nodes the contain a bulkId reference
   */
  private List<JsonNode> resolvePatchValuesWithPathForComplexValue(PatchRequestOperation operation)
  {
    List<String> bulkIdValues = operation.getValues().stream().filter(s -> {
      return StringUtils.containsIgnoreCase(s, AttributeNames.RFC7643.BULK_ID + ":");
    }).collect(Collectors.toList());
    List<String> newValuesList = operation.getValues().stream().filter(s -> {
      return !StringUtils.containsIgnoreCase(s, AttributeNames.RFC7643.BULK_ID + ":");
    }).collect(Collectors.toList());
    List<JsonNode> unresolvedValues = new ArrayList<>();
    for ( String complexNode : bulkIdValues )
    {
      ScimObjectNode scimObjectNode = JsonHelper.readJsonDocument(complexNode, ScimObjectNode.class);
      JsonNode valueNode = scimObjectNode.get(AttributeNames.RFC7643.VALUE);
      String bulkId = valueNode.textValue().replaceFirst("(?i)^" + AttributeNames.RFC7643.BULK_ID + ":", "");
      String resolvedResourceId = resolvedBulkIds.get(bulkId);
      if (StringUtils.isBlank(resolvedResourceId))
      {
        unresolvedValues.add(scimObjectNode);
        newValuesList.add(complexNode);
      }
      else
      {
        scimObjectNode.set(AttributeNames.RFC7643.VALUE, new TextNode(resolvedResourceId));
        newValuesList.add(scimObjectNode.toString());
      }
    }
    operation.setValues(newValuesList);
    return unresolvedValues;
  }

  /**
   * gets the nodes that contain bulkId references in a patch request that has a path value with a
   * simple-attribute reference like "manager.value" or "members.value"
   *
   * @param operation the patch request operation
   * @return the nodes the contain a bulkId reference
   */
  private List<JsonNode> resolvePatchValuesWithPathForSimpleValue(String attributeNamePart,
                                                                  PatchRequestOperation operation)
  {
    List<String> bulkIdValues = operation.getValues().stream().filter(s -> {
      return StringUtils.startsWithIgnoreCase(s, AttributeNames.RFC7643.BULK_ID + ":");
    }).collect(Collectors.toList());
    List<String> newValuesList = operation.getValues().stream().filter(s -> {
      return !StringUtils.startsWithIgnoreCase(s, AttributeNames.RFC7643.BULK_ID + ":");
    }).collect(Collectors.toList());
    List<JsonNode> unresolvedValues = new ArrayList<>();
    for ( String bulkIdValue : bulkIdValues )
    {
      String bulkId = bulkIdValue.replaceFirst("(?i)^" + AttributeNames.RFC7643.BULK_ID + ":", "");
      String resolvedResourceId = resolvedBulkIds.get(bulkId);
      if (StringUtils.isBlank(resolvedResourceId))
      {
        ScimObjectNode scimObjectNode = new ScimObjectNode();
        scimObjectNode.set(attributeNamePart, new TextNode(bulkIdValue));
        unresolvedValues.add(scimObjectNode);
        newValuesList.add(bulkIdValue);
      }
      else
      {
        newValuesList.add(resolvedResourceId);
      }
    }
    operation.setValues(newValuesList);
    return unresolvedValues;
  }

  /**
   * gets the nodes from a patch request that does not have a path value which means that the value of the patch
   * request operation must be a representation of the resource itself
   *
   * @param operation the path request operation
   * @param resourceType the resource type the describes the resource that should be patched
   * @param bulkId the bulkId of the current patch request operation
   * @return the nodes the contain a bulkId reference
   */
  private List<JsonNode> getBulkIdsForPatchAddOnResource(PatchRequestOperation operation,
                                                         ResourceType resourceType,
                                                         String bulkId)
  {
    if (operation.getValues().size() != 1)
    {
      return Collections.emptyList();
    }
    ScimObjectNode scimObjectNode = JsonHelper.readJsonDocument(operation.getValues().get(0), ScimObjectNode.class);
    List<JsonNode> bulkIdNodes = getBulkIdNodes(scimObjectNode, resourceType);
    checkForSelfOrCircularReference(bulkId, bulkIdNodes);
    bulkIdNodes = setBulkIds(bulkIdNodes);
    operation.setValues(Collections.singletonList(scimObjectNode.toString()));
    return bulkIdNodes;
  }

  /**
   * checks if a bulkId-reference references itself or is a circular reference
   *
   * @param bulkId the bulkId of the current operation that is handled
   * @param bulkIdNodes a list of all bulkId-references that have been found within the resource
   * @throws BadRequestException if a self-reference was detected
   * @throws ConflictException if a circular reference was detected
   */
  private void checkForSelfOrCircularReference(String bulkId, List<JsonNode> bulkIdNodes)
  {
    if (StringUtils.isBlank(bulkId) || bulkIdNodes.isEmpty())
    {
      return;
    }
    for ( JsonNode bulkIdNode : bulkIdNodes )
    {
      JsonNode jsonNode = bulkIdNode.get(AttributeNames.RFC7643.VALUE);
      String bulkReference = jsonNode.textValue().replaceFirst("(?i)^" + AttributeNames.RFC7643.BULK_ID + ":", "");
      if (bulkReference.equals(bulkId))
      {
        throw new BadRequestException("the bulkId '" + bulkId + "' is a self-reference. Self-references will not be "
                                      + "resolved", null, ScimType.RFC7644.INVALID_VALUE);
      }
      Set<String> mainBulkIdSet = circularReferenceDetectorMap.computeIfAbsent(bulkId, k -> new HashSet<>());
      mainBulkIdSet.add(bulkReference);
      Set<String> subBulkIdSet = circularReferenceDetectorMap.computeIfAbsent(bulkReference, k -> new HashSet<>());
      if (subBulkIdSet.contains(bulkId))
      {
        throw new ConflictException("the bulkIds '" + bulkId + "' and '" + bulkReference + "' do form a circular "
                                    + "reference that cannot be resolved.");
      }
    }
  }

  /**
   * will resolve bulkId references with the ids of the created resources if the resources have been created
   * yet.
   *
   * @param getBulkIdNodes a list of all bulkId references that have been found in the currently processed
   *          operation
   * @return a list of all bulkId references that could not yet be resolved
   */
  private List<JsonNode> setBulkIds(List<JsonNode> getBulkIdNodes)
  {
    List<JsonNode> unresolvedNodes = new ArrayList<>();
    for ( JsonNode complexBulkIdNode : getBulkIdNodes )
    {
      String value = complexBulkIdNode.get(AttributeNames.RFC7643.VALUE).textValue();
      String[] bulkIdParts = value.split(":");
      if (bulkIdParts.length != 2)
      {
        throw new BadRequestException("the value '" + value + "' is not a valid bulkId reference", null,
                                      ScimType.RFC7644.INVALID_VALUE);
      }
      String bulkId = bulkIdParts[1];
      String resourceId = resolvedBulkIds.get(bulkId);
      if (StringUtils.isNotBlank(resourceId))
      {
        JsonHelper.replaceNode(complexBulkIdNode, AttributeNames.RFC7643.VALUE, new TextNode(resourceId));
      }
      else
      {
        unresolvedNodes.add(complexBulkIdNode);
      }
    }
    return unresolvedNodes;
  }

  /**
   * extracts all nodes that do contain a bulkId-reference of the currently processed bulk request operation
   *
   * @param resource the resource that was sent with the bulk request operation
   * @param resourceType the resource type of the currently processed resource
   * @return a list of all nodes that do contain a bulkId reference
   */
  private List<JsonNode> getBulkIdNodes(ScimObjectNode resource, ResourceType resourceType)
  {
    List<SchemaAttribute> bulkIdCandidates = resourceType.getAllSchemas()
                                                         .stream()
                                                         .flatMap(schema -> schema.getBulkIdCandidates().stream())
                                                         .collect(Collectors.toList());
    if (bulkIdCandidates.isEmpty())
    {
      return Collections.emptyList();
    }
    List<JsonNode> bulkIdNodes = new ArrayList<>();
    for ( SchemaAttribute bulkIdCandidate : bulkIdCandidates )
    {
      bulkIdNodes.addAll(getBulkIdNodes(resourceType, resource, bulkIdCandidate));
    }
    return bulkIdNodes;
  }

  /**
   * a sub-method of {@link #getBulkIdNodes(ScimObjectNode, ResourceType)} that extracts the nodes
   *
   * @param resourceType the resource type of the currently processed resource
   * @param resource the resource that was sent with the bulk request operation that is currently processed
   * @param bulkIdCandidate a bulkIdCandidate that belongs to the current resource do directly extract the node
   *          that is allowed to have a bulkId-reference
   * @return a list with all extracted bulkId-references. This can be several if the target is a multi-valued
   *         complex type
   */
  private List<JsonNode> getBulkIdNodes(ResourceType resourceType,
                                        ScimObjectNode resource,
                                        SchemaAttribute bulkIdCandidate)
  {
    JsonNode bulkCandidateNode = resource.get(bulkIdCandidate.getName());
    if (bulkCandidateNode == null)
    {
      bulkCandidateNode = getNodeFromExtension(resourceType, resource, bulkIdCandidate);
      if (bulkCandidateNode == null)
      {
        return Collections.emptyList();
      }
    }
    List<JsonNode> bulkIdNodes = new ArrayList<>();
    if (bulkCandidateNode.isArray())
    {
      for ( JsonNode complexNode : bulkCandidateNode )
      {
        JsonNode bulkIdNode = complexNode.get(AttributeNames.RFC7643.VALUE);
        if (bulkIdNode == null)
        {
          continue;
        }
        if (StringUtils.startsWithIgnoreCase(bulkIdNode.textValue(), AttributeNames.RFC7643.BULK_ID + ":"))
        {
          bulkIdNodes.add(complexNode);
        }
      }
    }
    else
    {
      JsonNode bulkIdNode = bulkCandidateNode.get(AttributeNames.RFC7643.VALUE);
      if (bulkIdNode == null)
      {
        return Collections.emptyList();
      }
      if (StringUtils.startsWithIgnoreCase(bulkIdNode.textValue(), AttributeNames.RFC7643.BULK_ID + ":"))
      {
        bulkIdNodes.add(bulkCandidateNode);
      }
    }
    return bulkIdNodes;
  }

  /**
   * bulkId-references may also be present within extensions so this method checks if the bulkIdCandidate
   * belongs to an extension schema and if so the node is extracted from the extension node
   *
   * @param resourceType the resource type of the currently processed resource
   * @param resource the resource that was sent with the bulk request operation that is currently processed
   * @param bulkIdCandidate a bulkIdCandidate that belongs to the current resource do directly extract the node
   *          that is allowed to have a bulkId-reference
   * @return null if the bulkIdCandidate is not an extension or the node does not exist in the extension node.
   *         Otherwise the extracted bulkIdCandidate node is returned
   */
  private JsonNode getNodeFromExtension(ResourceType resourceType,
                                        ScimObjectNode resource,
                                        SchemaAttribute bulkIdCandidate)
  {
    final String schemaUri = bulkIdCandidate.getSchema().getId().orElse(null);
    boolean isExtensionNode = !resourceType.getSchema().equals(schemaUri);
    if (isExtensionNode)
    {
      ObjectNode extensionNode = (ObjectNode)resource.get(schemaUri);
      if (extensionNode != null)
      {
        return extensionNode.get(bulkIdCandidate.getName());
      }
    }
    return null;
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
      JsonNode validatedRequest = new RequestSchemaValidator(ScimObjectNode.class,
                                                             HttpMethod.POST).validateDocument(bulkRequestSchema,
                                                                                               jsonNode);
      BulkRequest bulkRequest = JsonHelper.copyResourceToObject(validatedRequest, BulkRequest.class);
      if (bulkConfig.getMaxOperations() < bulkRequest.getBulkRequestOperations().size())
      {
        throw new BadRequestException("too many operations maximum number of operations is '"
                                      + bulkConfig.getMaxOperations() + "'", null, ScimType.RFC7644.TOO_MANY);
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
    List<HttpMethod> validMethods = Arrays.asList(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);
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
