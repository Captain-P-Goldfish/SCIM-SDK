package de.captaingoldfish.scim.sdk.server.endpoints.authorize;

import java.util.Collections;
import java.util.Set;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 11:31 <br>
 * <br>
 * a default implementation for authorization that is used if the developer did not give any authorization
 * information's
 */
public final class DefaultAuthorization implements Authorization
{

  /**
   * {@inheritDoc}
   */
  @Override
  public String getClientId()
  {
    return "Anonymous";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getClientRoles()
  {
    return Collections.emptySet();
  }
}
