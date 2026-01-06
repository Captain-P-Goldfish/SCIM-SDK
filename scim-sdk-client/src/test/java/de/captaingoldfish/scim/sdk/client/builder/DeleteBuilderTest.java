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


/**
 * author Pascal Knueppel <br>
 * created at: 13.12.2019 - 09:05 <br>
 * <br>
 */
public class DeleteBuilderTest extends HttpServerMockup
{

  /**
   * verifies simply that the request is setup correctly for simple cases
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testSimpleDeleteRequestSuccess(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new DeleteBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class,
                                     scimHttpClient).sendRequest();
    }
    else
    {
      response = new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class, scimHttpClient).sendRequest();
    }
    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies simply that the request is setup correctly for simple cases even if the id needs to be url encoded
   */
  @Test
  public void testSimpleDeleteRequestSuccessWithUrlEncodedId()
  {
    final String id = "abc|def";
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    ServerResponse<User> response = new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                                        scimHttpClient).sendRequest();
    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that a request body is sent with the delete request if set
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testSimpleDeleteRequestSuccessWithRequestBody(boolean useFullUrl)
  {
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(id, user);

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    final String requestBody = "a simple plain text request body";
    setVerifyRequestAttributes((httpExchange, receivedRequestBody) -> {
      Assertions.assertEquals(requestBody, receivedRequestBody);
    });
    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new DeleteBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class,
                                     scimHttpClient).setResource(requestBody).sendRequest();
    }
    else
    {
      response = new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                     scimHttpClient).setResource(requestBody).sendRequest();
    }
    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());

  }


  /**
   * verifies simply that the request is setup correctly for simple cases
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testSimpleDeleteRequestFail(boolean useFullUrl)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new DeleteBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + UUID.randomUUID().toString(),
                                     User.class, scimHttpClient).sendRequest();
    }
    else
    {
      response = new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, UUID.randomUUID().toString(), User.class,
                                     scimHttpClient).sendRequest();
    }

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
    Assertions.assertNotNull(response.getResponseBody());
  }

  /**
   * sets the if-match-header in the request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testIfMatchHeader(boolean useFullUrl)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    final String version = "123456";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_MATCH_HEADER));
      wasCalled.set(true);
    });

    if (useFullUrl)
    {
      new DeleteBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + UUID.randomUUID().toString(), User.class,
                          scimHttpClient).setETagForIfMatch(version).sendRequest();
    }
    else
    {
      new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, UUID.randomUUID().toString(), User.class,
                          scimHttpClient).setETagForIfMatch(version).sendRequest();
    }

    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * sets the if-match-header in the request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testIfMatchHeader2(boolean useFullUrl)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    final String version = "123456";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_MATCH_HEADER));
      wasCalled.set(true);
    });

    if (useFullUrl)
    {
      new DeleteBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + UUID.randomUUID().toString(), User.class,
                          scimHttpClient).setETagForIfMatch(ETag.parseETag(version)).sendRequest();
    }
    else
    {
      new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, UUID.randomUUID().toString(), User.class,
                          scimHttpClient).setETagForIfMatch(ETag.parseETag(version)).sendRequest();
    }

    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * sets the if-match-header in the request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testIfNoneMatchHeader(boolean useFullUrl)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    final String version = "123456";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_NONE_MATCH_HEADER));
      wasCalled.set(true);
    });

    if (useFullUrl)
    {
      new DeleteBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + UUID.randomUUID().toString(), User.class,
                          scimHttpClient).setETagForIfNoneMatch(version).sendRequest();
    }
    else
    {
      new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, UUID.randomUUID().toString(), User.class,
                          scimHttpClient).setETagForIfNoneMatch(version).sendRequest();
    }

    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * sets the if-match-header in the request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testIfNoneMatchHeader2(boolean useFullUrl)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    final String version = "123456";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes((httpExchange, requestBody) -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_NONE_MATCH_HEADER));
      wasCalled.set(true);
    });

    if (useFullUrl)
    {
      new DeleteBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + UUID.randomUUID().toString(), User.class,
                          scimHttpClient).setETagForIfNoneMatch(ETag.parseETag(version)).sendRequest();
    }
    else
    {
      new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, UUID.randomUUID().toString(), User.class,
                          scimHttpClient).setETagForIfNoneMatch(ETag.parseETag(version)).sendRequest();
    }

    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * verifies that the response from the server can successfully be parsed if a not modified with an empty
   * response body is returned
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void parseNotModifiedResponse(boolean useFullUrl)
  {
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    final String id = UUID.randomUUID().toString();
    final String version = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).version(version).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new DeleteBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class,
                                     scimHttpClient).setETagForIfNoneMatch(version).sendRequest();
    }
    else
    {
      response = new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                     scimHttpClient).setETagForIfNoneMatch(version).sendRequest();
    }

    Assertions.assertFalse(response.isSuccess());
    Assertions.assertEquals(HttpStatus.NOT_MODIFIED, response.getHttpStatus());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getResponseBody());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that the response from the server can successfully be parsed if a precondition failed with an
   * empty response body is returned
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void parsePreConditionFailedResponse(boolean useFullUrl)
  {
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    final String id = UUID.randomUUID().toString();
    final String version = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).version(version).build();
    User user = User.builder().id(id).userName(UUID.randomUUID().toString()).meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    ServerResponse<User> response;
    if (useFullUrl)
    {
      response = new DeleteBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + id, User.class,
                                     scimHttpClient).setETagForIfMatch(version + "1").sendRequest();
    }
    else
    {
      response = new DeleteBuilder<>(getServerUrl(), EndpointPaths.USERS, id, User.class,
                                     scimHttpClient).setETagForIfMatch(version + "1").sendRequest();
    }

    Assertions.assertFalse(response.isSuccess());
    Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, response.getHttpStatus());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getResponseBody());
    Assertions.assertNotNull(response.getErrorResponse());
  }


}
