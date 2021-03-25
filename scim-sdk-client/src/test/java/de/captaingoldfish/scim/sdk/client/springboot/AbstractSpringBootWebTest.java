package de.captaingoldfish.scim.sdk.client.springboot;

import java.security.KeyStore;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreSupporter;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;
import de.captaingoldfish.scim.sdk.client.setup.scim.ScimConfig;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author: Pascal Knueppel <br>
 * created at: 09.12.2019 15:44 <br>
 * <br>
 * an abstract testclass that that is used as helper class to access the current port of the application and
 * to build URLs that can be used to access the application
 */
@Slf4j
public abstract class AbstractSpringBootWebTest
{

  /**
   * the password to open the used keystores in this test
   */
  private static final String KEYSTORE_MASTER_PASSWORD = "123456";

  /**
   * may be used to validate the incoming http headers
   */
  public static Consumer<Map<String, String>> headerValidator;

  /**
   * if spring boot test uses a random port the port will be injected into this variable
   */
  @Getter
  @LocalServerPort
  private int localServerPort;

  /**
   * contains the URL to which the requests must be sent
   */
  private String defaultUrl;

  /**
   * will initialize the url under which the locally started tomcat can be reached
   */
  @BeforeEach
  public void initializeUrl()
  {
    defaultUrl = "https://localhost:" + localServerPort;
  }

  /**
   * returns static set variables to null
   */
  @AfterEach
  public void resetStaticContext()
  {
    headerValidator = null;
    TestController.responseSupplier = null;
  }

  /**
   * this method will create a request url with the given path
   *
   * @param path the context path to the method that should be used
   * @return the complete server-url with the given context path to the method that should be used
   */
  public String getRequestUrl(String path)
  {
    return defaultUrl + (path.charAt(0) == '/' ? path : "/" + path);
  }

  /**
   * @return the client keystore that can be used for authentication on tls level
   */
  protected KeyStoreWrapper getClientAuthKeystore()
  {
    KeyStore keyStore = KeyStoreSupporter.readKeyStore(getClass().getResourceAsStream("/test-keys/test.jks"),
                                                       KeyStoreSupporter.KeyStoreType.JKS,
                                                       KEYSTORE_MASTER_PASSWORD);
    return new KeyStoreWrapper(keyStore, KEYSTORE_MASTER_PASSWORD);
  }

  /**
   * @return the client keystore that can be used for authentication on tls level
   */
  protected KeyStoreWrapper getUnauthorizedClientAuthKeystore()
  {
    KeyStore keyStore = KeyStoreSupporter.readKeyStore(getClass().getResourceAsStream("/test-keys/test-unauthorized"
                                                                                      + ".jks"),
                                                       KeyStoreSupporter.KeyStoreType.JKS,
                                                       KEYSTORE_MASTER_PASSWORD);
    return new KeyStoreWrapper(keyStore, KEYSTORE_MASTER_PASSWORD);
  }

  /**
   * @return the truststore to trust the self signed certificate of the tomcat server
   */
  protected KeyStoreWrapper getTruststore()
  {
    KeyStore truststore = KeyStoreSupporter.readTruststore(getClass().getResourceAsStream("/test-keys/test.jks"),
                                                           KeyStoreSupporter.KeyStoreType.JKS);
    return new KeyStoreWrapper(truststore, null);
  }

  /**
   * a test-controller to access this test-springboot-application via the {@link ScimHttpClient}
   */
  @Controller
  public static class TestController
  {

    /**
     * the context path to the test-controller endpoint with the scim endpoint
     */
    public static final String SCIM_ENDPOINT_PATH = "scim/v2";

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
     * can be used to provide a custom response. Setting this will disable the execution of the actual scim
     * endpoint so remember to set it back to null
     */
    public static Supplier<String> responseSupplier;

    /**
     * used to set a scim endpoint in the tests
     */
    private ScimConfig scimConfig = new ScimConfig();

    /**
     * will set authorization into the scim configuration endpoints which wil make sure that the client will not
     * be able to access the scim endpoints without being authenticated
     */
    @PostConstruct
    public void initializeScimConfig()
    {
      scimConfig.getUserResourceType().getFeatures().getAuthorization().setRoles(SecurityConstants.SUPER_ADMIN_ROLE);
      scimConfig.getGroupResourceType().getFeatures().getAuthorization().setRoles(SecurityConstants.SUPER_ADMIN_ROLE);
    }

    /**
     * a controller endpoint that will be scanned by {@link WebAppConfig}
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

    /**
     * this method will be used to provoke a timeout for the tests
     */
    @RequestMapping(value = SCIM_ENDPOINT_PATH
                            + "/**", method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.PUT,
                                               RequestMethod.PATCH,
                                               RequestMethod.DELETE}, produces = HttpHeader.SCIM_CONTENT_TYPE)
    public @ResponseBody String handleScimRequest(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  @RequestBody(required = false) String requestBody,
                                                  Principal principal)
    {
      if (responseSupplier != null)
      {
        return responseSupplier.get();
      }
      if (principal == null)
      {
        response.setStatus(HttpStatus.UNAUTHORIZED);
        return null;
      }
      Map<String, String> httpHeaders = getHttpHeaders(request);
      if (headerValidator != null)
      {
        headerValidator.accept(httpHeaders);
      }
      String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
      ResourceEndpoint resourceEndpoint = scimConfig.getResourceEndpoint();
      ScimResponse scimResponse = resourceEndpoint.handleRequest(request.getRequestURL().toString() + query,
                                                                 HttpMethod.valueOf(request.getMethod()),
                                                                 requestBody,
                                                                 httpHeaders,
                                                                 new Auth(principal));
      response.setContentType(HttpHeader.SCIM_CONTENT_TYPE);
      scimResponse.getHttpHeaders().forEach(response::setHeader);
      response.setStatus(scimResponse.getHttpStatus());
      return scimResponse.toPrettyString();
    }

    /**
     * extracts the http headers from the request and puts them into a map
     *
     * @param request the request object
     * @return a map with the http-headers
     */
    private Map<String, String> getHttpHeaders(HttpServletRequest request)
    {
      Map<String, String> httpHeaders = new HashMap<>();
      Enumeration<String> enumeration = request.getHeaderNames();
      while (enumeration != null && enumeration.hasMoreElements())
      {
        String headerName = enumeration.nextElement();
        httpHeaders.put(headerName, request.getHeader(headerName));
      }
      return httpHeaders;
    }

    /**
     * an authorization class to enable authorization on the scim endpoint
     */
    private static class Auth implements Authorization
    {

      /**
       * the spring authentication object
       */
      private Principal principal;

      public Auth(Principal principal)
      {
        this.principal = principal;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getClientId()
      {
        return principal.getName();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Set<String> getClientRoles()
      {
        return ((AbstractAuthenticationToken)principal).getAuthorities()
                                                       .stream()
                                                       .map(auth -> auth.getAuthority().replaceFirst("^ROLE_", ""))
                                                       .collect(Collectors.toSet());
      }
    }

  }
}
