package de.captaingoldfish.scim.sdk.client.builder;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.constants.ResponseType;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ScimServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 12:09 <br>
 * <br>
 */
public class UpdateBuilderTest extends HttpServerMockup
{

  /**
   * verifies that a resource can successfully be updated
   */
  @Test
  public void testUpdateResourceSuccess()
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    User updateUser = User.builder().name(Name.builder().givenName("goldfish").build()).build();
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ScimServerResponse<User> response = new UpdateBuilder<>(getServerUrl(), scimClientConfig, User.class,
                                                            scimHttpClient).setEndpoint(EndpointPaths.USERS)
                                                                           .setId(id)
                                                                           .setResource(updateUser)
                                                                           .sendRequest();

    Assertions.assertEquals(ResponseType.UPDATE, response.getResponseType());
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.getResource().isPresent());
    Assertions.assertTrue(response.getResource().get().getName().isPresent());
    Assertions.assertTrue(response.getResource().get().getName().get().getGivenName().isPresent());
    Assertions.assertEquals("goldfish", response.getResource().get().getName().get().getGivenName().get());
  }

  /**
   * verifies that an error response is correctly parsed
   */
  @Test
  public void testUpdateResourceFail()
  {
    final String id = UUID.randomUUID().toString();
    User updateUser = User.builder().name(Name.builder().givenName("goldfish").build()).build();
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ScimServerResponse<User> response = new UpdateBuilder<>(getServerUrl(), scimClientConfig, User.class,
                                                            scimHttpClient).setEndpoint(EndpointPaths.USERS)
                                                                           .setId(id)
                                                                           .setResource(updateUser)
                                                                           .sendRequest();

    Assertions.assertEquals(ResponseType.ERROR, response.getResponseType());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
    Assertions.assertFalse(response.getResource().isPresent());
    Assertions.assertTrue(response.getErrorResponse().isPresent());
  }

  /**
   * sets the if-match-header in the request
   */
  @Test
  public void testIfMatchHeader()
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    final String version = "123456";
    meta.setVersion(ETag.parseETag(version));

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_MATCH_HEADER));
      wasCalled.set(true);
    });

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    User updateUser = User.builder().name(Name.builder().givenName("goldfish").build()).build();
    ScimServerResponse<User> response = new UpdateBuilder<>(getServerUrl(), scimClientConfig, User.class,
                                                            scimHttpClient).setEndpoint(EndpointPaths.USERS)
                                                                           .setId(id)
                                                                           .setETagForIfMatch(version)
                                                                           .setResource(updateUser)
                                                                           .sendRequest();
    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(ResponseType.UPDATE, response.getResponseType());
    Assertions.assertTrue(response.getResource().isPresent());
  }

  /**
   * sets the if-match-header in the request
   */
  @Test
  public void testIfMatchHeader2()
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    final String version = "123456";
    meta.setVersion(ETag.parseETag(version));

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_MATCH_HEADER));
      wasCalled.set(true);
    });

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    User updateUser = User.builder().name(Name.builder().givenName("goldfish").build()).build();
    ScimServerResponse<User> response = new UpdateBuilder<>(getServerUrl(), scimClientConfig, User.class,
                                                            scimHttpClient).setEndpoint(EndpointPaths.USERS)
                                                                           .setId(id)
                                                                           .setETagForIfMatch(ETag.parseETag(version))
                                                                           .setResource(updateUser)
                                                                           .sendRequest();
    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(ResponseType.UPDATE, response.getResponseType());
    Assertions.assertTrue(response.getResource().isPresent());
  }

  /**
   * sets the if-match-header in the request
   */
  @Test
  public void testIfNoneMatchHeader()
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    final String version = "123456";
    meta.setVersion(ETag.parseETag(version));

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_NONE_MATCH_HEADER));
      wasCalled.set(true);
    });

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    User updateUser = User.builder().name(Name.builder().givenName("goldfish").build()).build();
    ScimServerResponse<User> response = new UpdateBuilder<>(getServerUrl(), scimClientConfig, User.class,
                                                            scimHttpClient).setEndpoint(EndpointPaths.USERS)
                                                                           .setId(id)
                                                                           .setETagForIfNoneMatch(version)
                                                                           .setResource(updateUser)
                                                                           .sendRequest();
    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(ResponseType.ERROR, response.getResponseType());
    Assertions.assertEquals(HttpStatus.NOT_MODIFIED, response.getHttpStatus());
  }

  /**
   * sets the if-match-header in the request
   */
  @Test
  public void testIfNoneMatchHeader2()
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    final String version = "123456";
    meta.setVersion(ETag.parseETag(version));

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_NONE_MATCH_HEADER));
      wasCalled.set(true);
    });

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    User updateUser = User.builder().name(Name.builder().givenName("goldfish").build()).build();
    ScimServerResponse<User> response = new UpdateBuilder<>(getServerUrl(), scimClientConfig, User.class,
                                                            scimHttpClient).setEndpoint(EndpointPaths.USERS)
                                                                           .setId(id)
                                                                           .setETagForIfNoneMatch(ETag.parseETag(version))
                                                                           .setResource(updateUser)
                                                                           .sendRequest();
    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(ResponseType.ERROR, response.getResponseType());
    Assertions.assertEquals(HttpStatus.NOT_MODIFIED, response.getHttpStatus());
  }
}
