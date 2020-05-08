package de.captaingoldfish.scim.sdk.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;

import de.captaingoldfish.scim.sdk.client.http.BasicAuth;
import de.captaingoldfish.scim.sdk.client.http.ConfigManipulator;
import de.captaingoldfish.scim.sdk.client.http.ProxyHelper;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;
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
   * an optional basic authentication object
   */
  private BasicAuth basicAuth;

  /**
   * may be used to manipulate the apache configuration before the http client is created
   */
  private ConfigManipulator configManipulator;

  @Builder
  public ScimClientConfig(Integer requestTimeout,
                          Integer socketTimeout,
                          Integer connectTimeout,
                          HostnameVerifier hostnameVerifier,
                          ProxyHelper proxy,
                          KeyStoreWrapper clientAuth,
                          KeyStoreWrapper truststore,
                          Map<String, String> httpHeaders,
                          Map<String, String[]> httpMultiHeaders,
                          BasicAuth basicAuth,
                          ConfigManipulator configManipulator)
  {
    this.requestTimeout = requestTimeout == null ? DEFAULT_TIMEOUT : requestTimeout;
    this.socketTimeout = socketTimeout == null ? DEFAULT_TIMEOUT : socketTimeout;
    this.connectTimeout = connectTimeout == null ? DEFAULT_TIMEOUT : connectTimeout;
    this.hostnameVerifier = hostnameVerifier;
    this.proxy = proxy;
    this.clientAuth = clientAuth;
    this.truststore = truststore;
    setHeaders(httpHeaders, httpMultiHeaders);
    this.basicAuth = basicAuth;
    this.configManipulator = configManipulator;
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
