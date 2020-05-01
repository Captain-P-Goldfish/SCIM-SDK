package de.captaingoldfish.scim.sdk.client.builder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.constants.ResponseType;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
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
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ScimServerResponse<User> response = new CreateBuilder<>(getServerUrl(), EndpointPaths.USERS, scimClientConfig,
                                                            User.class, scimHttpClient)
                                                                                       .setResource(User.builder()
                                                                                                        .userName("goldfish")
                                                                                                        .build())
                                                                                       .sendRequest();
    Assertions.assertEquals(CreateResponse.class, response.getScimResponse().get().getClass());
    Assertions.assertEquals(ResponseType.CREATE, response.getResponseType());
    Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
    Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));

    Assertions.assertTrue(response.getResource().isPresent());
    User user = response.getResource().get();
    Assertions.assertEquals("goldfish", user.getUserName().get());
    Assertions.assertEquals(user.getMeta().get().getVersion().get().getEntityTag(),
                            response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
  }
}
