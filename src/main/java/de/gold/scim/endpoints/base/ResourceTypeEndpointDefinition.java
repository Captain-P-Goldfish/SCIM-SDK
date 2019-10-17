package de.gold.scim.endpoints.base;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.endpoints.EndpointDefinition;
import de.gold.scim.endpoints.handler.ResourceTypeHandler;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 17.10.2019 - 22:41 <br>
 * <br>
 * Represents the standard {@link de.gold.scim.schemas.ResourceType} endpoint definition that is registered on
 * the fly. But if wanted the registration can also be overridden with a new implementation
 */
public class ResourceTypeEndpointDefinition extends EndpointDefinition
{

  /**
   * the factory is used for unit tests to prevent application context pollution
   */
  private ResourceTypeFactory resourceTypeFactory;

  protected ResourceTypeEndpointDefinition(ResourceTypeFactory resourceTypeFactory)
  {
    super(JsonHelper.loadJsonDocument(ClassPathReferences.RESOURCE_TYPE_RESOURCE_TYPE_JSON),
          JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON), null,
          new ResourceTypeHandler(resourceTypeFactory));
  }

  public ResourceTypeEndpointDefinition()
  {
    this(ResourceTypeFactory.getInstance());
  }


}
