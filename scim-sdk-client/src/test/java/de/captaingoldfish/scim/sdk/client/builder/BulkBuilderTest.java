package de.captaingoldfish.scim.sdk.client.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;


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
   * verifies that a bulk request is correctly resolved after return from the server
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testCreateBulkRequest(boolean useFullUrl)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl);
    ServerResponse<BulkResponse> response = bulkBuilder.bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .bulkId(UUID.randomUUID().toString())
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * causes a precondition failed response from the server by causing errors in the bulk request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testCreateWithPreconditionFailed(boolean useFullUrl)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl);
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0)
                                                       .bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * causes a payload too large error on the server side
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testPayloadTooLarge(boolean useFullUrl)
  {
    scimConfig.getServiceProvider().getBulkConfig().setMaxPayloadSize(1L);
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl);
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0)
                                                       .bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
  }

  /**
   * causes a too many operations error on the server side
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testTooManyOperations(boolean useFullUrl)
  {
    scimConfig.getServiceProvider().getBulkConfig().setMaxOperations(0);
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl);
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0)
                                                       .bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
  }

  /**
   * assumes that a wrong server was called and a totally unknown response is returned
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testCommunicationWithWrongServer(boolean useFullUrl)
  {
    setGetResponseBody(() -> "an internal server error occurred");
    setGetResponseStatus(() -> HttpStatus.INTERNAL_SERVER_ERROR);
    setGetResponseHeaders(() -> {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
      return headers;
    });

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl);
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0)
                                                       .bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    Assertions.assertEquals(getGetResponseBody().get(), response.getResponseBody());
  }
}
