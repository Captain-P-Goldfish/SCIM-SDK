package de.captaingoldfish.scim.sdk.client.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.http.client.methods.HttpUriRequest;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 07.12.2019 - 23:15 <br>
 * <br>
 * an abstract request builder implementation
 */
@Slf4j
public abstract class RequestBuilder<T extends ScimObjectNode>
{

  /**
   * the base url to the scim service
   */
  @Getter(AccessLevel.PROTECTED)
  private final String baseUrl;

  /**
   * the resource endpoint path e.g. /Users or /Groups
   */
  @Getter(AccessLevel.PROTECTED)
  private String endpoint;

  /**
   * the resource that should be sent to the service provider
   */
  @Getter
  private String resource;

  /**
   * the expected resource type
   */
  @Getter(AccessLevel.PROTECTED)
  private Class<T> responseEntityType;

  /**
   * an apache http client wrapper that offers some convenience methods
   */
  @Getter(AccessLevel.PROTECTED)
  private ScimHttpClient scimHttpClient;

  /**
   * the headers that are expected to be returned from the server. If any of the required response headers is
   * missing the response will be marked as not successful
   */
  @Getter
  private Map<String, String> requiredHttpHeaders;

  public RequestBuilder(String baseUrl, String endpoint, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
  {
    this.baseUrl = baseUrl;
    this.endpoint = endpoint;
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
   * @return the response from the given request. A response must not be returned in any case from the service
   *         provider so the returned type is still optional
   */
  public <R extends ServerResponse<T>> R sendRequest()
  {
    return this.sendRequestWithMultiHeaders(Collections.emptyMap());
  }

  /**
   * sends the defined request to the service provider
   *
   * @param httpHeaders allows the user to add additional http headers to the request
   * @return the response from the given request. A response must not be returned in any case from the service
   *         provider so the returned type is still optional
   */
  public <R extends ServerResponse<T>> R sendRequestWithMultiHeaders(Map<String, String[]> httpHeaders)
  {
    HttpUriRequest request = getHttpUriRequest();
    request.setHeader(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
    addHeaderToRequest(scimHttpClient.getScimClientConfig().getHttpHeaders(), httpHeaders, request);
    if (scimHttpClient.getScimClientConfig().getBasicAuth() != null)
    {
      request.setHeader(HttpHeader.AUTHORIZATION,
                        scimHttpClient.getScimClientConfig().getBasicAuth().getAuthorizationHeaderValue());
    }
    HttpResponse response = scimHttpClient.sendRequest(request);
    return (R)toResponse(response);
  }

  /**
   * adds the http headers to the current request
   *
   * @param defaultHeaders the default http headers from the
   *          {@link de.captaingoldfish.scim.sdk.client.ScimClientConfig}. these headers will be overridden by
   *          the map from {@link #sendRequest(Map)} if duplicate keys are present
   * @param preferredHeaders the http headers that have been added to the {@link #sendRequest(Map)} method. This
   *          map takes precedence for the default headers set in the
   *          {@link de.captaingoldfish.scim.sdk.client.ScimClientConfig}
   * @param request the request object to which these http headers will be added
   */
  protected void addHeaderToRequest(Map<String, String[]> defaultHeaders,
                                    Map<String, String[]> preferredHeaders,
                                    HttpUriRequest request)
  {
    Consumer<Map<String, String[]>> addHeaders = headerMap -> {
      if (headerMap == null)
      {
        return;
      }
      headerMap.forEach((key, values) -> {
        request.removeHeaders(key);
        for ( String value : values )
        {
          request.addHeader(key, value);
        }
      });
    };
    addHeaders.accept(defaultHeaders);
    addHeaders.accept(preferredHeaders);
  }

  /**
   * sends the defined request to the service provider
   *
   * @return the response from the given request. A response must not be returned in any case from the service
   *         provider so the returned type is still optional
   */
  public <R extends ServerResponse<T>> R sendRequest(Map<String, String> headers)
  {
    Map<String, String[]> multiHeader = new HashMap<>();
    headers.forEach((key, value) -> multiHeader.put(key, new String[]{value}));
    return this.sendRequestWithMultiHeaders(multiHeader);
  }

  /**
   * moved into its own method to override the returned class in the list-builder that has a sub-generic type
   */
  protected <R extends ServerResponse<T>> R toResponse(HttpResponse response)
  {
    ServerResponse<T> serverResponse = new ServerResponse<>(response,
                                                            isExpectedResponseCode(response.getHttpStatusCode()),
                                                            responseEntityType, isResponseParseable(),
                                                            getRequiredResponseHeaders());
    if (scimHttpClient.getScimClientConfig().getResponseConverter() == null)
    {
      return (R)serverResponse;
    }
    else
    {
      return (R)scimHttpClient.getScimClientConfig().getResponseConverter().apply(serverResponse);
    }
  }

  /**
   * this is the default implementation for the expected response headers that should be present within the
   * response. This is based on request since the delete request does not require a content type
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/313
   */
  protected Map<String, String> getRequiredResponseHeaders()
  {
    if (requiredHttpHeaders != null)
    {
      return requiredHttpHeaders;
    }
    if (scimHttpClient.getScimClientConfig().getExpectedHttpResponseHeaders() != null)
    {
      return scimHttpClient.getScimClientConfig().getExpectedHttpResponseHeaders();
    }
    Map<String, String> requiredHttpHeaders = new HashMap<>();
    requiredHttpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
    return requiredHttpHeaders;
  }

  /**
   * Set this if the SCIM provider is not behaving SCIM compliant by manipulating the expected headers that will
   * be returned from the server
   * <ul>
   * <li>null: The headers are checked as normally for the content-type "application/scim+json"</li>
   * <li>empty map: The check of response headers is disabled</li>
   * <li>filled map: The check of the response headers will be done with the entries of this map</li>
   * </ul>
   *
   * @param resource sets the resource that should be sent to the service provider
   */
  protected RequestBuilder<T> setExpectedResponseHeaders(Map<String, String> requiredResponseHeaders)
  {
    this.requiredHttpHeaders = requiredResponseHeaders;
    return this;
  }

  /**
   * builds the request for the server
   */
  protected abstract HttpUriRequest getHttpUriRequest();

}
