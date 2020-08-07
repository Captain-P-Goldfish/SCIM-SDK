package de.captaingoldfish.scim.sdk.keycloak.scim;

import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
public class ScimEndpointTest extends KeycloakScimManagementTest
{

  @Test
  public void testScimEndpointTest()
  {
    ScimEndpoint scimEndpoint = getScimEndpoint();
  }
}
