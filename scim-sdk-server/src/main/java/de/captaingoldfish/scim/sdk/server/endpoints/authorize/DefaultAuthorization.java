package de.captaingoldfish.scim.sdk.server.endpoints.authorize;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 11:31 <br>
 * <br>
 * a default implementation for authorization that is used if the developer did not give any authorization
 * information's
 */
@Slf4j
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

  /**
   * the default authorization object will always return true to authenticate the user. Be careful when using
   * this!
   */
  @Override
  public boolean authenticate(Map<String, String> httpHeaders, Map<String, String> queryParams)
  {
    log.warn("Used default authentication that will always return true. Please see '{}' and '{}' to check "
             + "how to utilize the authentication and authorization features or how to disable it.",
             "https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Authentication-and-Authorization#authentication",
             "https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Authentication-and-Authorization#authorization");
    return true;
  }
}
