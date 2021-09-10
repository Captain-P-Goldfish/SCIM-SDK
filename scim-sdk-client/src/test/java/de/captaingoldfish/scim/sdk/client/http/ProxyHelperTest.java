package de.captaingoldfish.scim.sdk.client.http;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
@Slf4j
public class ProxyHelperTest
{

  /**
   * default host to use
   */
  private static final String PROXY_HOST = "localhost";

  /**
   * default proxy username to use
   */
  private static final String PROXY_USERNAME = "max";

  /**
   * default proxy password to use
   */
  private static final String PROXY_PASSWORD = "mustermann";

  /**
   * will assert that an apache http client can be build with the proxy helper in a easy way
   */
  @Test
  public void testProxyHelper()
  {
    final String host = PROXY_HOST;
    final int port = 8888;
    final String proxyUsername = PROXY_USERNAME;
    final String proxyPassword = PROXY_PASSWORD;
    ProxyHelper proxyHelper = ProxyHelper.builder()
                                         .systemProxyHost(host)
                                         .systemProxyPort(port)
                                         .systemProxyUsername(proxyUsername)
                                         .systemProxyPassword(proxyPassword)
                                         .build();
    RequestConfig requestConfig = proxyHelper.getProxyConfig();
    Assertions.assertNotNull(requestConfig.getProxy());
    Assertions.assertNotEquals(RequestConfig.DEFAULT, proxyHelper.getProxyConfig());
    Assertions.assertEquals(host, requestConfig.getProxy().getHostName());
    Assertions.assertEquals(port, requestConfig.getProxy().getPort());

    Assertions.assertEquals(host, proxyHelper.getSystemProxyHost());
    Assertions.assertEquals(port, proxyHelper.getSystemProxyPort());
    Assertions.assertEquals(proxyUsername, proxyHelper.getSystemProxyUsername());
    Assertions.assertEquals(proxyPassword, proxyHelper.getSystemProxyPassword());

    if (proxyHelper.isProxySet())
    {
      HttpClientBuilder.create()
                       .setDefaultRequestConfig(requestConfig)
                       .setDefaultCredentialsProvider(proxyHelper.getProxyCredentials())
                       .build();
    }
    else
    {
      Assertions.fail("proxy was not set");
    }
  }

  /**
   * will assert that an apache http client can be build with the proxy helper in a easy way
   */
  @Test
  public void testProxyHelperSetter()
  {
    final String host = PROXY_HOST;
    final int port = 8888;
    final String proxyUsername = PROXY_USERNAME;
    final String proxyPassword = PROXY_PASSWORD;
    ProxyHelper proxyHelper = ProxyHelper.builder().build();
    proxyHelper.setSystemProxyHost(host);
    proxyHelper.setSystemProxyPort(port);
    proxyHelper.setSystemProxyUsername(proxyUsername);
    proxyHelper.setSystemProxyPassword(proxyPassword);

    RequestConfig requestConfig = proxyHelper.getProxyConfig();
    Assertions.assertNotNull(requestConfig.getProxy());
    Assertions.assertNotEquals(RequestConfig.DEFAULT, proxyHelper.getProxyConfig());
    Assertions.assertEquals(host, requestConfig.getProxy().getHostName());
    Assertions.assertEquals(port, requestConfig.getProxy().getPort());

    Assertions.assertEquals(host, proxyHelper.getSystemProxyHost());
    Assertions.assertEquals(port, proxyHelper.getSystemProxyPort());
    Assertions.assertEquals(proxyUsername, proxyHelper.getSystemProxyUsername());
    Assertions.assertEquals(proxyPassword, proxyHelper.getSystemProxyPassword());

    if (proxyHelper.isProxySet())
    {
      HttpClientBuilder.create()
                       .setDefaultRequestConfig(requestConfig)
                       .setDefaultCredentialsProvider(proxyHelper.getProxyCredentials())
                       .build();
    }
    else
    {
      Assertions.fail("proxy was not set");
    }
  }

  /**
   * will assert that an apache http client can be build with the proxy helper in a easy way
   */
  @Test
  public void testProxyHelperWithPortAsString()
  {
    final String host = PROXY_HOST;
    final String port = "8888";
    final String proxyUsername = PROXY_USERNAME;
    final String proxyPassword = PROXY_PASSWORD;
    ProxyHelper proxyHelper = ProxyHelper.builder()
                                         .systemProxyHost(host)
                                         .systemProxyPort(port)
                                         .systemProxyUsername(proxyUsername)
                                         .systemProxyPassword(proxyPassword)
                                         .build();
    RequestConfig requestConfig = proxyHelper.getProxyConfig();
    Assertions.assertNotNull(requestConfig.getProxy());
    Assertions.assertNotEquals(RequestConfig.DEFAULT, proxyHelper.getProxyConfig());
    Assertions.assertEquals(host, requestConfig.getProxy().getHostName());
    Assertions.assertEquals(Integer.parseInt(port), requestConfig.getProxy().getPort());

    if (proxyHelper.isProxySet())
    {
      HttpClientBuilder.create()
                       .setDefaultRequestConfig(requestConfig)
                       .setDefaultCredentialsProvider(proxyHelper.getProxyCredentials())
                       .build();
    }
    else
    {
      Assertions.fail("proxy was not set");
    }
  }

  /**
   * will assert that the default configuration is returned if no proxy details have been set
   */
  @Test
  public void testProxyHelperWithoutAnyProxySettings()
  {
    final String host = null;
    final int port = 0;
    final String proxyUsername = null;
    final String proxyPassword = null;
    ProxyHelper proxyHelper = ProxyHelper.builder()
                                         .systemProxyHost(host)
                                         .systemProxyPort(port)
                                         .systemProxyUsername(proxyUsername)
                                         .systemProxyPassword(proxyPassword)
                                         .build();
    RequestConfig requestConfig = proxyHelper.getProxyConfig();
    Assertions.assertEquals(RequestConfig.DEFAULT, proxyHelper.getProxyConfig());
    Assertions.assertNull(requestConfig.getProxy());
    Assertions.assertNull(proxyHelper.getProxyCredentials());
    Assertions.assertFalse(proxyHelper.isProxySet());
  }

  /**
   * will assert that the usernamePasswordCredentials are null if no proxy authentication has been set
   */
  @Test
  public void testProxyHelperWithoutProxyAuthentication()
  {
    final String host = PROXY_HOST;
    final String port = "8888";
    final String proxyUsername = null;
    final String proxyPassword = null;
    ProxyHelper proxyHelper = ProxyHelper.builder()
                                         .systemProxyHost(host)
                                         .systemProxyPort(port)
                                         .systemProxyUsername(proxyUsername)
                                         .systemProxyPassword(proxyPassword)
                                         .build();
    RequestConfig requestConfig = proxyHelper.getProxyConfig();
    Assertions.assertNotNull(requestConfig.getProxy());
    Assertions.assertNotEquals(RequestConfig.DEFAULT, proxyHelper.getProxyConfig());
    Assertions.assertEquals(host, requestConfig.getProxy().getHostName());
    Assertions.assertEquals(Integer.parseInt(port), requestConfig.getProxy().getPort());
    Assertions.assertNull(proxyHelper.getProxyCredentials());

    if (proxyHelper.isProxySet())
    {
      HttpClientBuilder.create()
                       .setDefaultRequestConfig(requestConfig)
                       .setDefaultCredentialsProvider(proxyHelper.getProxyCredentials())
                       .build();
    }
    else
    {
      Assertions.fail("proxy was not set");
    }
  }

  /**
   * will assert that an exception is thrown if port is null
   */
  @Test
  public void testProxyHelperWithInvalidPort()
  {
    final String host = PROXY_HOST;
    final String port = null;
    final String proxyUsername = PROXY_USERNAME;
    final String proxyPassword = PROXY_PASSWORD;
    Assertions.assertThrows(IllegalArgumentException.class,
                            () -> ProxyHelper.builder()
                                             .systemProxyHost(host)
                                             .systemProxyPort(port)
                                             .systemProxyUsername(proxyUsername)
                                             .systemProxyPassword(proxyPassword)
                                             .build());
  }

  /**
   * will assert that an exception is thrown if port has an invalid string
   */
  @Test
  public void testProxyHelperWithInvalidPort2()
  {
    final String host = PROXY_HOST;
    final String port = "abcd";
    final String proxyUsername = PROXY_USERNAME;
    final String proxyPassword = PROXY_PASSWORD;
    Assertions.assertThrows(IllegalArgumentException.class,
                            () -> ProxyHelper.builder()
                                             .systemProxyHost(host)
                                             .systemProxyPort(port)
                                             .systemProxyUsername(proxyUsername)
                                             .systemProxyPassword(proxyPassword)
                                             .build());
  }

  /**
   * verifies that the proxy credentials will be ignored if the password has not been set
   */
  @Test
  public void testSetProxyUsernameWithoutPassword()
  {
    ProxyHelper proxyHelper = ProxyHelper.builder()
                                         .systemProxyHost("localhost")
                                         .systemProxyPort(8888)
                                         .systemProxyUsername("goldfish")
                                         .build();
    Assertions.assertNull(proxyHelper.getProxyCredentials());
  }

}
