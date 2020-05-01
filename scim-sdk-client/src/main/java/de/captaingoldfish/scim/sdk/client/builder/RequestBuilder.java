package de.captaingoldfish.scim.sdk.client.builder;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.apache.http.client.methods.HttpUriRequest;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.AccessLevel;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 07.12.2019 - 23:15 <br>
 * <br>
 * an abstract request builder implementation
 */
public abstract class RequestBuilder<T extends ScimObjectNode>
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
                        String endpoint,
                        ScimClientConfig scimClientConfig,
                        Class<T> responseEntityType,
                        ScimHttpClient scimHttpClient)
  {
    this.baseUrl = baseUrl;
    this.endpoint = endpoint;
    this.scimClientConfig = scimClientConfig;
    this.responseEntityType = responseEntityType;
    this.scimHttpClient = scimHttpClient;
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
  public ServerResponse<T> sendRequest()
  {
    return this.sendRequest(Collections.emptyMap());
  }

  /**
   * tells this abstract class if the http status from the server is the expected success status
   *
   * @param httpStatus the http status from the server
   * @return true if the response status shows success
   */
  protected abstract boolean isExpectedResponseCode(int httpStatus);

  /**
   * an optional method that might be used by a builder to verify if the response can be parsed into the
   * expected resource type
   */
  protected Function<HttpResponse, Boolean> isResponseParseable()
  {
    return httpResponse -> false;
  }

  /**
   * sends the defined request to the service provider
   *
   * @param httpHeaders allows the user to add additional http headers to the request
   * @return the response from the given request. A response must not be returned in any case from the service
   *         provider so the returned type is still optional
   */
  public ServerResponse<T> sendRequest(Map<String, String[]> httpHeaders)
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
    HttpResponse response = scimHttpClient.sendRequest(request);
    return new ServerResponse<>(response, isExpectedResponseCode(response.getHttpStatusCode()), responseEntityType,
                                isResponseParseable());
  }

  /**
   * builds the request for the server
   */
  protected abstract HttpUriRequest getHttpUriRequest();

}
