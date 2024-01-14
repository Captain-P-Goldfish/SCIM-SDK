package de.captaingoldfish.scim.sdk.server.endpoints;

import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import lombok.experimental.UtilityClass;


/**
 * @author Pascal Knueppel
 * @since 07.01.2024
 */
@UtilityClass
public class ResourceEndpointBridge
{

  public static ResourceTypeFactory getResourceTypeFactory(ResourceEndpoint resourceEndpoint)
  {
    return resourceEndpoint.getResourceTypeFactory();
  }

}
