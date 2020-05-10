package de.captaingoldfish.scim.sdk.keycloak.auth;

import java.util.Collections;
import java.util.Set;

import org.keycloak.models.KeycloakSession;

import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * author Pascal Knueppel <br>
 * created at: 05.02.2020 <br>
 * <br>
 * this class is simply used within this example to pass the keycloak session into the resource handlers
 */
@Data
@AllArgsConstructor
public class ScimAuthorization implements Authorization
{

  /**
   * the keycloak session that is passed to the resource endpoints
   */
  private KeycloakSession keycloakSession;

  /**
   * only used for dedicated error messages
   */
  @Override
  public String getClientId()
  {
    return null;
  }

  /**
   * this can be used if authorization on endpoint level is desirable
   */
  @Override
  public Set<String> getClientRoles()
  {
    return Collections.emptySet();
  }
}
