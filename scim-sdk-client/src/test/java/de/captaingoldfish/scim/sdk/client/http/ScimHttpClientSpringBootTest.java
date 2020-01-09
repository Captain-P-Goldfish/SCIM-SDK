package de.captaingoldfish.scim.sdk.client.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.captaingoldfish.scim.sdk.client.exceptions.ConnectTimeoutRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.IORuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.SSLHandshakeRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.SocketTimeoutRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.UnknownHostRuntimeException;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreSupporter;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;
import de.captaingoldfish.scim.sdk.client.springboot.AbstractSpringBootWebTest;
import de.captaingoldfish.scim.sdk.client.springboot.SecurityConstants;
import de.captaingoldfish.scim.sdk.client.springboot.SpringBootInitializer;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
@ActiveProfiles(SecurityConstants.X509_PROFILE)
@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {SpringBootInitializer.class})
@TestPropertySource(properties = {"server.ssl.client-auth=need"})
public class ScimHttpClientSpringBootTest extends AbstractSpringBootWebTest
{


  /**
   * the password to open the used keystores in this test
   */
  private static final String KEYSTORE_MASTER_PASSWORD = "123456";

  /**
   * a keystore to enable mutual client authentication on the http client
   */
  private KeyStoreWrapper tlsClientAuthenticationKeystore;

  /**
   * a custom truststore that will be used instead of the default JVM cacert truststore if not null
   */
  private KeyStoreWrapper tlsTruststore;

  /**
   * will initialize the members of this class
   */
  @BeforeEach
  public void initialize()
  {
    KeyStore keyStore = KeyStoreSupporter.readKeyStore(getClass().getResourceAsStream("/test-keys/test.jks"),
                                                       KeyStoreSupporter.KeyStoreType.JKS,
                                                       KEYSTORE_MASTER_PASSWORD);
    tlsClientAuthenticationKeystore = new KeyStoreWrapper(keyStore, KEYSTORE_MASTER_PASSWORD);
    KeyStore truststore = KeyStoreSupporter.readTruststore(getClass().getResourceAsStream("/test-keys/test.jks"),
                                                           KeyStoreSupporter.KeyStoreType.JKS);
    tlsTruststore = new KeyStoreWrapper(truststore, null);
  }

  /**
   * assures that each method must define its own request validation if some is wanted
   */
  @AfterEach
  public void destroy()
  {
    TestController.validateRequest = null;
  }

  /**
   * this test will check that the timeout properties will be set correctly within the http-client
   *
   * @param connectTimeout the connect timeout in seconds
   * @param requestTimeout the request timeout in seconds
   * @param socketTimeout the socket timeout in seconds
   */
  @ParameterizedTest // NOPMD
  @CsvSource({"0,0,0", "1,10,10", "10,1,10", "10,10,1", "0,1,0", "0,10,10", "10,10,0", "10,0,10"})
  public void testGovernikusHttpClientTest(int connectTimeout, int requestTimeout, int socketTimeout) throws IOException
  {
    ScimHttpClient httpClient = ScimHttpClient.builder()
                                              .connectTimeout(connectTimeout)
                                              .requestTimeout(requestTimeout)
                                              .socketTimeout(socketTimeout)
                                              .proxy(null)
                                              .tlsClientAuthenticatonKeystore(tlsClientAuthenticationKeystore)
                                              .truststore(tlsTruststore)
                                              .hostnameVerifier((s, sslSession) -> true)
                                              .build();
    RequestConfig requestConfig = httpClient.getRequestConfig();
    Assertions.assertNotNull(requestConfig);
    final int timeoutMillis = ScimHttpClient.getTIMEOUT_MILLIS();
    final int connectTimeoutInMillis = connectTimeout > 0 ? connectTimeout * timeoutMillis : -1;
    final int requestTimeoutInMillis = requestTimeout > 0 ? requestTimeout * timeoutMillis : -1;
    final int socketTimeoutInMillis = socketTimeout > 0 ? socketTimeout * timeoutMillis : -1;
    Assertions.assertEquals(connectTimeoutInMillis, requestConfig.getConnectTimeout());
    Assertions.assertEquals(requestTimeoutInMillis, requestConfig.getConnectionRequestTimeout());
    Assertions.assertEquals(socketTimeoutInMillis, requestConfig.getSocketTimeout());
    try (CloseableHttpClient client = httpClient.getHttpClient();
      CloseableHttpResponse response = client.execute(new HttpGet(getRequestUrl(TestController.GET_ENDPOINT_PATH))))
    {
      String responseString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
      Assertions.assertEquals(TestController.HELLO_WORLD_RESPONSE_VALUE, responseString);
      Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    }
  }

  /**
   * this test will check that a socket timeout is done if the server needs more than one second to answer
   */
  @Test
  public void testProvokeSocketTimeout()
  {
    int connectTimeout = 10;
    int requestTimeout = 10;
    int socketTimeout = 1;
    ScimHttpClient httpClient = ScimHttpClient.builder()
                                              .connectTimeout(connectTimeout)
                                              .requestTimeout(requestTimeout)
                                              .socketTimeout(socketTimeout)
                                              .proxy(null)
                                              .tlsClientAuthenticatonKeystore(tlsClientAuthenticationKeystore)
                                              .truststore(tlsTruststore)
                                              .hostnameVerifier((s, sslSession) -> true)
                                              .build();
    try
    {
      httpClient.sendRequest(new HttpGet(getRequestUrl(TestController.TIMEOUT_ENDPOINT_PATH)));
      Assertions.fail("this point must not be reached");
    }
    catch (SocketTimeoutRuntimeException e)
    {
      Assertions.assertEquals("socket timeout after '1' seconds", e.getMessage());
      Assertions.assertEquals("Read timed out", e.getCause().getMessage());
    }
  }

  /**
   * this test will check that a connection timeout occurs if the port is wrong for example
   */
  @Test
  public void testProvokeConnectTimeout()
  {
    int connectTimeout = 1;
    int requestTimeout = 10;
    int socketTimeout = 10;
    ScimHttpClient httpClient = ScimHttpClient.builder()
                                              .connectTimeout(connectTimeout)
                                              .requestTimeout(requestTimeout)
                                              .socketTimeout(socketTimeout)
                                              .proxy(null)
                                              .tlsClientAuthenticatonKeystore(tlsClientAuthenticationKeystore)
                                              .truststore(tlsTruststore)
                                              .hostnameVerifier((s, sslSession) -> true)
                                              .build();
    String requestUrl = getRequestUrl(TestController.TIMEOUT_ENDPOINT_PATH).replaceFirst(String.valueOf(getLocalServerPort()),
                                                                                         "connectTimeout");
    try
    {
      httpClient.sendRequest(new HttpGet(requestUrl));
      Assertions.fail("this point must not be reached");
    }
    catch (ConnectTimeoutRuntimeException ex)
    {
      log.debug(ex.getMessage(), ex);
      Assertions.assertEquals("connection timeout after '1' seconds", ex.getMessage());
      Assertions.assertEquals("Connect to localhost:443 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] "
                              + "failed: connect timed out",
                              ex.getCause().getMessage());
    }
    catch (IORuntimeException ex)
    {
      Assertions.assertEquals("communication with server failed", ex.getMessage());
      MatcherAssert.assertThat(ex.getCause().getMessage(), Matchers.containsString("(Connection refused)"));
    }
  }

  /**
   * this test will check that a connection cannot be established if the host is unknown
   */
  @Test
  public void testProvokeUnknownHostException()
  {
    int connectTimeout = 1;
    int requestTimeout = 1;
    int socketTimeout = 1;
    ScimHttpClient httpClient = ScimHttpClient.builder()
                                              .connectTimeout(connectTimeout)
                                              .requestTimeout(requestTimeout)
                                              .socketTimeout(socketTimeout)
                                              .proxy(null)
                                              .tlsClientAuthenticatonKeystore(tlsClientAuthenticationKeystore)
                                              .truststore(tlsTruststore)
                                              .hostnameVerifier((s, sslSession) -> true)
                                              .build();
    String host = UUID.randomUUID().toString();
    String requestUrl = getRequestUrl(TestController.TIMEOUT_ENDPOINT_PATH).replaceFirst("localhost", host);
    try
    {
      httpClient.sendRequest(new HttpGet(requestUrl));
      Assertions.fail("this point must not be reached");
    }
    catch (UnknownHostRuntimeException ex)
    {
      log.debug(ex.getMessage(), ex);
      Assertions.assertEquals("could not find host '" + host + "'", ex.getMessage());
      MatcherAssert.assertThat(ex.getCause().getMessage(), Matchers.containsString(host));
    }
  }

  /**
   * verifies that client authentication works correctly
   */
  @Test
  public void testClientAuthenticationFails()
  {
    int connectTimeout = 1;
    int requestTimeout = 1;
    int socketTimeout = 1;
    ScimHttpClient httpClient = ScimHttpClient.builder()
                                              .connectTimeout(connectTimeout)
                                              .requestTimeout(requestTimeout)
                                              .socketTimeout(socketTimeout)
                                              .proxy(null)
                                              .truststore(tlsTruststore)
                                              .hostnameVerifier((s, sslSession) -> true)
                                              .build();
    try
    {
      httpClient.sendRequest(new HttpGet(getRequestUrl(TestController.TIMEOUT_ENDPOINT_PATH)));
      Assertions.fail("this point must not be reached");
    }
    catch (SSLHandshakeRuntimeException e)
    {
      Assertions.assertEquals("handshake error during connection setup", e.getMessage());
      Assertions.assertEquals("Received fatal alert: bad_certificate", e.getCause().getMessage());
    }
  }

  /**
   * verifies that the client does not trust the server without a proper truststore
   */
  @Test
  public void testServerNotTrusted()
  {
    int connectTimeout = 1;
    int requestTimeout = 1;
    int socketTimeout = 1;
    ScimHttpClient httpClient = ScimHttpClient.builder()
                                              .connectTimeout(connectTimeout)
                                              .requestTimeout(requestTimeout)
                                              .socketTimeout(socketTimeout)
                                              .proxy(null)
                                              .hostnameVerifier((s, sslSession) -> true)
                                              .build();
    try
    {
      httpClient.sendRequest(new HttpGet(getRequestUrl(TestController.TIMEOUT_ENDPOINT_PATH)));
      Assertions.fail("this point must not be reached");
    }
    catch (SSLHandshakeRuntimeException e)
    {
      Assertions.assertEquals("handshake error during connection setup", e.getMessage());
      MatcherAssert.assertThat(e.getCause().getMessage(),
                               Matchers.containsString("unable to find valid certification path to requested target"));
    }
  }

  /**
   * will check that the proxy settings are entered into the http-client
   */
  @Test
  public void testProxySettings()
  {
    final String PROXY_HOST = "localhost";
    final int PROXY_PORT = 8888;
    ProxyHelper proxyHelper = ProxyHelper.builder().systemProxyHost(PROXY_HOST).systemProxyPort(PROXY_PORT).build();
    ScimHttpClient httpClient = ScimHttpClient.builder().proxy(proxyHelper).build();
    RequestConfig requestConfig = httpClient.getRequestConfig();
    Assertions.assertNotNull(requestConfig.getProxy());
    Assertions.assertEquals(PROXY_HOST, requestConfig.getProxy().getHostName());
    Assertions.assertEquals(PROXY_PORT, requestConfig.getProxy().getPort());
  }

}
