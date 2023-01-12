package de.captaingoldfish.scim.sdk.client.foreign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.builder.PatchBuilder;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.User;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 12.01.2023
 */
@Slf4j
public class PatchBuilderTest extends HttpServerMockup
{

  /**
   * like {@link BulkBuilderTest#testCreateBulkRequestWithOverriddenExpectedResponseHeadersSuccess()} but checks
   * that the method is usable from the {@link PatchBuilder}
   */
  @Test
  public void testPatchRequestWithChangedExpectedHttpHeaders()
  {
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").build();
    userHandler.getInMemoryMap().put(user.getId().get(), user);

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = getServerUrl();

    // 1. overrides the map {@link RequestBuilder#getRequiredResponseHeaders()} with some nonsense header
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
    expectedHeaders.put("Blubb", "nonsense");
    PatchBuilder<User> createBuilder = new PatchBuilder<>(url, EndpointPaths.USERS, user.getId().get(), User.class,
                                                          scimHttpClient).setExpectedResponseHeaders(expectedHeaders);

    // 2. manipulates the response the server gives by setting exactly these headers into the response
    setGetResponseHeaders(() -> expectedHeaders);
    List<PatchRequestOperation> operations = new ArrayList<>();
    operations.add(PatchRequestOperation.builder()
                                        .op(PatchOp.REPLACE)
                                        .path(AttributeNames.RFC7643.USER_NAME)
                                        .value("mario")
                                        .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    log.warn(patchOpRequest.toPrettyString());
    ServerResponse<User> response = createBuilder.setPatchResource(patchOpRequest).sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    // 3. verifies that the returned response is marked as successful
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * sends a patch request but adds a prepared resource to the patch-request as simple string representation
   */
  @Test
  public void testSendPatchRequestWithResourceSetAsString()
  {
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").build();
    userHandler.getInMemoryMap().put(user.getId().get(), user);

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = getServerUrl();

    // 1. overrides the map {@link RequestBuilder#getRequiredResponseHeaders()} with some nonsense header
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
    expectedHeaders.put("Blubb", "nonsense");
    PatchBuilder<User> createBuilder = new PatchBuilder<>(url, EndpointPaths.USERS, user.getId().get(), User.class,
                                                          scimHttpClient).setExpectedResponseHeaders(expectedHeaders);

    // 2. manipulates the response the server gives by setting exactly these headers into the response
    setGetResponseHeaders(() -> expectedHeaders);
    List<PatchRequestOperation> operations = new ArrayList<>();
    operations.add(PatchRequestOperation.builder()
                                        .op(PatchOp.REPLACE)
                                        .path(AttributeNames.RFC7643.USER_NAME)
                                        .value("mario")
                                        .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    log.warn(patchOpRequest.toPrettyString());
    ServerResponse<User> response = createBuilder.setPatchResource(patchOpRequest.toString()).sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    // 3. verifies that the returned response is marked as successful
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }
}
