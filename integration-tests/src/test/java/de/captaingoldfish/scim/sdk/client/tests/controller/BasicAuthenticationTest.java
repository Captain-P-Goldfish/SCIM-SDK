package de.captaingoldfish.scim.sdk.client.tests.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.ScimRequestBuilder;
import de.captaingoldfish.scim.sdk.client.http.BasicAuth;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.tests.constants.TestConstants;
import de.captaingoldfish.scim.sdk.client.tests.scim.UserHandler;
import de.captaingoldfish.scim.sdk.client.tests.setup.BasicAuthTest;
import de.captaingoldfish.scim.sdk.client.tests.setup.ServerTest;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.User;


/**
 * <br>
 * <br>
 * created at: 05.05.2020
 *
 * @author Pascal Knueppel
 */

@BasicAuthTest
public class BasicAuthenticationTest extends ServerTest
{

  /**
   * the user has valid credentials and the right to access the scim endpoint
   */
  private BasicAuth authorizedCredentials = BasicAuth.builder()
                                                     .username(TestConstants.AUTHORIZED_USERNAME)
                                                     .password(TestConstants.PASSWORD)
                                                     .build();

  /**
   * contains credentials for a known user but with unsupported roles
   */
  private BasicAuth noAccessCredentials = BasicAuth.builder()
                                                   .username(TestConstants.UNAUTHORIZED_USERNAME)
                                                   .password(TestConstants.PASSWORD)
                                                   .build();

  /**
   * the SCIM client that is under test
   */
  private ScimRequestBuilder scimRequestBuilder;

  /**
   * initializes the request builder for the unit tests
   */
  @BeforeEach
  public void initializeTests()
  {
    ScimClientConfig clientConfig = ScimClientConfig.builder().truststore(getTruststore()).build();
    scimRequestBuilder = new ScimRequestBuilder(getApplicationUrl(), clientConfig);
  }

  /**
   * if no authorization details are sent a 401 unauthorized must be returned
   */
  @Test
  public void testNoAuthorizationUsed()
  {
    UserHandler userHandler = (UserHandler)getResourceEndpoint().getResourceTypeByName(ResourceTypeNames.USER)
                                                                .get()
                                                                .getResourceHandlerImpl();
    String userId = userHandler.getInMemoryMap().keySet().iterator().next();
    ServerResponse<User> response = scimRequestBuilder.get(User.class, EndpointPaths.USERS, userId).sendRequest();
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getHttpStatus());
  }

  /**
   * verifies that the authentication is successful if the basic credentials are sent along in the request
   */
  @Test
  public void testSuccessfulAuthentication()
  {
    scimRequestBuilder.getScimClientConfig().setBasicAuth(authorizedCredentials);
    UserHandler userHandler = (UserHandler)getResourceEndpoint().getResourceTypeByName(ResourceTypeNames.USER)
                                                                .get()
                                                                .getResourceHandlerImpl();
    String userId = userHandler.getInMemoryMap().keySet().iterator().next();
    ServerResponse<User> response = scimRequestBuilder.get(User.class, EndpointPaths.USERS, userId).sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
  }

  /**
   * verifies that the authentication is successful if the basic credentials are sent along in the request
   */
  @Test
  public void testX509OnBasicAuthentication()
  {
    scimRequestBuilder.getScimClientConfig().setClientAuth(getAuthorizedKey());
    UserHandler userHandler = (UserHandler)getResourceEndpoint().getResourceTypeByName(ResourceTypeNames.USER)
                                                                .get()
                                                                .getResourceHandlerImpl();
    String userId = userHandler.getInMemoryMap().keySet().iterator().next();
    ServerResponse<User> response = scimRequestBuilder.get(User.class, EndpointPaths.USERS, userId).sendRequest();
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getHttpStatus());
  }

  /**
   * will test that access will not work if the user does not have the appropriate role
   */
  @Test
  public void testAccessWithUnauthorizedUser()
  {
    scimRequestBuilder.getScimClientConfig().setBasicAuth(noAccessCredentials);
    UserHandler userHandler = (UserHandler)getResourceEndpoint().getResourceTypeByName(ResourceTypeNames.USER)
                                                                .get()
                                                                .getResourceHandlerImpl();
    String userId = userHandler.getInMemoryMap().keySet().iterator().next();
    ServerResponse<User> response = scimRequestBuilder.get(User.class, EndpointPaths.USERS, userId).sendRequest();
    Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getHttpStatus());
  }

}
