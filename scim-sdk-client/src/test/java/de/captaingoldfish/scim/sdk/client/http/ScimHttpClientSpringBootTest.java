package de.captaingoldfish.scim.sdk.client.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.captaingoldfish.scim.sdk.client.exceptions.ConnectTimeoutRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.SSLHandshakeRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.SocketTimeoutRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.UnknownHostRuntimeException;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreSupporter;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;
import de.captaingoldfish.scim.sdk.client.springboot.AbstractSpringBootWebTest;
import de.captaingoldfish.scim.sdk.client.springboot.SpringBootInitializer;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {SpringBootInitializer.class})
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
                                              .proxyHelper(null)
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
                                              .proxyHelper(null)
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
                                              .proxyHelper(null)
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
    catch (ConnectTimeoutRuntimeException e)
    {
      log.debug(e.getMessage(), e);
      Assertions.assertEquals("connection timeout after '1' seconds", e.getMessage());
      Assertions.assertEquals("Connect to localhost:443 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] "
                              + "failed: connect timed out",
                              e.getCause().getMessage());
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
                                              .proxyHelper(null)
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
                                              .proxyHelper(null)
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
                                              .proxyHelper(null)
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
      Assertions.assertEquals("sun.security.validator.ValidatorException: PKIX path building failed: "
                              + "sun.security.provider.certpath.SunCertPathBuilderException: "
                              + "unable to find valid certification path to requested target",
                              e.getCause().getMessage());
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
    ScimHttpClient httpClient = ScimHttpClient.builder().proxyHelper(proxyHelper).build();
    RequestConfig requestConfig = httpClient.getRequestConfig();
    Assertions.assertNotNull(requestConfig.getProxy());
    Assertions.assertEquals(PROXY_HOST, requestConfig.getProxy().getHostName());
    Assertions.assertEquals(PROXY_PORT, requestConfig.getProxy().getPort());
  }

  /**
   * a test-controller to access this test-springboot-application via the {@link ScimHttpClient}
   */
  @Controller
  public static class TestController
  {

    /**
     * the context path to the test-controller endpoint {@link #helloWorldEndpoint(HttpServletRequest)}
     */
    public static final String GET_ENDPOINT_PATH = "get-endpoint";

    /**
     * this endpoint is supposed to provoke an error due to a thread.sleep
     */
    public static final String TIMEOUT_ENDPOINT_PATH = "bad-test-endpoint";

    /**
     * this context path points to a method that will accept post requests
     */
    public static final String POST_ENDPOINT_PATH = "post-test-endpoint";

    /**
     * the value that is returned from the test-endpoint
     */
    public static final String HELLO_WORLD_RESPONSE_VALUE = "hello world";

    /**
     * this consumer can be used to validate the incoming requests on this controller
     */
    public static BiConsumer<HttpServletRequest, String> validateRequest;

    /**
     * a controller endpoint that will be scanned by
     * {@link de.governikus.autent.web.utils.springboot.WebAppConfig}
     *
     * @return "hello world"
     */
    @RequestMapping(GET_ENDPOINT_PATH)
    public @ResponseBody String helloWorldEndpoint(HttpServletRequest request)
    {
      if (validateRequest != null)
      {
        validateRequest.accept(request, null);
      }
      return HELLO_WORLD_RESPONSE_VALUE;
    }

    /**
     * this endpoint accepts post requests
     *
     * @return "hello world"
     */
    @RequestMapping(value = POST_ENDPOINT_PATH, method = RequestMethod.POST)
    public @ResponseBody String postEndpointPath(HttpServletRequest request, @RequestBody String requestBody)
    {
      if (validateRequest != null)
      {
        validateRequest.accept(request, requestBody);
      }
      return HELLO_WORLD_RESPONSE_VALUE;
    }

    /**
     * this method will be used to provoke a timeout for the tests
     */
    @RequestMapping(TIMEOUT_ENDPOINT_PATH)
    public @ResponseBody String blockingEndpoint() throws InterruptedException
    {
      final int twoSeconds = 2000;
      Thread.sleep(twoSeconds);
      return HELLO_WORLD_RESPONSE_VALUE;
    }
  }

  /**
   * spring security configuration for this test that will enable mutual client authentication to test the http
   * tls client authentication
   */
  @Order(ConfigureTestControllerWithSecurity.RANDOM_ORDER_NUMBER)
  @Configuration
  @EnableWebSecurity
  public static class ConfigureTestControllerWithSecurity extends WebSecurityConfigurerAdapter
  {

    /**
     * a order number that is given to this configuration that should not have any conflicts with other
     * spring-security configurations
     */
    public static final int RANDOM_ORDER_NUMBER = 499;

    /**
     * configure the endpoints that require mutual client authentication and add the regular expression to match
     * the username within the certificates distinguished name
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception
    {
      http.csrf()
          .disable()
          .authorizeRequests()
          .antMatchers(TestController.GET_ENDPOINT_PATH, TestController.TIMEOUT_ENDPOINT_PATH)
          .authenticated()
          .and()
          .x509()
          .subjectPrincipalRegex("CN=(.*)"); // the regular expression to parse the username from the DN
    }

    /**
     * will do the authentication if a request comes in. The CN of the certificate must match "test" to
     * successfully authenticate
     */
    @Bean
    public UserDetailsService userDetailsService()
    {
      return username -> {
        if ("test".equals(username))
        {
          return new User(username, "", AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
        }
        else
        {
          return null;
        }
      };
    }
  }

}
