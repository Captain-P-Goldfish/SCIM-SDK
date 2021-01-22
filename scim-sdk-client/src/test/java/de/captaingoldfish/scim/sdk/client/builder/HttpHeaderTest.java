package de.captaingoldfish.scim.sdk.client.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.resources.User;


/**
 * <br>
 * <br>
 * created at: 08.05.2020
 *
 * @author Pascal Knueppel
 */
public class HttpHeaderTest extends HttpServerMockup
{

  /**
   * used for getting users
   */
  private String existingUserId;

  /**
   * the current user handler implementation that is used to manage users
   */
  private UserHandler userHandler;

  /**
   *
   */
  @BeforeEach
  public void init()
  {
    userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    existingUserId = userHandler.getInMemoryMap().keySet().iterator().next();
  }


  /**
   * asserts that the header maps in the clientConfig if both are set merged into a single map
   */
  @Test
  public void testMergeSingleHeadersAndMultiHeadersInClientConfig()
  {
    Map<String, String> headers = new HashMap<>();
    final String customValue = "Custom 123456";
    headers.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String[]> multiHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    multiHeaders.put(HttpHeaders.AUTHORIZATION, new String[]{basicAuth});

    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .httpHeaders(headers)
                                                        .httpMultiHeaders(multiHeaders)
                                                        .build();

    Map<String, String[]> configuredHeaders = scimClientConfig.getHttpHeaders();
    String[] headerValues = configuredHeaders.get(HttpHeaders.AUTHORIZATION);
    Assertions.assertEquals(customValue, headerValues[0]);
    Assertions.assertEquals(basicAuth, headerValues[1]);
  }

  /**
   * checks that the default http headers are correctly set in the request-builder
   */
  @Test
  public void testWithDefaultHttpHeadersForCreate()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(customValue, headerValues.get(0));

      wasCalled.set(true);
    });

    new CreateBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class,
                        scimHttpClient).setResource(User.builder().userName("goldfish").build()).sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredHeaderOverrideDefaultHeadersForCreate()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, basicAuth);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    new CreateBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class,
                        scimHttpClient).setResource(User.builder().userName("goldfish").build())
                                       .sendRequest(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testAddDefaultheadersWithMultiValuedMap()
  {
    Map<String, String[]> defaultHeaders = new HashMap<>();
    final String[] customValue = new String[]{"123456", "654321"};
    defaultHeaders.put("test", customValue);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpMultiHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get("test");
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(2, headerValues.size());
      Assertions.assertEquals("123456", headerValues.get(0));
      Assertions.assertEquals("654321", headerValues.get(1));
      wasCalled.set(true);
    });

    new CreateBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class,
                        scimHttpClient).setResource(User.builder().userName("goldfish").build()).sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredMultiHeaderOverrideDefaultHeadersForCreate()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String[]> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, new String[]{basicAuth});

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    new CreateBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class,
                        scimHttpClient).setResource(User.builder().userName("goldfish").build())
                                       .sendRequestWithMultiHeaders(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * checks that the default http headers are correctly set in the request-builder
   */
  @Test
  public void testWithDefaultHttpHeadersForGet()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(customValue, headerValues.get(0));

      wasCalled.set(true);
    });

    new GetBuilder<>(getServerUrl(), EndpointPaths.USERS, existingUserId, User.class, scimHttpClient).sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredHeaderOverrideDefaultHeadersForGet()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, basicAuth);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    new GetBuilder<>(getServerUrl(), EndpointPaths.USERS, existingUserId, User.class,
                     scimHttpClient).sendRequest(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredMultiHeaderOverrideDefaultHeadersForGet()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String[]> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, new String[]{basicAuth});

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    new GetBuilder<>(getServerUrl(), EndpointPaths.USERS, existingUserId, User.class,
                     scimHttpClient).sendRequestWithMultiHeaders(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * checks that the default http headers are correctly set in the request-builder
   */
  @Test
  public void testWithDefaultHttpHeadersForList()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(customValue, headerValues.get(0));

      wasCalled.set(true);
    });

    new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient).get().sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredHeaderOverrideDefaultHeadersForList()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, basicAuth);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient).get()
                                                                                      .sendRequest(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredMultiHeaderOverrideDefaultHeadersForList()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String[]> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, new String[]{basicAuth});

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class,
                      scimHttpClient).get().sendRequestWithMultiHeaders(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * checks that the default http headers are correctly set in the request-builder
   */
  @Test
  public void testWithDefaultHttpHeadersForDelete()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(customValue, headerValues.get(0));

      wasCalled.set(true);
    });

    new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, existingUserId, User.class, scimHttpClient).sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredHeaderOverrideDefaultHeadersForDelete()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, basicAuth);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, existingUserId, User.class,
                        scimHttpClient).sendRequest(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredMultiHeaderOverrideDefaultHeadersForDelete()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String[]> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, new String[]{basicAuth});

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, existingUserId, User.class,
                        scimHttpClient).sendRequestWithMultiHeaders(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * checks that the default http headers are correctly set in the request-builder
   */
  @Test
  public void testWithDefaultHttpHeadersForUpdate()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(customValue, headerValues.get(0));

      wasCalled.set(true);
    });

    User updateResource = userHandler.getResource(existingUserId, null, null, null);
    new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, existingUserId, User.class,
                        scimHttpClient).setResource(updateResource).sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredHeaderOverrideDefaultHeadersForUpdate()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, basicAuth);

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    User updateResource = userHandler.getResource(existingUserId, null, null, null);
    new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, existingUserId, User.class,
                        scimHttpClient).setResource(updateResource).sendRequest(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * asserts that the default headers are overridden by the preferred headers
   */
  @Test
  public void testPreferredMultiHeaderOverrideDefaultHeadersForUpdate()
  {
    Map<String, String> defaultHeaders = new HashMap<>();
    final String customValue = "Custom 123456";
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, customValue);

    Map<String, String[]> preferredHeaders = new HashMap<>();
    final String basicAuth = "Basic MTox";
    preferredHeaders.put(HttpHeaders.AUTHORIZATION, new String[]{basicAuth});

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(defaultHeaders).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> headerValues = httpExchange.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
      Assertions.assertNotNull(headerValues);
      Assertions.assertEquals(1, headerValues.size());
      Assertions.assertEquals(basicAuth, headerValues.get(0));
      wasCalled.set(true);
    });

    User updateResource = userHandler.getResource(existingUserId, null, null, null);
    new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, existingUserId, User.class,
                        scimHttpClient).setResource(updateResource).sendRequestWithMultiHeaders(preferredHeaders);
    Assertions.assertTrue(wasCalled.get());
  }
}
