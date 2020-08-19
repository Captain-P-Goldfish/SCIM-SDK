package de.captaingoldfish.scim.sdk.client.tests.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.ScimRequestBuilder;
import de.captaingoldfish.scim.sdk.client.exceptions.SSLHandshakeRuntimeException;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.tests.scim.UserHandler;
import de.captaingoldfish.scim.sdk.client.tests.setup.ServerTest;
import de.captaingoldfish.scim.sdk.client.tests.setup.X509AuthTest;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.User;
import lombok.extern.slf4j.Slf4j;


/**
 * <br>
 * <br>
 * created at: 05.05.2020
 *
 * @author Pascal Knueppel
 */
@Slf4j
@X509AuthTest
class X509AuthenticationTest extends ServerTest
{

  /**
   * the SCIM client that is under test
   */
  private ScimRequestBuilder scimRequestBuilder;

  /**
   * initializes the request builder
   */
  @BeforeEach
  public void initializeTests()
  {
    ScimClientConfig clientConfig = ScimClientConfig.builder().truststore(getTruststore()).build();
    scimRequestBuilder = new ScimRequestBuilder(getApplicationUrl(), clientConfig);
  }

  /**
   * tests that authentication fails if no authentication details are given
   */
  @Test
  public void testWithoutAnyAuthentication()
  {
    UserHandler userHandler = (UserHandler)getResourceEndpoint().getResourceTypeByName(ResourceTypeNames.USER)
                                                                .get()
                                                                .getResourceHandlerImpl();
    String userId = userHandler.getInMemoryMap().keySet().iterator().next();
    try
    {
      scimRequestBuilder.get(User.class, EndpointPaths.USERS, userId).sendRequest();
      Assertions.fail("this point must not be reached");
    }
    catch (SSLHandshakeRuntimeException ex)
    {
      log.trace(ex.getMessage(), ex);
    }
  }

  /**
   * tests that authentication succeeds if the client keystore is configured
   */
  @Test
  public void testWithSuccessfulClientAuthentication()
  {
    scimRequestBuilder.getScimClientConfig().setClientAuth(getAuthorizedKey());
    UserHandler userHandler = (UserHandler)getResourceEndpoint().getResourceTypeByName(ResourceTypeNames.USER)
                                                                .get()
                                                                .getResourceHandlerImpl();
    String userId = userHandler.getInMemoryMap().keySet().iterator().next();
    ServerResponse<User> response = scimRequestBuilder.get(User.class, EndpointPaths.USERS, userId).sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
  }



  /**
   * tests that authorization fails if the authenticated client keystore does not have the corresponding role
   */
  @Test
  public void testWithUnauthorizedClientAuthentication()
  {
    scimRequestBuilder.getScimClientConfig().setClientAuth(getUnauthorizedKey());
    UserHandler userHandler = (UserHandler)getResourceEndpoint().getResourceTypeByName(ResourceTypeNames.USER)
                                                                .get()
                                                                .getResourceHandlerImpl();
    String userId = userHandler.getInMemoryMap().keySet().iterator().next();
    ServerResponse<User> response = scimRequestBuilder.get(User.class, EndpointPaths.USERS, userId).sendRequest();
    Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getHttpStatus());
  }
}
