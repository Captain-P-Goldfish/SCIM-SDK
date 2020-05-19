package de.captaingoldfish.scim.sdk.client.tests.auth;

import java.util.Set;

import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import lombok.AllArgsConstructor;


/**
 * <br>
 * <br>
 * created at: 21.05.2020
 *
 * @author Pascal Knüppel
 */
@AllArgsConstructor
public class ScimAuthorization implements Authorization
{

  /**
   * the clientId of the authenticated user
   */
  private String clientId;

  /**
   * the current roles of the authenticated client
   */
  private Set<String> roles;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getClientId()
  {
    return clientId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getClientRoles()
  {
    return roles;
  }
}
