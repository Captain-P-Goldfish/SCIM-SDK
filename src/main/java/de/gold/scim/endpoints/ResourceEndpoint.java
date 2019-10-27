package de.gold.scim.endpoints;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.constants.ScimType;
import de.gold.scim.constants.enums.HttpMethod;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.InternalServerException;
import de.gold.scim.exceptions.NotImplementedException;
import de.gold.scim.request.BulkRequest;
import de.gold.scim.request.BulkRequestOperation;
import de.gold.scim.resources.ServiceProvider;
import de.gold.scim.response.BulkResponse;
import de.gold.scim.response.BulkResponseOperation;
import de.gold.scim.response.ErrorResponse;
import de.gold.scim.response.ScimResponse;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.utils.JsonHelper;
import de.gold.scim.utils.RequestUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 26.10.2019 - 00:05 <br>
 * <br>
 * This class will receive any request and will then delegate the request to the correct endpoint and resource
 * type
 */
@Slf4j
public final class ResourceEndpoint extends ResourceEndpointHandler
{


  /**
   * this constructor was introduced for unit tests to add a specific resourceTypeFactory instance which will
   * prevent application context pollution within unit tests
   */
  public ResourceEndpoint(ServiceProvider serviceProvider, EndpointDefinition... endpointDefinitions)
  {
    super(serviceProvider, endpointDefinitions);
  }

  /**
   * this method will resolve the SCIM request based on the given information
   *
   * @param requestUrl the fully qualified resource URL e.g.:
   *
   *          <pre>
   *             https://localhost/v2/scim/Users<br>
   *             https://localhost/v2/scim/Users/123456<br>
   *             https://localhost/v2/scim/Users/.search<br>
   *             https://localhost/v2/scim/Users?startIndex=1&count=20&filter=userName+eq+%22chucky%22
   *          </pre>
   *
   * @param httpMethod the http method that was used by in the request
   * @param requestBody the request body of the request, may be null
   * @return the resolved SCIM response
   */
  public ScimResponse handleRequest(String requestUrl, HttpMethod httpMethod, String requestBody)
  {
    UriInfos uriInfos = getRequestUrlInfos(requestUrl, httpMethod);
    if (EndpointPaths.BULK.equals(uriInfos.getResourceEndpoint()))
    {
      return bulk(uriInfos, requestBody);
    }
    return resolveRequest(httpMethod, requestBody, uriInfos);
  }

  /**
   * this method will handle the request send by the user by delegating to the corresponding methods
   *
   * @param httpMethod the http method that was used by the client
   * @param requestBody the request body
   * @param uriInfos the parsed information's of the request url
   * @return a response for the client that is either successful or an error
   */
  protected ScimResponse resolveRequest(HttpMethod httpMethod, String requestBody, UriInfos uriInfos)
  {
    switch (httpMethod)
    {
      case POST:
        if (uriInfos.isSearchRequest())
        {
          return listResources(uriInfos.getResourceEndpoint(), requestBody, uriInfos::getBaseUri);
        }
        else
        {
          return createResource(uriInfos.getResourceEndpoint(), requestBody, uriInfos::getBaseUri);
        }
      case GET:
        if (uriInfos.isSearchRequest())
        {
          String startIndex = uriInfos.getQueryParameters().get(AttributeNames.RFC7643.START_INDEX);
          String count = uriInfos.getQueryParameters().get(AttributeNames.RFC7643.COUNT);
          return listResources(uriInfos.getResourceEndpoint(),
                               startIndex == null ? null : Long.parseLong(startIndex),
                               count == null ? null : Integer.parseInt(count),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.FILTER),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.SORT_BY),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.SORT_ORDER),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.ATTRIBUTES),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES),
                               uriInfos::getBaseUri);
        }
        else
        {
          return getResource(uriInfos.getResourceEndpoint(), uriInfos.getResourceId(), uriInfos::getBaseUri);
        }
      case PUT:
        return updateResource(uriInfos.getResourceEndpoint(),
                              uriInfos.getResourceId(),
                              requestBody,
                              uriInfos::getBaseUri);
      case PATCH:
        throw new NotImplementedException("not yet implemented");
      default:
        return deleteResource(uriInfos.getResourceEndpoint(), uriInfos.getResourceId());
    }
  }

  /**
   * resolves a bulk request
   *
   * @param uriInfos the parsed URL information for the bulk request
   * @param requestBody the bulk request body
   * @return the response of the bulk request
   */
  private BulkResponse bulk(UriInfos uriInfos, String requestBody)
  {
    BulkRequest bulkRequest = JsonHelper.readJsonDocument(requestBody, BulkRequest.class);
    List<BulkRequestOperation> operations = bulkRequest.getBulkRequestOperations();
    operations = sortOperations(operations);
    List<BulkResponseOperation> responseOperations = new ArrayList<>();
    final int failOnErrors = bulkRequest.getFailOnErrors().orElse(Integer.MAX_VALUE);
    validateFailOnErrors(failOnErrors);
    int errorCounter = 0;
    for ( BulkRequestOperation operation : operations )
    {
      HttpMethod httpMethod = HttpMethod.valueOf(operation.getMethod());
      validateHttpMethodForBulkOperation(httpMethod);
      String[] pathParts = operation.getPath().split("/");
      ResourceType resourceType = getResourceType(pathParts);
      UriInfos operationUriInfo = UriInfos.builder()
                                          .baseUri(uriInfos.getBaseUri())
                                          .resourceEndpoint(pathParts[0])
                                          .resourceId(pathParts.length > 1 ? pathParts[1] : null)
                                          .resourceType(resourceType)
                                          .build();
      ScimResponse scimResponse = resolveRequest(httpMethod, requestBody, operationUriInfo);
      if (ErrorResponse.class.isAssignableFrom(scimResponse.getClass()))
      {
        errorCounter++;
        if (errorCounter > failOnErrors)
        {
          // TODO handle failOnErrors exceeded
          log.warn("TODO handle failOnErrors exceeded");
          continue;
        }
      }
      final String location = uriInfos.getBaseUri() + operationUriInfo.getResourceEndpoint()
                              + (operationUriInfo.getResourceId() == null ? ""
                                : "/" + operationUriInfo.getResourceId());
      responseOperations.add(BulkResponseOperation.builder()
                                                  .bulkId(operation.getBulkId().orElse(null))
                                                  .status(scimResponse.getHttpStatus())
                                                  .method(operation.getMethod())
                                                  .location(location)
                                                  .response(ErrorResponse.class.isAssignableFrom(scimResponse.getClass())
                                                    ? (ErrorResponse)scimResponse : null)
                                                  .build());
    }
    return BulkResponse.builder().bulkResponseOperation(responseOperations).build();
  }

  private void validateFailOnErrors(int failOnErrors)
  {
    log.warn("TODO validate fail on errors");
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
   * verifies that the bulk operation uses a valid http method<br>
   * <br>
   *
   * <pre>
   *    The body of a bulk operation contains a set of HTTP resource operations
   *    using one of the HTTP methods supported by the API, i.e., POST, PUT,
   *    PATCH, or DELETE.
   * </pre>
   */
  private void validateHttpMethodForBulkOperation(HttpMethod httpMethod)
  {
    List<HttpMethod> validMethods = Arrays.asList(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);
    if (!validMethods.contains(httpMethod))
    {
      throw new BadRequestException("bulk request used invalid http method. Only the following methods are allowed "
                                    + "for bulk: " + validMethods, null, ScimType.Custom.UNPARSEABLE_REQUEST);
    }
  }

  /**
   * resolves the request uri to individual information's that are necessary to resolve the request
   *
   * @param requestUrl the fully qualified request url
   * @return the individual request information's
   */
  protected UriInfos getRequestUrlInfos(String requestUrl, HttpMethod httpMethod)
  {
    final URL url = toUrl(requestUrl);
    final String[] pathParts = url.getPath().split("/");
    final ResourceType resourceType = getResourceType(pathParts);
    if (isBulkRequest(httpMethod, resourceType))
    {
      return UriInfos.builder()
                     .baseUri(StringUtils.substringBeforeLast(requestUrl, EndpointPaths.BULK))
                     .resourceEndpoint(EndpointPaths.BULK)
                     .build();
    }
    final boolean endsOfSearch = EndpointPaths.SEARCH.endsWith(pathParts[pathParts.length - 1]);
    final boolean endsOfResource = resourceType.getEndpoint().endsWith(pathParts[pathParts.length - 1]);
    final String resourceId = endsOfSearch ? null : (endsOfResource ? null : pathParts[pathParts.length - 1]);
    final boolean searchRequest = endsOfSearch && HttpMethod.POST.equals(httpMethod)
                                  || HttpMethod.GET.equals(httpMethod) && resourceId == null;
    final String baseUri = StringUtils.substringBeforeLast(requestUrl, resourceType.getEndpoint());
    UriInfos uriInfos = UriInfos.builder()
                                .baseUri(baseUri)
                                .searchRequest(searchRequest)
                                .resourceEndpoint(resourceType.getEndpoint())
                                .resourceId(resourceId)
                                .queryParameters(url.getQuery())
                                .resourceType(resourceType)
                                .build();
    validateUriInfos(uriInfos, httpMethod);
    return uriInfos;
  }

  /**
   * this method will verify that the parsed data of the request is valid for accessing SCIM endpoint
   *
   * @param uriInfos the parsed infos of the request url
   * @param httpMethod the http method that was used
   */
  private void validateUriInfos(UriInfos uriInfos, HttpMethod httpMethod)
  {
    switch (httpMethod)
    {
      case POST:
        if (StringUtils.isNotBlank(uriInfos.getResourceId()))
        {
          throw new BadRequestException("ID values in the path are not allowed on '" + httpMethod + "' requests", null,
                                        ScimType.Custom.INVALID_PARAMETERS);
        }
        break;
      case PUT:
      case PATCH:
      case DELETE:
        if (StringUtils.isBlank(uriInfos.getResourceId()))
        {
          throw new BadRequestException("missing ID value in request path for method '" + httpMethod + "'", null,
                                        ScimType.Custom.INVALID_PARAMETERS);
        }
    }
  }

  /**
   * checks if we got a bulk request
   *
   * @param httpMethod the http method must be post for bulk requests
   * @param resourceType the resource type must be null. There are no resource types registered for bulk
   * @return true if this is a bulk request, false else
   */
  private boolean isBulkRequest(HttpMethod httpMethod, ResourceType resourceType)
  {
    if (resourceType == null) // this is only null if the request is a bulk request
    {
      if (HttpMethod.POST.equals(httpMethod))
      {
        return true;
      }
      else
      {
        throw new BadRequestException("Bulk endpoint can only be reached with a HTTP-POST request", null, null);
      }
    }
    return false;
  }

  /**
   * will get the resource type to which the request url points
   *
   * @param urlParts the request url parts separated by "/"
   * @return the found resource type or null if the request points to the bulk-endpoint
   * @throws BadRequestException if the request does neither point to the bulk endpoint nor a registered
   *           resource type
   */
  private ResourceType getResourceType(String[] urlParts)
  {
    if (EndpointPaths.BULK.endsWith(urlParts[urlParts.length - 1]))
    {
      return null;
    }
    for ( ResourceType resourceType : getResourceTypeFactory().getAllResourceTypes() )
    {
      if (StringUtils.endsWith(resourceType.getEndpoint(), urlParts[urlParts.length - 1])
          || (urlParts.length > 1 && StringUtils.endsWith(resourceType.getEndpoint(), urlParts[urlParts.length - 2])))
      {
        return resourceType;
      }
    }
    throw new BadRequestException("the request url does not point to a registered resource type. Registered resource "
                                  + "types are: ["
                                  + getResourceTypeFactory().getAllResourceTypes()
                                                            .stream()
                                                            .map(ResourceType::getEndpoint)
                                                            .collect(Collectors.joining(","))
                                  + "]", null, ScimType.Custom.INVALID_PARAMETERS);
  }

  /**
   * turns the given string into an {@link URL} object
   *
   * @param url the url string
   * @return the {@link URL} instance of the given string
   */
  private URL toUrl(String url)
  {
    try
    {
      return new URL(url);
    }
    catch (MalformedURLException e)
    {
      throw new InternalServerException(e.getMessage(), e, null);
    }
  }

  /**
   * represents the parsed uri infos of the request
   */
  @Getter
  protected static class UriInfos
  {

    /**
     * the resource endpoint reference e.g. "/Users" or "/Groups"
     */
    private final String resourceEndpoint;

    /**
     * the id of the resource for PUT, DELETE, PATCH and GET requests
     */
    private final String resourceId;

    /**
     * if the given request is a query POST request
     */
    private final boolean searchRequest;

    /**
     * the base uri to this SCIM endpoint
     */
    private final String baseUri;

    /**
     * the get parameters or the uri
     */
    private final Map<String, String> queryParameters;

    /**
     * the resource type to which the url points
     */
    private final ResourceType resourceType;

    @Builder
    public UriInfos(String resourceEndpoint,
                    String resourceId,
                    boolean searchRequest,
                    String baseUri,
                    String queryParameters,
                    ResourceType resourceType)
    {
      this.resourceEndpoint = resourceEndpoint;
      this.resourceId = resourceId;
      this.searchRequest = searchRequest;
      this.baseUri = baseUri;
      this.queryParameters = queryParameters == null ? new HashMap<>()
        : RequestUtils.getQueryParameters(queryParameters);
      this.resourceType = resourceType;
    }
  }
}
