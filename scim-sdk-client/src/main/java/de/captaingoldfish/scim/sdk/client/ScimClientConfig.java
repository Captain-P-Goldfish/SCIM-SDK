package de.captaingoldfish.scim.sdk.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.net.ssl.HostnameVerifier;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClientBuilder;

import de.captaingoldfish.scim.sdk.client.http.BasicAuth;
import de.captaingoldfish.scim.sdk.client.http.ConfigManipulator;
import de.captaingoldfish.scim.sdk.client.http.ProxyHelper;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 10.12.2019 - 13:39 <br>
 * <br>
 */
@Getter
@Setter
@NoArgsConstructor
public class ScimClientConfig
{

  /**
   * the default timeout value to use in seconds
   */
  public static final int DEFAULT_TIMEOUT = 10;

  /**
   * request timeout in seconds
   */
  private int requestTimeout;

  /**
   * socket timeout in seconds
   */
  private int socketTimeout;

  /**
   * connect timeout in seconds
   */
  private int connectTimeout;

  /**
   * if cookie management should be enabled or not. Default is false.
   */
  private boolean enableCookieManagement;

  /**
   * if large request operation lists should automatically be split into several requests based on the
   * maxOperations value from the service provider. Please note that a failed request in the middle of the
   * process might cause unwanted results on the server that need to be resolved manually.
   */
  private boolean enableAutomaticBulkRequestSplitting;

  /**
   * the hostname verifier that should be used in the requests
   */
  private HostnameVerifier hostnameVerifier;

  /**
   * proxy if the request must be sent through a proxy
   */
  private ProxyHelper proxy;

  /**
   * the keystore that should be used for client authentication
   */
  private KeyStoreWrapper clientAuth;

  /**
   * the truststore to trust the server
   */
  private KeyStoreWrapper truststore;

  /**
   * additional http headers that may be used to authorize at the scim server
   */
  private Map<String, String[]> httpHeaders;

  /**
   * normally SCIM responses must have set the http-header "application/scim+json". But some providers are not
   * providing these headers. If so, set this map in order to modify the check of the response headers from the
   * server. <br>
   * <ul>
   * <li>null: The headers are checked as normally for the content-type "application/scim+json"</li>
   * <li>empty map: The check of response headers is disabled</li>
   * <li>filled map: The check of the response headers will be done with the entries of this map</li>
   * </ul>
   */
  private Map<String, String> expectedHttpResponseHeaders;

  /**
   * an optional basic authentication object
   */
  private BasicAuth basicAuth;

  /**
   * may be used to manipulate the apache configuration before the http client is created
   */
  private ConfigManipulator configManipulator;

  /**
   * if the filter-expression-comparators should be sent in lowercase instead of uppercase e.g.: eq instead of
   * EQ.
   */
  private boolean useLowerCaseInFilterComparators;

  /**
   * a string value describing the TLS version that is used for SSLContexts
   */
  private String tlsVersion;

  /**
   * a {@link HttpClientBuilder} instance for custom HTTP clients
   */
  private HttpClientBuilder httpClientBuilder;

  /**
   * the maximum number of operations in a single patch requests. This can be useful if a provider is not
   * capable of handling large number of operations in a single request. Value "0" or lower indicates that there
   * is no restriction and that all operations are sent in a single request.
   */
  private int maxPatchOperationsPerRequest;

  // @formatter:off
  /**
   * an optional converter method that allows to modify the responses into specific customized responses for
   * special use-cases. Here is an example of how to make use of it:
   *
   * <pre>
   *
   * {@code
   *
   * @Data
   * @EqualsAndHashCode(callSuper = true)
   * public class RemoteProviderResponse<T extends ScimObjectNode> extends ServerResponse<T>
   * {
   *
   *   private Exception exception;
   *
   *
   *   public RemoteProviderResponse(ServerResponse<T> serverResponse)
   *   {
   *     super(serverResponse.getHttpResponse(), serverResponse.isSuccess(), serverResponse.getResponseBody(),
   *           serverResponse.isValidScimResponse(), serverResponse.getHttpStatus(), serverResponse.getType(),
   *           serverResponse.getIsResponseParseable(), serverResponse.getRequiredResponseHeaders());
   *   }
   *
   *   public static Function<ServerResponse<? extends ScimObjectNode>, ? extends ServerResponse<? extends ScimObjectNode>> getResponseConverter()
   *   {
   *     return RemoteProviderResponse::new;
   *   }
   * }
   * }
   * </pre>
   */
   // @formatter:on
  private Function<ServerResponse<? extends ScimObjectNode>, ? extends ServerResponse<? extends ScimObjectNode>> responseConverter;

  @Builder
  public ScimClientConfig(Integer requestTimeout,
                          Integer socketTimeout,
                          Integer connectTimeout,
                          boolean enableCookieManagement,
                          boolean enableAutomaticBulkRequestSplitting,
                          HostnameVerifier hostnameVerifier,
                          ProxyHelper proxy,
                          KeyStoreWrapper clientAuth,
                          KeyStoreWrapper truststore,
                          Map<String, String> httpHeaders,
                          Map<String, String[]> httpMultiHeaders,
                          BasicAuth basicAuth,
                          ConfigManipulator configManipulator,
                          boolean useLowerCaseInFilterComparators,
                          Map<String, String> expectedHttpResponseHeaders,
                          String tlsVersion,
                          HttpClientBuilder httpClientBuilder,
                          int maxPatchOperationsPerRequest,
                          Function<ServerResponse<? extends ScimObjectNode>, ? extends ServerResponse<? extends ScimObjectNode>> responseConverter)
  {
    this.requestTimeout = requestTimeout == null ? DEFAULT_TIMEOUT : requestTimeout;
    this.socketTimeout = socketTimeout == null ? DEFAULT_TIMEOUT : socketTimeout;
    this.connectTimeout = connectTimeout == null ? DEFAULT_TIMEOUT : connectTimeout;
    this.enableCookieManagement = enableCookieManagement;
    this.enableAutomaticBulkRequestSplitting = enableAutomaticBulkRequestSplitting;
    this.hostnameVerifier = hostnameVerifier;
    this.proxy = proxy;
    this.clientAuth = clientAuth;
    this.truststore = truststore;
    setHeaders(httpHeaders, httpMultiHeaders);
    this.basicAuth = basicAuth;
    this.configManipulator = configManipulator;
    this.useLowerCaseInFilterComparators = useLowerCaseInFilterComparators;
    this.expectedHttpResponseHeaders = expectedHttpResponseHeaders;
    this.tlsVersion = Optional.ofNullable(tlsVersion).map(StringUtils::stripToNull).orElse("TLSv1.2");
    this.httpClientBuilder = httpClientBuilder;
    this.maxPatchOperationsPerRequest = maxPatchOperationsPerRequest;
    this.responseConverter = responseConverter;
  }

  /**
   * merges the values of the single headers map and the multi-headers map into a single map
   */
  private void setHeaders(Map<String, String> httpSingleHeaders, Map<String, String[]> httpMultiHeaders)
  {
    this.httpHeaders = new HashMap<>();
    if (httpSingleHeaders != null)
    {
      httpSingleHeaders.forEach((key, value) -> this.httpHeaders.put(key, new String[]{value}));
    }
    if (httpMultiHeaders != null)
    {
      httpMultiHeaders.forEach((key, valueArray) -> {
        String[] multiValues = this.httpHeaders.get(key);
        if (multiValues == null)
        {
          this.httpHeaders.put(key, valueArray);
        }
        else
        {
          List<String> headerList = new ArrayList<>(Arrays.asList(multiValues));
          headerList.addAll(Arrays.asList(valueArray));
          this.httpHeaders.put(key, headerList.toArray(new String[0]));
        }
      });
    }
  }

  /**
   * Get the custom HttpClientBuilder if set, get a default HttpClientBuilder otherwise.
   */
  public HttpClientBuilder getHttpClientBuilder()
  {
    return this.httpClientBuilder == null ? HttpClientBuilder.create() : this.httpClientBuilder;
  }

  /**
   * override lombok builder
   */
  public static class ScimClientConfigBuilder
  {

    public ScimClientConfigBuilder basic(String username, String password)
    {
      basicAuth = BasicAuth.builder().username(username).password(password).build();
      return this;
    }

  }
}
