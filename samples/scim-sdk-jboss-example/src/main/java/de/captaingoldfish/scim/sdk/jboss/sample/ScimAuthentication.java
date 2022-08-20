package de.captaingoldfish.scim.sdk.jboss.sample;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;


/**
 * author Pascal Knueppel <br>
 * created at: 20.08.2022 - 11:44 <br>
 * <br>
 */
public class ScimAuthentication implements Authorization
{

  @Override
  public Set<String> getClientRoles()
  {
    // get the roles of the user for authorizaiton access:
    // see https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Authentication-and-Authorization#authorization
    return Collections.singleton("admin");
  }

  @Override
  public boolean authenticate(Map<String, String> httpHeaders, Map<String, String> queryParams)
  {
    // authenticate the user
    // see https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Authentication-and-Authorization#authentication
    return true;
  }
}
