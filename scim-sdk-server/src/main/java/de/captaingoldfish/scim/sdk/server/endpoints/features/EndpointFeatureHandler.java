package de.captaingoldfish.scim.sdk.server.endpoints.features;

import java.util.Set;

import de.captaingoldfish.scim.sdk.common.exceptions.ForbiddenException;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 26.11.2019 - 09:51 <br>
 * <br>
 * this class is used to handle additional features for endpoints
 */
@Slf4j
public class EndpointFeatureHandler
{

  /**
   * handles several checks for the currently accessed endpoint on the given resource type
   *
   * @param resourceType the current resource type to get access to the endpoint control settings
   * @param endpointType the endpoint type that the client tries to access
   * @param authorization should return the roles of an user and may contain arbitrary data needed in the
   *          handler implementation
   */
  public static void handleEndpointFeatures(ResourceType resourceType,
                                            EndpointType endpointType,
                                            Authorization authorization)
  {
    isEndpointEnabled(resourceType, endpointType);
    isClientAuthorized(resourceType, endpointType, authorization);
  }

  /**
   * verifies if the client is authorized to access the given endpoint and will throw a forbidden except
   *
   * @param resourceType the resource type that might hold information's about the needed authorization on the
   *          given endpoints
   * @param endpointType the endpoint type the client tries to access
   * @param authorization should give us the roles that have been granted to the currently authenticated client
   */
  private static void isClientAuthorized(ResourceType resourceType,
                                         EndpointType endpointType,
                                         Authorization authorization)
  {
    switch (endpointType)
    {
      case CREATE:
        isAuthorized(resourceType,
                     endpointType,
                     authorization,
                     resourceType.getFeatures().getAuthorization().getRolesCreate());
        break;
      case UPDATE:
        isAuthorized(resourceType,
                     endpointType,
                     authorization,
                     resourceType.getFeatures().getAuthorization().getRolesUpdate());
        break;
      case DELETE:
        isAuthorized(resourceType,
                     endpointType,
                     authorization,
                     resourceType.getFeatures().getAuthorization().getRolesDelete());
        break;
      default:
        isAuthorized(resourceType,
                     endpointType,
                     authorization,
                     resourceType.getFeatures().getAuthorization().getRolesGet());
    }
  }

  /**
   * checks if the current client is authorized to access the given endpoint
   *
   * @param resourceType the resource type on which the endpoint is accessed
   * @param endpointType the method that was called by the client
   * @param authorization the authorization about the currently authenticated client
   * @param roles the required roles to access the given endpoint
   */
  private static void isAuthorized(ResourceType resourceType,
                                   EndpointType endpointType,
                                   Authorization authorization,
                                   Set<String> roles)
  {
    if (roles == null || roles.isEmpty())
    {
      return;
    }
    if (authorization == null)
    {
      log.debug("the resource endpoint '{}' requires authorization but there was no authorization information "
                + "passed. required roles: '{}'",
                endpointType,
                roles);
      throw new ForbiddenException("you are not authorized to access the '" + endpointType
                                   + "' endpoint on resource type '" + resourceType.getName() + "'");
    }
    if (!authorization.getClientRoles().containsAll(roles))
    {
      log.debug("the client '{}' tried to execute an action without proper authorization. "
                + "Required authorization is '{}' but the client has '{}'",
                authorization.getClientId(),
                roles,
                authorization.getClientRoles());
      throw new ForbiddenException("you are not authorized to access the '" + endpointType
                                   + "' endpoint on resource type '" + resourceType.getName() + "'");
    }
  }

  /**
   * this method checks if the current used endpoint is disabled and throws a {@link NotImplementedException} if
   * the support for this endpoint was disabled
   *
   * @param resourceType the current resource type to get access to the endpoint control settings
   * @param endpointType the endpoint type that the client tries to access
   */
  private static void isEndpointEnabled(ResourceType resourceType, EndpointType endpointType)
  {
    if (resourceType.isDisabled())
    {
      throw new NotImplementedException("the resource type '" + resourceType.getName() + "' is disabled");
    }
    EndpointControlFeature endpointControlFeature = resourceType.getFeatures().getEndpointControlFeature();
    switch (endpointType)
    {
      case CREATE:
        if (endpointControlFeature.isCreateDisabled())
        {
          throw new NotImplementedException("create is not supported for resource type '" + resourceType.getName()
                                            + "'");
        }
        break;
      case GET:
        if (endpointControlFeature.isGetDisabled())
        {
          throw new NotImplementedException("get is not supported for resource type '" + resourceType.getName() + "'");
        }
        break;
      case LIST:
        if (endpointControlFeature.isListDisabled())
        {
          throw new NotImplementedException("list is not supported for resource type '" + resourceType.getName() + "'");
        }
        break;
      case UPDATE:
        if (endpointControlFeature.isUpdateDisabled())
        {
          throw new NotImplementedException("update is not supported for resource type '" + resourceType.getName()
                                            + "'");
        }
        break;
      case DELETE:
        if (endpointControlFeature.isDeleteDisabled())
        {
          throw new NotImplementedException("delete is not supported for resource type '" + resourceType.getName()
                                            + "'");
        }
        break;
    }
  }
}
