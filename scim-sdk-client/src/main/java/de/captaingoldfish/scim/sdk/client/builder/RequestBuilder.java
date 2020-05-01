package de.captaingoldfish.scim.sdk.client.builder;

import java.util.Collections;
import java.util.Map;

import org.apache.http.client.methods.HttpUriRequest;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ScimServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import lombok.AccessLevel;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 07.12.2019 - 23:15 <br>
 * <br>
 * an abstract request builder implementation
 */
public abstract class RequestBuilder<T extends ResourceNode>
{

  /**
   * the base url to the scim service
   */
  @Getter(AccessLevel.PROTECTED)
  private final String baseUrl;

  /**
   * the http client configuration
   */
  private final ScimClientConfig scimClientConfig;

  /**
   * the resource endpoint path e.g. /Users or /Groups
   */
  @Getter(AccessLevel.PROTECTED)
  private String endpoint;

  /**
   * the resource that should be sent to the service provider
   */
  @Getter(AccessLevel.PROTECTED)
  private String resource;

  /**
   * the expected resource type
   */
  @Getter(AccessLevel.PROTECTED)
  private Class<T> responseEntityType;

  /**
   * an apache http client wrapper that offers some convenience methods
   */
  private ScimHttpClient scimHttpClient;

  public RequestBuilder(String baseUrl,
                        ScimClientConfig scimClientConfig,
                        Class<T> responseEntityType,
                        ScimHttpClient scimHttpClient)
  {
    this.baseUrl = baseUrl;
    this.scimClientConfig = scimClientConfig;
    this.responseEntityType = responseEntityType;
    this.scimHttpClient = scimHttpClient;
  }

  /**
   * @param endpoint the resource endpoint path e.g. /Users or /Groups
   */
  protected RequestBuilder<T> setEndpoint(String endpoint)
  {
    this.endpoint = endpoint;
    return this;
  }

  /**
   * @param resource sets the resource that should be sent to the service provider
   */
  protected RequestBuilder<T> setResource(String resource)
  {
    this.resource = resource;
    return this;
  }

  /**
   * @param resource sets the resource that should be sent to the service provider
   */
  protected RequestBuilder<T> setResource(JsonNode resource)
  {
    this.resource = resource.toString();
    return this;
  }

  /**
   * sends the defined request to the service provider
   *
   * @return the response from the given request. A response must not be returned in any case from the service
   *         provider so the returned type is still optional
   */
  public ScimServerResponse<T> sendRequest()
  {
    return this.sendRequest(Collections.emptyMap());
  }

  /**
   * sends the defined request to the service provider
   *
   * @param httpHeaders allows the user to add additional http headers to the request
   * @return the response from the given request. A response must not be returned in any case from the service
   *         provider so the returned type is still optional
   */
  public ScimServerResponse<T> sendRequest(Map<String, String[]> httpHeaders)
  {
    HttpUriRequest request = getHttpUriRequest();
    request.setHeader(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
    if (httpHeaders != null)
    {
      httpHeaders.forEach((key, values) -> {
        for ( String value : values )
        {
          request.addHeader(key, value);
        }
      });
    }
    if (scimClientConfig.getBasicAuth() != null)
    {
      request.setHeader(HttpHeader.AUHORIZATION, scimClientConfig.getBasicAuth().getAuthorizationHeaderValue());
    }
    return handleResponse(scimHttpClient.sendRequest(request));
  }

  /**
   * builds the scim response from the response body
   *
   * @param httpResponseCode the response code of the response
   * @param responseBody the response body of the server
   * @return the response object
   */
  protected abstract <T1 extends ScimResponse> T1 buildScimResponse(int httpResponseCode, String responseBody);

  /**
   * translates the response into a {@link ScimResponse}
   *
   * @param response the response from the scim server
   * @return the parsed scim response object
   */
  private ScimServerResponse<T> handleResponse(HttpResponse response)
  {
    ScimResponse scimResponse = buildScimResponse(response.getHttpStatusCode(), response.getResponseBody());
    response.getResponseHeaders().forEach(scimResponse.getHttpHeaders()::put);
    return ScimServerResponse.<T> builder()
                             .scimResponse(scimResponse)
                             .responseEntityType(responseEntityType)
                             .responseStatus(response.getHttpStatusCode())
                             .build();
  }

  /**
   * builds the request for the server
   */
  protected abstract HttpUriRequest getHttpUriRequest();

}
