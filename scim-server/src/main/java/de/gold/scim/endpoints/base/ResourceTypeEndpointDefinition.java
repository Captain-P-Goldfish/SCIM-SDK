package de.gold.scim.endpoints.base;

import java.util.Collections;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.endpoints.EndpointDefinition;
import de.gold.scim.endpoints.ResourceHandler;
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

  public ResourceTypeEndpointDefinition(ResourceTypeFactory resourceTypeFactory)
  {
    this(new ResourceTypeHandler(resourceTypeFactory));
  }

  public ResourceTypeEndpointDefinition(ResourceHandler resourceHandler)
  {
    super(JsonHelper.loadJsonDocument(ClassPathReferences.RESOURCE_TYPE_RESOURCE_TYPE_JSON),
          JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON),
          Collections.singletonList(JsonHelper.loadJsonDocument(ClassPathReferences.RESOURCE_TYPES_FILTER_EXT_JSON)),
          resourceHandler);
  }

}
