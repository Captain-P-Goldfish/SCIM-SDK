package de.captaingoldfish.scim.sdk.keycloak.auth;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.NotAuthorizedException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminAuth;

import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 05.02.2020 <br>
 * <br>
 * this class is simply used within this example to pass the keycloak session into the resource handlers
 */
@Slf4j
@Data
@RequiredArgsConstructor
public class ScimAuthorization implements Authorization
{

  /**
   * the keycloak session that is passed to the resource endpoints
   */
  private final KeycloakSession keycloakSession;

  /**
   * the current authentication of the client / user
   */
  private AdminAuth authResult;

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

  /**
   * authenticates the user
   */
  @Override
  public boolean authenticate(Map<String, String> httpHeaders, Map<String, String> queryParams)
  {
    if (authResult == null)
    {

      try
      {
        // authResult = Authentication.authenticate(keycloakSession);
        return true;
      }
      catch (NotAuthorizedException ex)
      {
        log.error("authentication failed", ex);
        return false;
      }
    }
    else
    {
      return true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRealm()
  {
    return keycloakSession.getContext().getRealm().getName();
  }
}
