package de.captaingoldfish.scim.sdk.server.endpoints.authorize;

import java.util.Set;


/**
 * author Pascal Knueppel <br>
 * created at: 27.11.2019 - 17:05 <br>
 * <br>
 * this interface may be used by the developer to pass authorization information about the user into this
 * framework it will also be delivered into the handler implementations so that a developer is also able to
 * pass arbitrary information's to the own implementation
 */
public interface Authorization
{

  /**
   * this is just a marker for error messages that will be printed into the log for debug purposes to be able to
   * identify the client that tried to do a forbidden action
   */
  public String getClientId();

  /**
   * @return the roles that an authenticated client possesses
   */
  public Set<String> getClientRoles();

}
