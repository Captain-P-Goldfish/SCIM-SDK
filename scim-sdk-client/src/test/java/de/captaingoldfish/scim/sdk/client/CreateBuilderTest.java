package de.captaingoldfish.scim.sdk.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.constants.ResponseType;
import de.captaingoldfish.scim.sdk.client.response.ScimServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;


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
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimServerResponse<User> response = new CreateBuilder<>(getServerUrl(), scimClientConfig,
                                                            User.class).setEndpoint(EndpointPaths.USERS)
                                                                       .setResource(User.builder()
                                                                                        .userName("goldfish")
                                                                                        .build())
                                                                       .sendRequest();
    Assertions.assertEquals(CreateResponse.class, response.getScimResponse().getClass());
    Assertions.assertEquals(ResponseType.CREATE, response.getResponseType());
    Assertions.assertEquals(User.class, response.getResourceType());
    Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
    Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));

    User user = response.getResource();
    Assertions.assertEquals("goldfish", user.getUserName().get());
    Assertions.assertEquals(user.getMeta().get().getVersion().get().getEntityTag(),
                            response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
  }
}
