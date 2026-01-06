package de.captaingoldfish.scim.sdk.client.builder;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
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
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    User updateUser = User.builder()
                          .userName(user.getUserName().get())
                          .name(Name.builder().givenName("goldfish").build())
                          .build();
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ServerResponse<User> response = new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                                        scimHttpClient).setResource(updateUser).sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that a resource can successfully be updated even if the id needs to be url encoded
   */
  @Test
  public void testUpdateResourceSuccessWithUrlEncodedId()
  {
    final String id = "abc|def";
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    User updateUser = User.builder()
                          .userName(user.getUserName().get())
                          .name(Name.builder().givenName("goldfish").build())
                          .build();
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ServerResponse<User> response = new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                                        scimHttpClient).setResource(updateUser).sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that an error response is correctly parsed
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testUpdateResourceFail(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    User updateUser = User.builder()
                          .userName(UUID.randomUUID().toString())
                          .name(Name.builder().givenName("goldfish").build())
                          .build();
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new UpdateBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class,
                                     scimHttpClient).setResource(updateUser).sendRequest();

    }
    else
    {
      response = new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                     scimHttpClient).setResource(updateUser).sendRequest();
    }

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
  }

  /**
   * sets the if-match-header in the request and expects a successful update
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testIfMatchHeader(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
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
    User updateUser = User.builder()
                          .userName(user.getUserName().get())
                          .name(Name.builder().givenName("goldfish").build())
                          .build();

    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new UpdateBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class,
                                     scimHttpClient).setETagForIfMatch(version).setResource(updateUser).sendRequest();
    }
    else
    {
      response = new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                     scimHttpClient).setETagForIfMatch(version).setResource(updateUser).sendRequest();
    }

    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * sets the if-match-header in the request and expects a successful update.
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testIfMatchHeader2(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
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
    User updateUser = User.builder()
                          .userName(user.getUserName().get())
                          .name(Name.builder().givenName("goldfish").build())
                          .build();
    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new UpdateBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class,
                                     scimHttpClient).setETagForIfMatch(ETag.parseETag(version))
                                                    .setResource(updateUser)
                                                    .sendRequest();
    }
    else
    {
      response = new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                     scimHttpClient).setETagForIfMatch(ETag.parseETag(version))
                                                    .setResource(updateUser)
                                                    .sendRequest();
    }

    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * sets the if-match-header in the request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testIfNoneMatchHeader(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
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
    User updateUser = User.builder()
                          .userName(user.getUserName().get())
                          .name(Name.builder().givenName("goldfish").build())
                          .build();
    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new UpdateBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class,
                                     scimHttpClient).setETagForIfNoneMatch(version)
                                                    .setResource(updateUser)
                                                    .sendRequest();
    }
    else
    {
      response = new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                     scimHttpClient).setETagForIfNoneMatch(version)
                                                    .setResource(updateUser)
                                                    .sendRequest();
    }

    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(HttpStatus.NOT_MODIFIED, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * sets the if-match-header in the request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testIfNoneMatchHeader2(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
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
    User updateUser = User.builder()
                          .userName(user.getUserName().get())
                          .name(Name.builder().givenName("goldfish").build())
                          .build();
    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new UpdateBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class,
                                     scimHttpClient).setETagForIfNoneMatch(ETag.parseETag(version))
                                                    .setResource(updateUser)
                                                    .sendRequest();
    }
    else
    {
      response = new UpdateBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                     scimHttpClient).setETagForIfNoneMatch(ETag.parseETag(version))
                                                    .setResource(updateUser)
                                                    .sendRequest();
    }

    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(HttpStatus.NOT_MODIFIED, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }
}
