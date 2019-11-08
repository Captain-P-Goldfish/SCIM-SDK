package de.gold.scim.server.endpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.common.constants.HttpStatus;
import de.gold.scim.common.constants.SchemaUris;
import de.gold.scim.common.constants.ScimType;
import de.gold.scim.common.constants.enums.HttpMethod;
import de.gold.scim.common.exceptions.BadRequestException;
import de.gold.scim.common.exceptions.NotImplementedException;
import de.gold.scim.common.exceptions.ScimException;
import de.gold.scim.common.request.BulkRequest;
import de.gold.scim.common.request.BulkRequestOperation;
import de.gold.scim.common.resources.ServiceProvider;
import de.gold.scim.common.resources.complex.BulkConfig;
import de.gold.scim.common.response.BulkResponse;
import de.gold.scim.common.response.BulkResponseOperation;
import de.gold.scim.common.response.ErrorResponse;
import de.gold.scim.common.response.ScimResponse;
import de.gold.scim.common.schemas.Schema;
import de.gold.scim.common.utils.JsonHelper;
import de.gold.scim.server.schemas.ResourceTypeFactory;
import de.gold.scim.server.schemas.SchemaFactory;
import de.gold.scim.server.schemas.SchemaValidator;
import de.gold.scim.server.utils.RequestUtils;
import de.gold.scim.server.utils.UriInfos;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 07.11.2019 - 23:48 <br>
 * <br>
 */
@Slf4j
@AllArgsConstructor
class BulkEndpoint
{

  private ResourceEndpoint resourceEndpoint;

  @Getter
  private ServiceProvider serviceProvider;

  @Getter
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * resolves a bulk request
   *
   * @param requestBody the bulk request body
   * @return the response of the bulk request
   */
  public BulkResponse bulk(String baseUri, String requestBody)
  {
    BulkRequest bulkRequest = parseAndValidateBulkRequest(requestBody);
    List<BulkRequestOperation> operations = bulkRequest.getBulkRequestOperations();
    operations = sortOperations(operations);
    List<BulkResponseOperation> responseOperations = new ArrayList<>();
    final int failOnErrors = RequestUtils.getEffectiveFailOnErrors(bulkRequest);
    int errorCounter = 0;

    for ( BulkRequestOperation operation : operations )
    {
      if (errorCounter >= failOnErrors)
      {
        // The service provider stops processing the bulk operation and immediately returns a response to the client
        break;
      }
      HttpMethod httpMethod = operation.getMethod();
      UriInfos operationUriInfo = UriInfos.getRequestUrlInfos(getResourceTypeFactory(),
                                                              baseUri + operation.getPath(),
                                                              httpMethod);
      final String id = Optional.ofNullable(operationUriInfo.getResourceId())
                                .map(resourceId -> "/" + resourceId)
                                .orElse("");
      final String location = baseUri + operationUriInfo.getResourceEndpoint() + id;
      BulkResponseOperation.BulkResponseOperationBuilder responseBuilder = BulkResponseOperation.builder();
      responseBuilder.bulkId(operation.getBulkId().orElse(null)).method(operation.getMethod()).location(location);
      try
      {
        validateOperation(operation);
      }
      catch (BadRequestException ex)
      {
        errorCounter++;
        responseOperations.add(responseBuilder.status(ex.getStatus()).response(new ErrorResponse(ex)).build());
        continue;
      }

      ScimResponse scimResponse = resourceEndpoint.resolveRequest(httpMethod,
                                                                  operation.getData().orElse(null),
                                                                  operationUriInfo);
      responseBuilder.status(scimResponse.getHttpStatus())
                     .response(ErrorResponse.class.isAssignableFrom(scimResponse.getClass())
                       ? (ErrorResponse)scimResponse : null);
      if (ErrorResponse.class.isAssignableFrom(scimResponse.getClass()))
      {
        if (HttpMethod.POST.equals(operation.getMethod()))
        {
          // A "location" attribute that includes the resource's endpoint MUST be returned for all operations
          // except for failed POST operations (which have no location)
          responseBuilder.location(null);
        }
        errorCounter++;
      }
      responseOperations.add(responseBuilder.build());
    }
    int httpStatus = HttpStatus.OK;
    if (errorCounter >= failOnErrors)
    {
      // The service returns an appropriate response status code if too many errors occurred
      httpStatus = HttpStatus.PRECONDITION_FAILED;
    }
    return BulkResponse.builder().httpStatus(httpStatus).bulkResponseOperation(responseOperations).build();
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
      JsonNode validatedRequest = SchemaValidator.validateSchemaDocumentForRequest(getResourceTypeFactory(),
                                                                                   bulkRequestSchema,
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
   * this method must resolve the order of the operations by resolving the bulkIds and the references within the
   * methods
   *
   * @param operations the list of operations
   * @return the sorted operations in the order they should be executed
   */
  private List<BulkRequestOperation> sortOperations(List<BulkRequestOperation> operations)
  {
    // TODO
    log.warn("TODO sorting bulk operations not yet implemented");
    return operations;
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
