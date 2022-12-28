package de.captaingoldfish.scim.sdk.client.builder;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.User;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class CreateBuilderTest extends HttpServerMockup
{

  /**
   * verifies that a create request is correctly executed and parsed
   */
  @Test
  public void testCreateBuilderTest()
  {
    final String username = UUID.randomUUID().toString();
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    User user = User.builder().userName(username).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ServerResponse<User> response = new CreateBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class,
                                                        scimHttpClient).setResource(user).sendRequest();
    Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
    Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());

    User createdUser = response.getResource();
    Assertions.assertEquals(username, createdUser.getUserName().get());
    Assertions.assertEquals(createdUser.getMeta().get().getVersion().get().getEntityTag(),
                            response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
  }

  /**
   * verifies that a create request is correctly executed and parsed if a fully qualified URL is used
   */
  @Test
  public void testWithFullyQualifiedUrl()
  {
    final String username = UUID.randomUUID().toString();
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ServerResponse<User> response = new CreateBuilder<>(getServerUrl() + EndpointPaths.USERS, User.class,
                                                        scimHttpClient).setResource(User.builder().userName(username).build()).sendRequest();
    Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
    Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());

    User user = response.getResource();
    Assertions.assertEquals(username, user.getUserName().get());
    Assertions.assertEquals(user.getMeta().get().getVersion().get().getEntityTag(),
                            response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
  }
}
