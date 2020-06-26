package de.captaingoldfish.scim.sdk.client;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
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
    User user = User.builder().userName("goldfish").build();
    user.setSchemas(Collections.singleton(SchemaUris.GROUP_URI)); // this will cause an error for wrong schema uri

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
    Assertions.assertEquals("main resource schema 'urn:ietf:params:scim:schemas:core:2.0:User' is not present in "
                            + "resource. Main schema is: urn:ietf:params:scim:schemas:core:2.0:User",
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

    User updateUser = User.builder().nickName("hello world").build();
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
   * verifies that a response for a get-request is correctly returned if the user was not found
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildUpdateRequestWithUserNotFound(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();

    User updateUser = User.builder().nickName("hello world").build();
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
}
