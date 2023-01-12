package de.captaingoldfish.scim.sdk.client.foreign;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.builder.BulkBuilder;
import de.captaingoldfish.scim.sdk.client.builder.RequestBuilder;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;


/**
 * @author Pascal Knueppel
 * @since 12.01.2023
 */
public class BulkBuilderTest extends HttpServerMockup
{

  /**
   * some tests might change the configuration of the scim configuration so this method will set back this
   * configuration
   */
  @AfterEach
  public void resetConfiguration()
  {
    scimConfig.getServiceProvider().getBulkConfig().setMaxOperations(10);
    scimConfig.getServiceProvider().getBulkConfig().setMaxPayloadSize((long)(Math.pow(1024, 2) * 2));
  }


  /**
   * <ul>
   * <li>overrides the map {@link RequestBuilder#getRequiredResponseHeaders()} with some nonsense header</li>
   * <li>manipulates the response the server gives by setting exactly these headers into the response</li>
   * <li>verifies that the returned response is marked as successful</li>
   * </ul>
   */
  @Test
  public void testCreateBulkRequestWithOverriddenExpectedResponseHeadersSuccess()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = getServerUrl();

    // 1. overrides the map {@link RequestBuilder#getRequiredResponseHeaders()} with some nonsense header
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
    expectedHeaders.put("Blubb", "nonsense");
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, false,
                                              null).setExpectedResponseHeaders(expectedHeaders);

    // 2. manipulates the response the server gives by setting exactly these headers into the response
    setGetResponseHeaders(() -> expectedHeaders);

    ServerResponse<BulkResponse> response = bulkBuilder.bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .bulkId(UUID.randomUUID().toString())
                                                       .data(User.builder()
                                                                 .userName(UUID.randomUUID().toString())
                                                                 .build())
                                                       .sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    // 3. verifies that the returned response is marked as successful
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * <ul>
   * <li>overrides the map {@link RequestBuilder#getRequiredResponseHeaders()} with some nonsense header</li>
   * <li>manipulates the response the server gives by setting exactly these headers into the response but at
   * least one header will be missing in the response</li>
   * <li>verifies that the returned response is marked as failure</li>
   * </ul>
   */
  @Test
  public void testCreateBulkRequestWithOverriddenExpectedResponseHeadersMissingHeaders()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = getServerUrl();

    // 1. overrides the map {@link RequestBuilder#getRequiredResponseHeaders()} with some nonsense header
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
    expectedHeaders.put("Blubb", "nonsense");
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, false,
                                              null).setExpectedResponseHeaders(expectedHeaders);

    // 2. manipulates the response the server gives by setting exactly these headers into the response but at
    // least one header will be missing in the response
    Map<String, String> headersToReturn = new HashMap<>(expectedHeaders);
    headersToReturn.remove("Blubb");
    setGetResponseHeaders(() -> headersToReturn);

    ServerResponse<BulkResponse> response = bulkBuilder.bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .bulkId(UUID.randomUUID().toString())
                                                       .data(User.builder()
                                                                 .userName(UUID.randomUUID().toString())
                                                                 .build())
                                                       .sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    // 3. verifies that the returned response is marked as failure
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * <ul>
   * <li>overrides the map {@link ScimClientConfig#getExpectedHttpResponseHeaders()} with some nonsense
   * header</li>
   * <li>manipulates the response the server gives by setting exactly these headers into the response</li>
   * <li>verifies that the returned response is marked as failure</li>
   * </ul>
   */
  @Test
  public void testCreateBulkRequestWithOverriddenExpectedResponseHeadersInClientConfig()
  {
    // 1. overrides the map {@link ScimClientConfig#getRequiredResponseHeaders()} with some nonsense header
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
    expectedHeaders.put("Blubb", "nonsense");
    ScimClientConfig scimClientConfig = ScimClientConfig.builder().expectedHttpResponseHeaders(expectedHeaders).build();

    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = getServerUrl();

    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, false, null);

    // 2. manipulates the response the server gives by setting exactly these headers into the response
    setGetResponseHeaders(() -> expectedHeaders);

    ServerResponse<BulkResponse> response = bulkBuilder.bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .bulkId(UUID.randomUUID().toString())
                                                       .data(User.builder()
                                                                 .userName(UUID.randomUUID().toString())
                                                                 .build())
                                                       .sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    // 3. verifies that the returned response is marked as a success
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * <ul>
   * <li>overrides the map {@link ScimClientConfig#getExpectedHttpResponseHeaders()} with some nonsense
   * header</li>
   * <li>manipulates the response the server gives by setting exactly these headers into the response but at
   * least one header will be missing in the response</li>
   * <li>verifies that the returned response is marked as failure</li>
   * </ul>
   */
  @Test
  public void testMissingDefaultHeadersInResponse()
  {
    // 1. overrides the map {@link ScimClientConfig#getRequiredResponseHeaders()} with some nonsense header
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
    expectedHeaders.put("Blubb", "nonsense");
    ScimClientConfig scimClientConfig = ScimClientConfig.builder().expectedHttpResponseHeaders(expectedHeaders).build();

    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = getServerUrl();

    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, false, null);

    // 2. manipulates the response the server gives by setting exactly these headers into the response but at
    // least one header will be missing in the response
    Map<String, String> headersToReturn = new HashMap<>(expectedHeaders);
    headersToReturn.remove("Blubb");
    setGetResponseHeaders(() -> headersToReturn);

    ServerResponse<BulkResponse> response = bulkBuilder.bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .bulkId(UUID.randomUUID().toString())
                                                       .data(User.builder()
                                                                 .userName(UUID.randomUUID().toString())
                                                                 .build())
                                                       .sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    // 3. verifies that the returned response is marked as failure
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }
}
