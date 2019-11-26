package de.captaingoldfish.scim.sdk.server.endpoints.features;

import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;


/**
 * author Pascal Knueppel <br>
 * created at: 26.11.2019 - 09:51 <br>
 * <br>
 * this class is used to handle additional features for endpoints
 */
public class EndpointFeatureHandler
{

  /**
   * this method checks if the current used endpoint is disabled and throws a {@link NotImplementedException} if
   * the support for this endpoint was disabled
   *
   * @param resourceType the current resource type to get access to the endpoint control settings
   * @param endpointType the endpoint type that the client tries to access
   */
  public static void isEndpointEnabled(ResourceType resourceType, EndpointType endpointType)
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
