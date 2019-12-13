package de.captaingoldfish.scim.sdk.client.builder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.client.constants.ResponseType;
import de.captaingoldfish.scim.sdk.client.response.ScimServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;


/**
 * author Pascal Knueppel <br>
 * created at: 13.12.2019 - 09:05 <br>
 * <br>
 */
public class GetBuilderTest extends HttpServerMockup
{

  /**
   * verifies simply that the request is setup correctly for simple cases
   */
  @Test
  public void testGetRequestWithMissingId()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    try
    {
      new GetBuilder<>(getServerUrl(), scimClientConfig, User.class).setEndpoint(EndpointPaths.USERS).sendRequest();
      Assertions.fail("this point must not be reached");
    }
    catch (IllegalStateException ex)
    {
      Assertions.assertEquals("id must not be blank for get-requests", ex.getMessage());
    }
  }

  /**
   * verifies simply that the request is setup correctly for simple cases
   */
  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  public void testSetEmptyId(String id)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    try
    {
      new GetBuilder<>(getServerUrl(), scimClientConfig, User.class).setEndpoint(EndpointPaths.USERS).setId(id);
      Assertions.fail("this point must not be reached");
    }
    catch (IllegalStateException ex)
    {
      Assertions.assertEquals("id must not be blank for get-requests", ex.getMessage());
    }
  }

  /**
   * verifies simply that the request is setup correctly for simple cases
   */
  @Test
  public void testSimpleGetRequest()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimServerResponse<User> response = new GetBuilder<>(getServerUrl(), scimClientConfig,
                                                         User.class).setEndpoint(EndpointPaths.USERS)
                                                                    .setId(UUID.randomUUID().toString())
                                                                    .sendRequest();
    Assertions.assertEquals(ResponseType.ERROR, response.getResponseType());
    Assertions.assertEquals(ErrorResponse.class, response.getScimResponse().getClass());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
  }

  /**
   * sets the if-match-header in the request
   */
  @Test
  public void testIfMatchHeader()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();

    final String version = "123456";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes(httpExchange -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_MATCH_HEADER));
      wasCalled.set(true);
    });

    new GetBuilder<>(getServerUrl(), scimClientConfig, User.class).setEndpoint(EndpointPaths.USERS)
                                                                  .setId(UUID.randomUUID().toString())
                                                                  .setETagForIfMatch(version)
                                                                  .sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * sets the if-match-header in the request
   */
  @Test
  public void testIfMatchHeader2()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();

    final String version = "123456";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes(httpExchange -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_MATCH_HEADER));
      wasCalled.set(true);
    });

    new GetBuilder<>(getServerUrl(), scimClientConfig, User.class).setEndpoint(EndpointPaths.USERS)
                                                                  .setId(UUID.randomUUID().toString())
                                                                  .setETagForIfMatch(ETag.parseETag(version))
                                                                  .sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * sets the if-match-header in the request
   */
  @Test
  public void testIfNoneMatchHeader()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();

    final String version = "123456";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes(httpExchange -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_NONE_MATCH_HEADER));
      wasCalled.set(true);
    });

    new GetBuilder<>(getServerUrl(), scimClientConfig, User.class).setEndpoint(EndpointPaths.USERS)
                                                                  .setId(UUID.randomUUID().toString())
                                                                  .setETagForIfNoneMatch(version)
                                                                  .sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * sets the if-match-header in the request
   */
  @Test
  public void testIfNoneMatchHeader2()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();

    final String version = "123456";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    setVerifyRequestAttributes(httpExchange -> {
      Assertions.assertEquals(ETag.parseETag(version).toString(),
                              httpExchange.getRequestHeaders().getFirst(HttpHeader.IF_NONE_MATCH_HEADER));
      wasCalled.set(true);
    });

    new GetBuilder<>(getServerUrl(), scimClientConfig, User.class).setEndpoint(EndpointPaths.USERS)
                                                                  .setId(UUID.randomUUID().toString())
                                                                  .setETagForIfNoneMatch(ETag.parseETag(version))
                                                                  .sendRequest();
    Assertions.assertTrue(wasCalled.get());
  }


}
