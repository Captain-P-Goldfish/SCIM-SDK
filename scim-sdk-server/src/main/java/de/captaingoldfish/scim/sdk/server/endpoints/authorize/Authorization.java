package de.captaingoldfish.scim.sdk.server.endpoints.authorize;

import java.util.HashSet;
import java.util.Map;
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
  default String getClientId()
  {
    return null;
  }

  /**
   * @return the roles that an authenticated client possesses
   */
  public Set<String> getClientRoles();

  /**
   * verifies if the client is authorized to access the given endpoint and will throw a forbidden exception
   *
   * @param resourceType the resource type that might hold information's about the needed authorization on the
   *          given endpoints
   * @param endpointType the endpoint type the client tries to access
   */
  default void isClientAuthorized(ResourceType resourceType, EndpointType endpointType)
  {
    Set<String> defaultRoles = resourceType.getFeatures().getAuthorization().getRoles();
    boolean useOrOnRoles = resourceType.getFeatures().getAuthorization().isUseOrOnRoles();
    switch (endpointType)
    {
      case CREATE:
        isAuthorized(resourceType,
                     endpointType,
                     useOrOnRoles,
                     resourceType.getFeatures().getAuthorization().getRolesCreate(),
                     defaultRoles);
        break;
      case UPDATE:
        isAuthorized(resourceType,
                     endpointType,
                     useOrOnRoles,
                     resourceType.getFeatures().getAuthorization().getRolesUpdate(),
                     defaultRoles);
        break;
      case DELETE:
        isAuthorized(resourceType,
                     endpointType,
                     useOrOnRoles,
                     resourceType.getFeatures().getAuthorization().getRolesDelete(),
                     defaultRoles);
        break;
      case LIST:
        isAuthorized(resourceType,
                     endpointType,
                     useOrOnRoles,
                     resourceType.getFeatures().getAuthorization().getRolesList(),
                     defaultRoles);
      default:
        isAuthorized(resourceType,
                     endpointType,
                     useOrOnRoles,
                     resourceType.getFeatures().getAuthorization().getRolesGet(),
                     defaultRoles);
    }
  }

  /**
   * checks if the current client is authorized to access the given endpoint
   *
   * @param resourceType the resource type on which the endpoint is accessed
   * @param endpointType the method that was called by the client
   * @param roles the required roles to access the given endpoint
   */
  default void isAuthorized(ResourceType resourceType,
                            EndpointType endpointType,
                            boolean useOrOnRoles,
                            Set<String> roles,
                            Set<String> defaultRoles)
  {
    final Set<String> effectiveRoles = new HashSet<>(Optional.ofNullable(roles).orElse(new HashSet<>()));
    if (effectiveRoles.isEmpty())
    {
      effectiveRoles.addAll(defaultRoles);
    }
    if (effectiveRoles.isEmpty())
    {
      return;
    }

    final boolean isAuthorized;
    if (useOrOnRoles)
    {
      isAuthorized = effectiveRoles.stream().anyMatch(role -> {
        return Optional.ofNullable(getClientRoles()).map(clientRoles -> clientRoles.contains(role)).orElse(false);
      });
    }
    else
    {
      isAuthorized = Optional.ofNullable(getClientRoles())
                             .map(clientRoles -> clientRoles.containsAll(effectiveRoles))
                             .orElse(false);
    }

    if (!isAuthorized)
    {
      log.debug("The client '{}' tried to execute an action without proper authorization. "
                + "Required authorization is '{}' but the client has '{}'",
                getClientId(),
                effectiveRoles,
                getClientRoles());
      throw new ForbiddenException("You are not authorized to access the '" + endpointType
                                   + "' endpoint on resource type '" + resourceType.getName() + "'");
    }
  }

  /**
   * this method can be used to authenticate a user. This method is called on a request-base which means that
   * the authentication method is executed once for each request that requires authentication
   *
   * @param httpHeaders in case that the authentication details are sent in the http headers
   * @param queryParams in case that authentication identifier are used in the query
   * @return true if the user / client was successfully be authenticated, false else
   * @see <a href=
   *      "https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Authentication-and-Authorization#authentication">
   *      https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Authentication-and-Authorization#authentication
   *      </a>
   */
  public boolean authenticate(Map<String, String> httpHeaders, Map<String, String> queryParams);

  /**
   * the current realm for which the authentication should be executed. This value will be present in the
   * WWW-Authenticate response header of the {@link de.captaingoldfish.scim.sdk.common.response.ErrorResponse}
   * object if the authentication has failed
   */
  default String getRealm()
  {
    return "SCIM";
  }

}
