package de.captaingoldfish.scim.sdk.server.endpoints.authorize;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.captaingoldfish.scim.sdk.common.exceptions.ForbiddenException;
import de.captaingoldfish.scim.sdk.server.endpoints.features.EndpointType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;


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

  public static final Logger log = LoggerFactory.getLogger(Authorization.class);

  /**
   * this is just a marker for error messages that will be printed into the log for debug purposes to be able to
   * identify the client that tried to do a forbidden action
   */
  public String getClientId();

  /**
   * @return the roles that an authenticated client possesses
   */
  public Set<String> getClientRoles();

  /**
   * verifies if the client is authorized to access the given endpoint and will throw a forbidden except
   *
   * @param resourceType the resource type that might hold information's about the needed authorization on the
   *          given endpoints
   * @param endpointType the endpoint type the client tries to access
   */
  default void isClientAuthorized(ResourceType resourceType, EndpointType endpointType)
  {
    switch (endpointType)
    {
      case CREATE:
        isAuthorized(resourceType, endpointType, resourceType.getFeatures().getAuthorization().getRolesCreate());
        break;
      case UPDATE:
        isAuthorized(resourceType, endpointType, resourceType.getFeatures().getAuthorization().getRolesUpdate());
        break;
      case DELETE:
        isAuthorized(resourceType, endpointType, resourceType.getFeatures().getAuthorization().getRolesDelete());
        break;
      default:
        isAuthorized(resourceType, endpointType, resourceType.getFeatures().getAuthorization().getRolesGet());
    }
  }

  /**
   * checks if the current client is authorized to access the given endpoint
   *
   * @param resourceType the resource type on which the endpoint is accessed
   * @param endpointType the method that was called by the client
   * @param roles the required roles to access the given endpoint
   */
  default void isAuthorized(ResourceType resourceType, EndpointType endpointType, Set<String> roles)
  {
    if (roles == null || roles.isEmpty())
    {
      return;
    }
    if (!Optional.ofNullable(getClientRoles()).map(clientRoles -> clientRoles.containsAll(roles)).orElse(false))
    {
      log.debug("the client '{}' tried to execute an action without proper authorization. "
                + "Required authorization is '{}' but the client has '{}'",
                getClientId(),
                roles,
                getClientRoles());
      throw new ForbiddenException("you are not authorized to access the '" + endpointType
                                   + "' endpoint on resource type '" + resourceType.getName() + "'");
    }
  }

}
