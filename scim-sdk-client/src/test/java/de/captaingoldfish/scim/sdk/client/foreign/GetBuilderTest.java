package de.captaingoldfish.scim.sdk.client.foreign;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.builder.GetBuilder;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.User;


/**
 * @author Pascal Knueppel
 * @since 12.01.2023
 */
public class GetBuilderTest extends HttpServerMockup
{

  /**
   * like {@link BulkBuilderTest#testCreateBulkRequestWithOverriddenExpectedResponseHeadersSuccess()} but checks
   * that the method is usable from the {@link de.captaingoldfish.scim.sdk.client.builder.GetBuilder}
   */
  @Test
  public void testGetRequestWithChangedExpectedHttpHeaders()
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
    GetBuilder<User> createBuilder = new GetBuilder<>(url, EndpointPaths.USERS, user.getId().get(), User.class,
                                                      scimHttpClient).setExpectedResponseHeaders(expectedHeaders);

    // 2. manipulates the response the server gives by setting exactly these headers into the response
    setGetResponseHeaders(() -> expectedHeaders);

    ServerResponse<User> response = createBuilder.sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    // 3. verifies that the returned response is marked as successful
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }
}
