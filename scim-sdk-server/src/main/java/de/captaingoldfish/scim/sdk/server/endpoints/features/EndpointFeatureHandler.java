package de.captaingoldfish.scim.sdk.server.endpoints.features;

import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.DefaultAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
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
   * a custom implementation that is used in case that the provider did not give any authorization information's
   * about the user. This is necessary to check if the endpoint itself has defined any necessary roles to get
   * accessed. If yes and no authorization is passed by the developer this implementation assures that the
   * authorization is properly executed and a
   * {@link de.captaingoldfish.scim.sdk.common.exceptions.ForbiddenException} is thrown
   */
  private static final DefaultAuthorization DEFAULT_AUTHORIZATION = new DefaultAuthorization();

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
    handleAuthorization(resourceType, endpointType, authorization);
  }

  /**
   * handles the authorization feature
   * 
   * @param resourceType the resource type that represents the accessed endpoint
   * @param endpointType the endpoint type that was called e.g. create or update
   * @param authorization the authorization implementation from the provider
   */
  private static void handleAuthorization(ResourceType resourceType,
                                          EndpointType endpointType,
                                          Authorization authorization)
  {
    ResourceTypeAuthorization resourceTypeAuthorization = resourceType.getFeatures().getAuthorization();
    if (!resourceTypeAuthorization.isAuthenticated())
    {
      return;
    }
    if (authorization != null)
    {
      authorization.isClientAuthorized(resourceType, endpointType);
    }
    else
    {
      log.trace("No authorization information for the current client on resource endpoint '{}' for endpoint-type "
                + "'{}'. Using default authorization handler",
                resourceType.getEndpoint(),
                endpointType);
      DEFAULT_AUTHORIZATION.isClientAuthorized(resourceType, endpointType);
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
