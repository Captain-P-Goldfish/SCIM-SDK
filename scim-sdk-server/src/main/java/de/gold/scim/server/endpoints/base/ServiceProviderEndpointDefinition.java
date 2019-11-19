package de.gold.scim.server.endpoints.base;

import de.gold.scim.common.constants.ClassPathReferences;
import de.gold.scim.common.resources.ServiceProvider;
import de.gold.scim.common.utils.JsonHelper;
import de.gold.scim.server.endpoints.EndpointDefinition;
import de.gold.scim.server.endpoints.handler.ServiceProviderHandler;
import de.gold.scim.server.schemas.ResourceType;


/**
 * author Pascal Knueppel <br>
 * created at: 17.10.2019 - 22:41 <br>
 * <br>
 * Represents the standard {@link ResourceType} endpoint definition that is registered on the fly. But if
 * wanted the registration can also be overridden with a new implementation
 */
public class ServiceProviderEndpointDefinition extends EndpointDefinition
{

  /**
   * @param serviceProvider each created {@link de.gold.scim.server.endpoints.ResourceEndpointHandler} must get
   *          hold of a single * {@link ServiceProvider} instance which is shared with this object. so both
   *          instances need to hold the same * object reference in order for the application to work correctly
   */
  public ServiceProviderEndpointDefinition(ServiceProvider serviceProvider)
  {
    super(JsonHelper.loadJsonDocument(ClassPathReferences.SERVICE_PROVIDER_RESOURCE_TYPE_JSON),
          JsonHelper.loadJsonDocument(ClassPathReferences.META_SERVICE_PROVIDER_JSON), null,
          new ServiceProviderHandler(serviceProvider));
  }

}
