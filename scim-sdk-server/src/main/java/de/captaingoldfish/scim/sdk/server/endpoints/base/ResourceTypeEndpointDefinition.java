package de.captaingoldfish.scim.sdk.server.endpoints.base;

import java.util.Collections;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.ResourceTypeHandler;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;


/**
 * author Pascal Knueppel <br>
 * created at: 17.10.2019 - 22:41 <br>
 * <br>
 * Represents the standard {@link ResourceType} endpoint definition that is registered on the fly. But if
 * wanted the registration can also be overridden with a new implementation
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
