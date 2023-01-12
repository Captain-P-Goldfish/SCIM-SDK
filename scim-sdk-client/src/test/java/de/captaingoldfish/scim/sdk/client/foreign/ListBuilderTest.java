package de.captaingoldfish.scim.sdk.client.foreign;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.ScimRequestBuilder;
import de.captaingoldfish.scim.sdk.client.builder.ListBuilder;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 14:11 <br>
 * <br>
 */
public class ListBuilderTest extends HttpServerMockup
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
   * simply assures that the list request can be called over the scimRequestBuilder from another package
   */
  @Test
  public void testListRequest()
  {
    ServerResponse<ListResponse<User>> response = scimRequestBuilder.list(User.class, EndpointPaths.USERS)
                                                                    .get()
                                                                    .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * like {@link BulkBuilderTest#testCreateBulkRequestWithOverriddenExpectedResponseHeadersSuccess()} but checks
   * that the method is usable from the {@link ListBuilder.GetRequestBuilder}
   */
  @Test
  public void testListGetRequestWithChangedExpectedHttpHeaders()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = getServerUrl();

    // 1. overrides the map {@link RequestBuilder#getRequiredResponseHeaders()} with some nonsense header
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
    expectedHeaders.put("Blubb", "nonsense");
    ListBuilder.GetRequestBuilder<User> listBuilder = new ListBuilder<>(url, EndpointPaths.USERS, User.class,
                                                                        scimHttpClient).get()
                                                                                       .setExpectedResponseHeaders(expectedHeaders);

    // 2. manipulates the response the server gives by setting exactly these headers into the response
    setGetResponseHeaders(() -> expectedHeaders);

    ServerResponse<ListResponse<User>> response = listBuilder.sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    // 3. verifies that the returned response is marked as successful
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * like {@link BulkBuilderTest#testCreateBulkRequestWithOverriddenExpectedResponseHeadersSuccess()} but checks
   * that the method is usable from the {@link ListBuilder.PostRequestBuilder}
   */
  @Test
  public void testListPostRequestWithChangedExpectedHttpHeaders()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = getServerUrl();

    // 1. overrides the map {@link RequestBuilder#getRequiredResponseHeaders()} with some nonsense header
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
    expectedHeaders.put("Blubb", "nonsense");
    ListBuilder.PostRequestBuilder<User> listBuilder = new ListBuilder<>(url, EndpointPaths.USERS, User.class,
                                                                         scimHttpClient).post()
                                                                                        .setExpectedResponseHeaders(expectedHeaders);

    // 2. manipulates the response the server gives by setting exactly these headers into the response
    setGetResponseHeaders(() -> expectedHeaders);

    ServerResponse<ListResponse<User>> response = listBuilder.sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    // 3. verifies that the returned response is marked as successful
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }
}
