package de.captaingoldfish.scim.sdk.client;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.resources.ResourceType;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.TestSingletonHandler;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.client.setup.scim.resources.ScimTestSingleton;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 10:50 <br>
 * <br>
 */
@Slf4j
public class ScimRequestBuilderTest extends HttpServerMockup
{

  /**
   * the request builder that is under test
   */
  private ScimRequestBuilder scimRequestBuilder;

  /**
   * initializes the request builder
   */
  @BeforeEach
  public void init()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .connectTimeout(5)
                                                        .requestTimeout(5)
                                                        .socketTimeout(5)
                                                        .build();
    scimRequestBuilder = new ScimRequestBuilder(getServerUrl(), scimClientConfig);
  }

  /**
   * verifies that a create request can be successfully built and send to the scim service provider
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildCreateRequest(boolean useFullUrl)
  {
    User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();
    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.create(getServerUrl() + EndpointPaths.USERS, User.class)
                                   .setResource(user)
                                   .sendRequest();
    }
    else
    {
      response = scimRequestBuilder.create(User.class, EndpointPaths.USERS).setResource(user).sendRequest();
    }
    Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));

    User returnedUser = response.getResource();
    Assertions.assertEquals("goldfish", returnedUser.getUserName().get());
    Assertions.assertEquals(returnedUser.getMeta().get().getVersion().get().getEntityTag(),
                            response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
  }

  /**
   * verifies that an error response is correctly parsed
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildCreateRequestWithErrorResponse(boolean useFullUrl)
  {
    // the missing username will cause an error for missing required attribute
    User user = User.builder().nickName("goldfish").build();

    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.create(getServerUrl() + EndpointPaths.USERS, User.class)
                                   .setResource(user)
                                   .sendRequest();
    }
    else
    {
      response = scimRequestBuilder.create(User.class, EndpointPaths.USERS).setResource(user).sendRequest();
    }
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
    Assertions.assertEquals("Required 'READ_WRITE' attribute "
                            + "'urn:ietf:params:scim:schemas:core:2.0:User:userName' is missing",
                            response.getErrorResponse().getDetail().get());
  }

  /**
   * verifies that a get-request can successfully be built
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildGetRequest(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.get(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class).sendRequest();
    }
    else
    {
      response = scimRequestBuilder.get(User.class, EndpointPaths.USERS, id).sendRequest();
    }

    Assertions.assertNotNull(response.getResource());
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that a response for a get-request is correctly returned if the user was not found
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildGetRequestWithUserNotFound(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();

    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.get(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class).sendRequest();
    }
    else
    {
      response = scimRequestBuilder.get(User.class, EndpointPaths.USERS, id).sendRequest();
    }

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
  }

  /**
   * verifies that the list response is correctly build and that the resources are returned as expected
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildListRequestWithGet(boolean useFullUrl)
  {
    ServerResponse<ListResponse<User>> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.list(getServerUrl() + EndpointPaths.USERS, User.class).get().sendRequest();
    }
    else
    {
      response = scimRequestBuilder.list(User.class, EndpointPaths.USERS).get().sendRequest();
    }

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    Assertions.assertEquals(scimConfig.getServiceProvider().getFilterConfig().getMaxResults(),
                            response.getResource().getItemsPerPage());
    Assertions.assertEquals(scimConfig.getServiceProvider().getFilterConfig().getMaxResults(),
                            response.getResource().getListedResources().size());
  }

  /**
   * sends a list get-request with custom parameters
   */
  @Test
  public void testSendListWithGetAndCustomParams()
  {
    AtomicBoolean wasCalled = new AtomicBoolean(false);
    super.setVerifyRequestAttributes((httpExchange, s) -> {
      UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl("http://localhost/"
                                                                     + httpExchange.getRequestURI().toString())
                                                        .build();
      Assertions.assertEquals(uriComponents.getQueryParams().get("hello").get(0), "world");
      Assertions.assertEquals(uriComponents.getQueryParams().get("world").get(0), "hello");
      wasCalled.set(true);
    });

    ServerResponse<ListResponse<User>> response = scimRequestBuilder.list(User.class, EndpointPaths.USERS)
                                                                    .custom("hello", "world")
                                                                    .custom("world", "hello")
                                                                    .get()
                                                                    .sendRequest();

    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    Assertions.assertEquals(scimConfig.getServiceProvider().getFilterConfig().getMaxResults(),
                            response.getResource().getItemsPerPage());
    Assertions.assertEquals(scimConfig.getServiceProvider().getFilterConfig().getMaxResults(),
                            response.getResource().getListedResources().size());
  }

  /**
   * sends a list get-request with custom parameters
   */
  @Test
  public void testSendListWithFullUrlGetAndCustomParams()
  {
    AtomicBoolean wasCalled = new AtomicBoolean(false);
    super.setVerifyRequestAttributes((httpExchange, s) -> {
      UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl("http://localhost/"
                                                                     + httpExchange.getRequestURI().toString())
                                                        .build();
      Assertions.assertEquals(uriComponents.getQueryParams().get("hello").get(0), "world");
      Assertions.assertEquals(uriComponents.getQueryParams().get("world").get(0), "hello");
      wasCalled.set(true);
    });

    ServerResponse<ListResponse<User>> response = scimRequestBuilder.list(getServerUrl() + EndpointPaths.USERS
                                                                          + "?hello=world",
                                                                          User.class)
                                                                    .custom("world", "hello")
                                                                    .get()
                                                                    .sendRequest();

    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    Assertions.assertEquals(scimConfig.getServiceProvider().getFilterConfig().getMaxResults(),
                            response.getResource().getItemsPerPage());
    Assertions.assertEquals(scimConfig.getServiceProvider().getFilterConfig().getMaxResults(),
                            response.getResource().getListedResources().size());
  }

  /**
   * verifies that the list response is correctly build and that the resources are returned as expected
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildListRequestWithPost(boolean useFullUrl)
  {
    ServerResponse<ListResponse<User>> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.list(getServerUrl() + EndpointPaths.USERS, User.class).post().sendRequest();
    }
    else
    {
      response = scimRequestBuilder.list(User.class, EndpointPaths.USERS).post().sendRequest();
    }

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    Assertions.assertEquals(scimConfig.getServiceProvider().getFilterConfig().getMaxResults(),
                            response.getResource().getItemsPerPage());
    Assertions.assertEquals(scimConfig.getServiceProvider().getFilterConfig().getMaxResults(),
                            response.getResource().getListedResources().size());
  }

  /**
   * verifies that a delete-request can successfully be built
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildDeleteRequest(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.delete(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class).sendRequest();
    }
    else
    {
      response = scimRequestBuilder.delete(User.class, EndpointPaths.USERS, id).sendRequest();
    }

    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that a delete-request can successfully be built for a singleton entry
   */
  @Test
  public void testBuildDeleteRequestForSingleton()
  {
    TestSingletonHandler.scimTestSingleton = ScimTestSingleton.builder().build();

    ServerResponse<User> response = scimRequestBuilder.delete(User.class, TestSingletonHandler.ENDPOINT).sendRequest();

    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that a response for a get-request is correctly returned if the user was not found
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildDeleteRequestWithUserNotFound(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();

    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.delete(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class).sendRequest();
    }
    else
    {
      response = scimRequestBuilder.delete(User.class, EndpointPaths.USERS, id).sendRequest();
    }

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
  }

  /**
   * verifies that a delete-request can successfully be built
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildUpdateRequest(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    User updateUser = User.builder().userName(user.getUserName().get()).nickName("hello world").build();
    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.update(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class)
                                   .setResource(updateUser)
                                   .sendRequest();
    }
    else
    {
      response = scimRequestBuilder.update(User.class, EndpointPaths.USERS, id).setResource(updateUser).sendRequest();
    }

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that a delete-request can successfully be built for singletons
   */
  @Test

  public void testBuildUpdateRequest()
  {
    TestSingletonHandler.scimTestSingleton = ScimTestSingleton.builder()
                                                              .singletonAttribute("blubb")
                                                              .meta(Meta.builder()
                                                                        .created(Instant.now())
                                                                        .lastModified(Instant.now())
                                                                        .build())
                                                              .build();

    ScimTestSingleton updatedResource = JsonHelper.copyResourceToObject(TestSingletonHandler.scimTestSingleton,
                                                                        ScimTestSingleton.class);
    final String updatedValue = "hello world";
    updatedResource.setSingletonAttribute(updatedValue);
    ServerResponse<ScimTestSingleton> response = scimRequestBuilder.update(ScimTestSingleton.class,
                                                                           TestSingletonHandler.ENDPOINT)
                                                                   .setResource(updatedResource)
                                                                   .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertNotNull(response.getResource());
    ScimTestSingleton patchedResource = response.getResource();
    Assertions.assertEquals(updatedValue, patchedResource.getSingletonAttribute().orElse(null));
  }

  /**
   * verifies that a response for a get-request is correctly returned if the user was not found
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildUpdateRequestWithUserNotFound(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();

    User updateUser = User.builder().userName("goldfish").nickName("hello world").build();
    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.update(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class)
                                   .setResource(updateUser)
                                   .sendRequest();
    }
    else
    {
      response = scimRequestBuilder.update(User.class, EndpointPaths.USERS, id).setResource(updateUser).sendRequest();
    }

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
  }

  /**
   * verifies that it is possible to add additional http headers to the request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testSendAdditionalHeaders(boolean useFullUrl)
  {
    final Map<String, String[]> httpHeaders = new HashMap<>();
    final String token = UUID.randomUUID().toString();
    httpHeaders.put(HttpHeader.AUTHORIZATION, new String[]{"Bearer " + token, "hello world"});
    httpHeaders.put(HttpHeader.IF_MATCH_HEADER, new String[]{token});

    User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    super.setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> authHeaders = httpExchange.getRequestHeaders().get(HttpHeader.AUTHORIZATION);
      Assertions.assertEquals(2, authHeaders.size(), authHeaders.toString());
      Assertions.assertEquals("Bearer " + token, authHeaders.get(0), authHeaders.toString());
      Assertions.assertEquals("hello world", authHeaders.get(1), authHeaders.toString());

      List<String> ifMatchHeaders = httpExchange.getRequestHeaders().get(HttpHeader.IF_MATCH_HEADER);
      Assertions.assertEquals(1, ifMatchHeaders.size(), ifMatchHeaders.toString());
      Assertions.assertEquals(token, ifMatchHeaders.get(0), ifMatchHeaders.toString());
      wasCalled.set(true);
    });

    if (useFullUrl)
    {
      scimRequestBuilder.create(getServerUrl() + EndpointPaths.USERS, User.class)
                        .setResource(user)
                        .sendRequestWithMultiHeaders(httpHeaders);
    }
    else
    {
      scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                        .setResource(user)
                        .sendRequestWithMultiHeaders(httpHeaders);
    }
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * verifies that the service provider configuration can be loaded without having to set an id value
   */
  @Test
  public void testGetSingletonWithGet()
  {
    ServerResponse<ServiceProvider> response = scimRequestBuilder.get(ServiceProvider.class,
                                                                      EndpointPaths.SERVICE_PROVIDER_CONFIG)
                                                                 .sendRequest();
    Assertions.assertEquals(HttpStatus.OK,
                            response.getHttpStatus(),
                            Optional.ofNullable(response.getErrorResponse())
                                    .map(ErrorResponse::toPrettyString)
                                    .orElse(null));
  }

  /**
   * verifies that a service provider configuration can successfully be read without any problems
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testGetServiceProviderAndReadDetails(boolean useFullUrl)
  {
    ServerResponse<ServiceProvider> response;
    if (useFullUrl)
    {
      response = scimRequestBuilder.get(getServerUrl() + EndpointPaths.SERVICE_PROVIDER_CONFIG, ServiceProvider.class)
                                   .sendRequest();

    }
    else
    {
      response = scimRequestBuilder.get(ServiceProvider.class, EndpointPaths.SERVICE_PROVIDER_CONFIG, null)
                                   .sendRequest();
    }
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    ServiceProvider serviceProvider = response.getResource();
    Assertions.assertNotNull(serviceProvider);
    Assertions.assertDoesNotThrow(serviceProvider::getBulkConfig);
    Assertions.assertDoesNotThrow(serviceProvider::getFilterConfig);
    Assertions.assertDoesNotThrow(serviceProvider::getPatchConfig);
    Assertions.assertDoesNotThrow(serviceProvider::getSortConfig);
    Assertions.assertDoesNotThrow(serviceProvider::getETagConfig);
    Assertions.assertDoesNotThrow(serviceProvider::getChangePasswordConfig);
    Assertions.assertDoesNotThrow(serviceProvider::getAuthenticationSchemes);
  }

  /**
   * verifies that closing the {@link ScimRequestBuilder} will close the underlying apache http client and that
   * a new client is generated if get {@link ScimHttpClient#getHttpClient()} method is called
   */
  @Test
  public void testCloseScimRequestBuilder()
  {
    Assertions.assertDoesNotThrow(() -> scimRequestBuilder.close());

    ScimHttpClient scimHttpClient = scimRequestBuilder.getScimHttpClient();
    ScimHttpClient scimHttpClient2 = scimRequestBuilder.getScimHttpClient();
    Assertions.assertEquals(scimHttpClient, scimHttpClient2);

    CloseableHttpClient client = scimHttpClient.getHttpClient();
    Assertions.assertNotNull(client);
    Assertions.assertEquals(client, scimHttpClient2.getHttpClient());

    Assertions.assertDoesNotThrow(() -> scimRequestBuilder.close());

    scimHttpClient2 = scimRequestBuilder.getScimHttpClient();
    Assertions.assertEquals(scimHttpClient, scimHttpClient2);
    // even when closed accessing the get httpclient method must return a new instance
    Assertions.assertNotNull(scimHttpClient2.getHttpClient());
    Assertions.assertNotEquals(client, scimHttpClient2.getHttpClient());
  }

  /**
   * verifies that the builtin resource type can be used to successfully load the data from the resource types
   * endpoint
   */
  @Test
  public void testGetResourceTypes()
  {
    ServerResponse<ListResponse<ResourceType>> response = scimRequestBuilder.list(getServerUrl()
                                                                                  + EndpointPaths.RESOURCE_TYPES,
                                                                                  ResourceType.class)
                                                                            .get()
                                                                            .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    ListResponse<ResourceType> resourceTypeListResponse = response.getResource();
    Assertions.assertEquals(6, resourceTypeListResponse.getTotalResults());
    List<ResourceType> resourceTypeList = resourceTypeListResponse.getListedResources();
    MatcherAssert.assertThat(resourceTypeList.stream().map(ResourceType::getName).collect(Collectors.toList()),
                             Matchers.containsInAnyOrder("ResourceType",
                                                         "ServiceProviderConfig",
                                                         "TestSingleton",
                                                         "Schema",
                                                         "Group",
                                                         "User"));
  }
}
