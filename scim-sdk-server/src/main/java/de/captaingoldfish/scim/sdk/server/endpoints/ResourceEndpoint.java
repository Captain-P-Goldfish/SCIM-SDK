package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.exceptions.UnauthenticatedException;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.endpoints.features.EndpointFeatureHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.features.EndpointType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import de.captaingoldfish.scim.sdk.server.utils.UriInfos;
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
   * create a resource endpoint with default meta-endpoints
   *
   * @param serviceProvider the service provider configuration of this SCIM provider setup
   * @param endpointDefinitions the endpoint definitions that should be registered additionally to the meta
   *          endpoint "/ServiceProviderConfig, /ResourceTypes, /Schemas"
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
   * @param httpHeaders the http request headers, may be null
   * @param context an optional context object that will be expanded with additional information of the current
   *          request
   * @return the resolved SCIM response
   */
  public ScimResponse handleRequest(String requestUrl,
                                    HttpMethod httpMethod,
                                    String requestBody,
                                    Map<String, String> httpHeaders,
                                    Context context)
  {
    return handleRequest(requestUrl, httpMethod, requestBody, httpHeaders, null, null, context);
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
   * @param httpHeaders the http request headers, may be null
   * @param doBeforeExecution arbitary code that is executed before the endpoint is called. This might be used
   *          to execute authentication on dedicated resource types
   * @param context an optional context object that will be expanded with additional information of the current
   *          request
   * @return the resolved SCIM response
   */
  public ScimResponse handleRequest(String requestUrl,
                                    HttpMethod httpMethod,
                                    String requestBody,
                                    Map<String, String> httpHeaders,
                                    Consumer<ResourceType> doBeforeExecution,
                                    Context context)
  {
    return handleRequest(requestUrl, httpMethod, requestBody, httpHeaders, doBeforeExecution, null, context);
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
   * @param httpHeaders the http request headers, may be null
   * @param doAfterExecution an optional implementation that can be used to execute arbitrary code after the
   *          execution of the request has been finished. First parameter is the response object second is a
   *          boolean that tells if the request failed or succeeded.
   * @param context an optional context object that will be expanded with additional information of the current
   *          request
   * @return the resolved SCIM response
   */
  public ScimResponse handleRequest(String requestUrl,
                                    HttpMethod httpMethod,
                                    String requestBody,
                                    Map<String, String> httpHeaders,
                                    BiConsumer<ScimResponse, Boolean> doAfterExecution,
                                    Context context)
  {
    return handleRequest(requestUrl, httpMethod, requestBody, httpHeaders, null, doAfterExecution, context);
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
   * @param httpHeaders the http request headers, may be null
   * @param authorization should return the roles of an user and may contain arbitrary data needed in the
   *          handler implementation
   * @param doBeforeExecution arbitary code that is executed before the endpoint is called. This might be used
   *          to execute authentication on dedicated resource types
   * @param doAfterExecution an optional implementation that can be used to execute arbitrary code after the
   *          execution of the request has been finished. First parameter is the response object second is a
   *          boolean that tells if the request failed or succeeded.
   * @param context an optional context object that will be expanded with additional information of the current
   *          request
   * @return the resolved SCIM response
   */
  public ScimResponse handleRequest(String requestUrl,
                                    HttpMethod httpMethod,
                                    String requestBody,
                                    Map<String, String> httpHeaders,
                                    Consumer<ResourceType> doBeforeExecution,
                                    BiConsumer<ScimResponse, Boolean> doAfterExecution,
                                    Context context)
  {
    ScimResponse scimResponse;

    handleScimRequest: try
    {
      UriInfos uriInfos = UriInfos.getRequestUrlInfos(getResourceTypeFactory(), requestUrl, httpMethod, httpHeaders);
      if (EndpointPaths.BULK.equals(uriInfos.getResourceEndpoint()))
      {
        BulkEndpoint bulkEndpoint = new BulkEndpoint(this, getServiceProvider(), getResourceTypeFactory(),
                                                     uriInfos.getHttpHeaders(), uriInfos.getQueryParameters(),
                                                     doBeforeExecution);
        scimResponse = bulkEndpoint.bulk(uriInfos.getBaseUri(), requestBody, context);
        break handleScimRequest;
      }
      scimResponse = resolveRequest(httpMethod, requestBody, uriInfos, doBeforeExecution, context);
    }
    catch (ScimException ex)
    {
      scimResponse = new ErrorResponse(ex);
    }
    catch (Exception ex)
    {
      scimResponse = new ErrorResponse(new InternalServerException(ex.getMessage(), ex, null));
    }

    if (doAfterExecution != null)
    {
      doAfterExecution.accept(scimResponse, isErrorResponse(scimResponse));
    }

    return scimResponse;
  }

  /**
   * this method will handle the request send by the user by delegating to the corresponding methods
   *
   * @param httpMethod the http method that was used by the client
   * @param requestBody the request body
   * @param uriInfos the parsed information's of the request url
   * @param authorization should return the roles of an user and may contain arbitrary data needed in the
   *          handler implementation
   * @param doBeforeExecution arbitrary code that is executed before the endpoint is called. This might be used
   *          to execute authentication on dedicated resource types
   * @return a response for the client that is either successful or an error
   */
  protected ScimResponse resolveRequest(HttpMethod httpMethod,
                                        String requestBody,
                                        UriInfos uriInfos,
                                        Consumer<ResourceType> doBeforeExecution,
                                        Context context)
  {
    Optional.ofNullable(doBeforeExecution).ifPresent(consumer -> consumer.accept(uriInfos.getResourceType()));
    Context effectiveContext = getEffectiveContext(uriInfos, requestBody, context);
    authenticateClient(uriInfos, effectiveContext.getAuthorization());
    switch (httpMethod)
    {
      case POST:
        if (uriInfos.isSearchRequest())
        {
          EndpointFeatureHandler.handleEndpointFeatures(uriInfos.getResourceType(),
                                                        EndpointType.LIST,
                                                        effectiveContext.getAuthorization());
          return listResources(uriInfos.getResourceEndpoint(), requestBody, uriInfos::getBaseUri, effectiveContext);
        }
        else
        {
          EndpointFeatureHandler.handleEndpointFeatures(uriInfos.getResourceType(),
                                                        EndpointType.CREATE,
                                                        effectiveContext.getAuthorization());
          return createResource(uriInfos.getResourceEndpoint(), requestBody, uriInfos::getBaseUri, effectiveContext);
        }
      case GET:
        if (uriInfos.isSearchRequest() && !uriInfos.getResourceType().getFeatures().isSingletonEndpoint())
        {
          EndpointFeatureHandler.handleEndpointFeatures(uriInfos.getResourceType(),
                                                        EndpointType.LIST,
                                                        effectiveContext.getAuthorization());
          String startIndex = uriInfos.getQueryParameters().get(AttributeNames.RFC7643.START_INDEX.toLowerCase());
          String count = uriInfos.getQueryParameters().get(AttributeNames.RFC7643.COUNT);
          return listResources(uriInfos.getResourceEndpoint(),
                               RequestUtils.parseStartIndex(startIndex).orElse(null),
                               RequestUtils.parseCount(count).orElse(null),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.FILTER),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.SORT_BY.toLowerCase()),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.SORT_ORDER.toLowerCase()),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.ATTRIBUTES),
                               uriInfos.getQueryParameters()
                                       .get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES.toLowerCase()),
                               uriInfos::getBaseUri,
                               effectiveContext);
        }
        else
        {
          EndpointFeatureHandler.handleEndpointFeatures(uriInfos.getResourceType(),
                                                        EndpointType.GET,
                                                        effectiveContext.getAuthorization());
          return getResource(uriInfos.getResourceEndpoint(),
                             uriInfos.getResourceId(),
                             uriInfos.getQueryParameters().get(AttributeNames.RFC7643.ATTRIBUTES),
                             uriInfos.getQueryParameters()
                                     .get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES.toLowerCase()),
                             uriInfos.getHttpHeaders(),
                             uriInfos::getBaseUri,
                             effectiveContext);
        }
      case PUT:
        EndpointFeatureHandler.handleEndpointFeatures(uriInfos.getResourceType(),
                                                      EndpointType.UPDATE,
                                                      effectiveContext.getAuthorization());
        return updateResource(uriInfos.getResourceEndpoint(),
                              uriInfos.getResourceId(),
                              requestBody,
                              uriInfos.getHttpHeaders(),
                              uriInfos::getBaseUri,
                              effectiveContext);
      case PATCH:
        EndpointFeatureHandler.handleEndpointFeatures(uriInfos.getResourceType(),
                                                      EndpointType.UPDATE,
                                                      effectiveContext.getAuthorization());
        return patchResource(uriInfos.getResourceEndpoint(),
                             uriInfos.getResourceId(),
                             requestBody,
                             uriInfos.getQueryParameters().get(AttributeNames.RFC7643.ATTRIBUTES),
                             uriInfos.getQueryParameters()
                                     .get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES.toLowerCase()),
                             uriInfos.getHttpHeaders(),
                             uriInfos::getBaseUri,
                             effectiveContext);
      default:
        EndpointFeatureHandler.handleEndpointFeatures(uriInfos.getResourceType(),
                                                      EndpointType.DELETE,
                                                      effectiveContext.getAuthorization());
        return deleteResource(uriInfos.getResourceEndpoint(),
                              uriInfos.getResourceId(),
                              uriInfos.getHttpHeaders(),
                              effectiveContext);
    }
  }

  /**
   * expands the currently existing context passed by the developer or creates a new context and adds the
   * current context data to it
   *
   * @param uriInfos the current uri infos of the request
   * @param requestBody to be able to access the original request body within the resourcehandler
   * @param context the context created by the developer (might be null)
   * @return an expanded or new context
   */
  private Context getEffectiveContext(UriInfos uriInfos, String requestBody, Context context)
  {
    Context effectiveContext = Optional.ofNullable(context).orElse(new Context(null));
    effectiveContext.setResourceReferenceUrl(id -> {
      return super.getReferenceUrlSupplier(uriInfos::getBaseUri).apply(id, uriInfos.getResourceType().getName());
    });
    effectiveContext.setCrossResourceReferenceUrl((id, resourceName) -> {
      return super.getReferenceUrlSupplier(uriInfos::getBaseUri).apply(id, resourceName);
    });
    effectiveContext.setRequestBodySupplier(() -> {
      return requestBody;
    });
    return effectiveContext;
  }

  /**
   * checks if the given response is an error response or a successful response
   *
   * @return true if the result is an error, false else
   */
  private boolean isErrorResponse(ScimResponse scimResponse)
  {
    return ErrorResponse.class.isAssignableFrom(scimResponse.getClass())
           || (BulkResponse.class.isAssignableFrom(scimResponse.getClass())
               && scimResponse.getHttpStatus() != HttpStatus.OK);
  }

  /**
   * will authenticate the client that is currently accessing the resource server
   *
   * @param authorization the authorization object that will handle the authentication
   */
  private void authenticateClient(UriInfos uriInfos, Authorization authorization)
  {
    ResourceType resourceType = uriInfos.getResourceType();
    if (!resourceType.getFeatures().getAuthorization().isAuthenticated())
    {
      // no authentication required for this endpoint
      return;
    }
    boolean isAuthenticated = authorization.authenticate(uriInfos.getHttpHeaders(), uriInfos.getQueryParameters());
    if (!isAuthenticated)
    {
      log.debug("Authentication has failed");
      throw new UnauthenticatedException("not authenticated", getServiceProvider().getAuthenticationSchemes(),
                                         authorization.getRealm());
    }
  }
}
