package de.gold.scim.endpoints;

import de.gold.scim.resources.ServiceProvider;
import de.gold.scim.schemas.ResourceTypeFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 00:01 <br>
 * <br>
 * a helper class to create a {@link ResourceEndpointHandler} with a custom resource type factory for any unit
 * test
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceEndpointHandlerUtil
{

  /**
   * creates a new endpoint definition with the given {@link ResourceTypeFactory}
   */
  public static ResourceEndpointHandler getUnitTestResourceEndpointHandler(ResourceTypeFactory resourceTypeFactory,
                                                                           EndpointDefinition... endpointDefinitions)
  {
    return getUnitTestResourceEndpointHandler(resourceTypeFactory,
                                              ServiceProvider.builder().build(),
                                              endpointDefinitions);
  }

  /**
   * creates a new endpoint definition with the given {@link ResourceTypeFactory}
   */
  public static ResourceEndpointHandler getUnitTestResourceEndpointHandler(ResourceTypeFactory resourceTypeFactory,
                                                                           ServiceProvider serviceProvider,
                                                                           EndpointDefinition... endpointDefinitions)
  {
    return new ResourceEndpointHandler(resourceTypeFactory, serviceProvider, endpointDefinitions);
  }
}
