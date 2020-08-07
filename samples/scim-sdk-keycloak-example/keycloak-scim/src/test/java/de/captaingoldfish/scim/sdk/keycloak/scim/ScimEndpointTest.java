package de.captaingoldfish.scim.sdk.keycloak.scim;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.BooleanNode;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
@Slf4j
public class ScimEndpointTest extends KeycloakScimManagementTest
{

  /**
   * verifies that a second instantiation of the endpoint does not create additional unwanted database entries
   */
  @Test
  public void testRecreateEndpoint()
  {
    // the endpoint is already initialized by method super.initializeEndpoint()
    new ScimEndpoint(getKeycloakSession(), getAuthentication());
    Assertions.assertEquals(1, countEntriesInTable(ScimServiceProviderEntity.class));
    Assertions.assertEquals(2, countEntriesInTable(ScimResourceTypeEntity.class));
  }

  /**
   * verify that the scim endpoint is accessible
   */
  @Test
  public void testScimEndpointTest()
  {
    ScimEndpoint scimEndpoint = getScimEndpoint();
    HttpServletRequest request = RequestBuilder.builder(scimEndpoint).endpoint(EndpointPaths.USERS).build();

    Response response = scimEndpoint.handleScimRequest(request);

    Assertions.assertEquals(HttpStatus.OK, response.getStatus());
  }

  /**
   * this test verifies that the scim endpoint cannot be accessed if scim was disabled for the specific realm
   */
  @Test
  public void testDisableScim()
  {
    ServiceProvider serviceProvider = getScimEndpoint().getResourceEndpoint().getServiceProvider();
    serviceProvider.set("enabled", BooleanNode.valueOf(false));
    AdminstrationResource adminstrationResource = getScimEndpoint().administerResources();
    adminstrationResource.updateServiceProviderConfig(serviceProvider.toString());

    ScimEndpoint scimEndpoint = getScimEndpoint();
    HttpServletRequest request = RequestBuilder.builder(scimEndpoint).endpoint(EndpointPaths.USERS).build();

    try
    {
      scimEndpoint.handleScimRequest(request);
      Assertions.fail("this point must not be reached");
    }
    catch (NotFoundException ex)
    {
      log.trace("everything's fine", ex);
    }
  }
}
