package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.Map;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
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
   * @param httpHeaders the http request headers, may be null
   * @return the resolved SCIM response
   */
  public ScimResponse handleRequest(String requestUrl,
                                    HttpMethod httpMethod,
                                    String requestBody,
                                    Map<String, String> httpHeaders)
  {
    try
    {
      UriInfos uriInfos = UriInfos.getRequestUrlInfos(getResourceTypeFactory(), requestUrl, httpMethod, httpHeaders);
      if (EndpointPaths.BULK.equals(uriInfos.getResourceEndpoint()))
      {
        BulkEndpoint bulkEndpoint = new BulkEndpoint(this, getServiceProvider(), getResourceTypeFactory());
        return bulkEndpoint.bulk(uriInfos.getBaseUri(), requestBody);
      }
      return resolveRequest(httpMethod, requestBody, uriInfos);
    }
    catch (ScimException ex)
    {
      return new ErrorResponse(ex);
    }
    catch (Exception ex)
    {
      return new ErrorResponse(new InternalServerException(ex.getMessage(), ex, null));
    }
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
          String startIndex = uriInfos.getQueryParameters().get(AttributeNames.RFC7643.START_INDEX.toLowerCase());
          String count = uriInfos.getQueryParameters().get(AttributeNames.RFC7643.COUNT);
          return listResources(uriInfos.getResourceEndpoint(),
                               startIndex == null ? null : Long.parseLong(startIndex),
                               count == null ? null : Integer.parseInt(count),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.FILTER),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.SORT_BY.toLowerCase()),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.SORT_ORDER.toLowerCase()),
                               uriInfos.getQueryParameters().get(AttributeNames.RFC7643.ATTRIBUTES),
                               uriInfos.getQueryParameters()
                                       .get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES.toLowerCase()),
                               uriInfos::getBaseUri);
        }
        else
        {
          return getResource(uriInfos.getResourceEndpoint(),
                             uriInfos.getResourceId(),
                             uriInfos.getQueryParameters().get(AttributeNames.RFC7643.ATTRIBUTES),
                             uriInfos.getQueryParameters()
                                     .get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES.toLowerCase()),
                             uriInfos.getHttpHeaders(),
                             uriInfos::getBaseUri);
        }
      case PUT:
        return updateResource(uriInfos.getResourceEndpoint(),
                              uriInfos.getResourceId(),
                              requestBody,
                              uriInfos.getHttpHeaders(),
                              uriInfos::getBaseUri);
      case PATCH:
        return patchResource(uriInfos.getResourceEndpoint(),
                             uriInfos.getResourceId(),
                             requestBody,
                             uriInfos.getQueryParameters().get(AttributeNames.RFC7643.ATTRIBUTES),
                             uriInfos.getQueryParameters()
                                     .get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES.toLowerCase()),
                             uriInfos.getHttpHeaders(),
                             uriInfos::getBaseUri);
      default:
        return deleteResource(uriInfos.getResourceEndpoint(), uriInfos.getResourceId(), uriInfos.getHttpHeaders());
    }
  }
}
