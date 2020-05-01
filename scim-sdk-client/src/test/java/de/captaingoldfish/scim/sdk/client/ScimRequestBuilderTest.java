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
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.constants.ResponseType;
import de.captaingoldfish.scim.sdk.client.response.ScimServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
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
  @Test
  public void testBuildCreateRequest()
  {
    User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();
    ScimServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                          .setResource(user)
                                                          .sendRequest();
    Assertions.assertEquals(CreateResponse.class, response.getScimResponse().get().getClass());
    Assertions.assertEquals(ResponseType.CREATE, response.getResponseType());
    Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
    Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));

    Assertions.assertTrue(response.getResource().isPresent());
    User returnedUser = response.getResource().get();
    Assertions.assertEquals("goldfish", returnedUser.getUserName().get());
    Assertions.assertEquals(returnedUser.getMeta().get().getVersion().get().getEntityTag(),
                            response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
  }

  /**
   * verifies that an error response is correctly parsed
   */
  @Test
  public void testBuildCreateRequestWithErrorResponse()
  {
    User user = User.builder().userName("goldfish").build();
    user.setSchemas(Collections.singleton(SchemaUris.GROUP_URI)); // this will cause an error for wrong schema uri

    ScimServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                          .setResource(user)
                                                          .sendRequest();
    Assertions.assertEquals(ErrorResponse.class, response.getScimResponse().get().getClass());
    Assertions.assertEquals(ResponseType.ERROR, response.getResponseType());
    Assertions.assertTrue(response.getErrorResponse().isPresent());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    Assertions.assertEquals("main resource schema 'urn:ietf:params:scim:schemas:core:2.0:User' is not present in "
                            + "resource. Main schema is: urn:ietf:params:scim:schemas:core:2.0:User",
                            response.getErrorResponse().get().getDetail());
    Assertions.assertFalse(response.getResource().isPresent());
  }

  /**
   * verifies that a get-request can successfully be built
   */
  @Test
  public void testBuildGetRequest()
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    ScimServerResponse<User> response = scimRequestBuilder.get(User.class, EndpointPaths.USERS).setId(id).sendRequest();

    Assertions.assertTrue(response.getResource().isPresent());
    Assertions.assertEquals(user.getId().get(), response.getResource().get().getId().get());
  }

  /**
   * verifies that a response for a get-request is correctly returned if the user was not found
   */
  @Test
  public void testBuildGetRequestWithUserNotFound()
  {
    final String id = UUID.randomUUID().toString();

    ScimServerResponse<User> response = scimRequestBuilder.get(User.class, EndpointPaths.USERS).setId(id).sendRequest();

    Assertions.assertEquals(ResponseType.ERROR, response.getResponseType());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
    Assertions.assertFalse(response.getResource().isPresent());
    Assertions.assertTrue(response.getErrorResponse().isPresent());
  }

  /**
   * verifies that a delete-request can successfully be built
   */
  @Test
  public void testBuildDeleteRequest()
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    ScimServerResponse<User> response = scimRequestBuilder.delete(User.class, EndpointPaths.USERS)
                                                          .setId(id)
                                                          .sendRequest();

    Assertions.assertFalse(response.getResource().isPresent());
    Assertions.assertFalse(response.getErrorResponse().isPresent());
    Assertions.assertEquals(ResponseType.DELETE, response.getResponseType());
  }

  /**
   * verifies that a response for a get-request is correctly returned if the user was not found
   */
  @Test
  public void testBuildDeleteRequestWithUserNotFound()
  {
    final String id = UUID.randomUUID().toString();

    ScimServerResponse<User> response = scimRequestBuilder.delete(User.class, EndpointPaths.USERS)
                                                          .setId(id)
                                                          .sendRequest();

    Assertions.assertEquals(ResponseType.ERROR, response.getResponseType());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
    Assertions.assertFalse(response.getResource().isPresent());
    Assertions.assertTrue(response.getErrorResponse().isPresent());
  }

  /**
   * verifies that a delete-request can successfully be built
   */
  @Test
  public void testBuildUpdateRequest()
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    User updateUser = User.builder().nickName("hello world").build();
    ScimServerResponse<User> response = scimRequestBuilder.update(User.class, EndpointPaths.USERS)
                                                          .setId(id)
                                                          .setResource(updateUser)
                                                          .sendRequest();

    Assertions.assertTrue(response.getResource().isPresent());
    Assertions.assertFalse(response.getErrorResponse().isPresent());
    Assertions.assertEquals(ResponseType.UPDATE, response.getResponseType());
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
  }

  /**
   * verifies that a response for a get-request is correctly returned if the user was not found
   */
  @Test
  public void testBuildUpdateRequestWithUserNotFound()
  {
    final String id = UUID.randomUUID().toString();

    User updateUser = User.builder().nickName("hello world").build();
    ScimServerResponse<User> response = scimRequestBuilder.update(User.class, EndpointPaths.USERS)
                                                          .setId(id)
                                                          .setResource(updateUser)
                                                          .sendRequest();

    Assertions.assertEquals(ResponseType.ERROR, response.getResponseType());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
    Assertions.assertFalse(response.getResource().isPresent());
    Assertions.assertTrue(response.getErrorResponse().isPresent());
  }

  /**
   * verifies that it is possible to add additional http headers to the request
   */
  @Test
  public void testSendAdditionalHeaders()
  {
    final Map<String, String[]> httpHeaders = new HashMap<>();
    final String token = UUID.randomUUID().toString();
    httpHeaders.put(HttpHeader.AUHORIZATION, new String[]{"Bearer " + token, "hello world"});
    httpHeaders.put(HttpHeader.IF_MATCH_HEADER, new String[]{token});

    User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    super.setVerifyRequestAttributes((httpExchange, requestBody) -> {
      List<String> authHeaders = httpExchange.getRequestHeaders().get(HttpHeader.AUHORIZATION);
      Assertions.assertEquals(2, authHeaders.size(), authHeaders.toString());
      Assertions.assertEquals("Bearer " + token, authHeaders.get(0), authHeaders.toString());
      Assertions.assertEquals("hello world", authHeaders.get(1), authHeaders.toString());

      List<String> ifMatchHeaders = httpExchange.getRequestHeaders().get(HttpHeader.IF_MATCH_HEADER);
      Assertions.assertEquals(1, ifMatchHeaders.size(), ifMatchHeaders.toString());
      Assertions.assertEquals(token, ifMatchHeaders.get(0), ifMatchHeaders.toString());
      wasCalled.set(true);
    });

    scimRequestBuilder.create(User.class, EndpointPaths.USERS).setResource(user).sendRequest(httpHeaders);
    Assertions.assertTrue(wasCalled.get());
  }
}
