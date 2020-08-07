package de.captaingoldfish.scim.sdk.keycloak.scim;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
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
}
