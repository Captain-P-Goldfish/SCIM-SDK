package de.captaingoldfish.scim.sdk.client.builder;

import java.util.Map;

import javax.net.ssl.HostnameVerifier;

import de.captaingoldfish.scim.sdk.client.http.BasicAuth;
import de.captaingoldfish.scim.sdk.client.http.ProxyHelper;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 10.12.2019 - 13:39 <br>
 * <br>
 */
@Getter
@NoArgsConstructor
public class ScimClientConfig
{

  /**
   * the default timeout value to use in seconds
   */
  protected static final int DEFAULT_TIMEOUT = 10;

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
  private Map<String, String> httpHeaders;

  /**
   * an optional basic authentication object
   */
  private BasicAuth basicAuth;

  @Builder
  public ScimClientConfig(Integer requestTimeout,
                          Integer socketTimeout,
                          Integer connectTimeout,
                          HostnameVerifier hostnameVerifier,
                          ProxyHelper proxy,
                          KeyStoreWrapper clientAuth,
                          KeyStoreWrapper truststore,
                          Map<String, String> httpHeaders,
                          BasicAuth basicAuth)
  {
    this.requestTimeout = requestTimeout == null ? DEFAULT_TIMEOUT : requestTimeout;
    this.socketTimeout = socketTimeout == null ? DEFAULT_TIMEOUT : socketTimeout;
    this.connectTimeout = connectTimeout == null ? DEFAULT_TIMEOUT : connectTimeout;
    this.hostnameVerifier = hostnameVerifier;
    this.proxy = proxy;
    this.clientAuth = clientAuth;
    this.truststore = truststore;
    this.httpHeaders = httpHeaders;
    this.basicAuth = basicAuth;
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
