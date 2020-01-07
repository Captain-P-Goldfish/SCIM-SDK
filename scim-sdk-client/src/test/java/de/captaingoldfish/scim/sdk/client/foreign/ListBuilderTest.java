package de.captaingoldfish.scim.sdk.client.foreign;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.builder.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.builder.ScimRequestBuilder;
import de.captaingoldfish.scim.sdk.client.constants.ResponseType;
import de.captaingoldfish.scim.sdk.client.response.ScimServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
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
    ScimServerResponse<User> response = scimRequestBuilder.list(User.class)
                                                          .get()
                                                          .setEndpoint(EndpointPaths.USERS)
                                                          .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertEquals(ResponseType.LIST, response.getResponseType());
    Assertions.assertTrue(response.getScimResponse().isPresent());
    Assertions.assertEquals(ListResponse.class, response.getScimResponse().get().getClass());
  }
}
