package de.captaingoldfish.scim.sdk.server.endpoints.base;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.SchemaHandler;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;


/**
 * author Pascal Knueppel <br>
 * created at: 20.10.2019 - 16:40 <br>
 * <br>
 */
public class SchemaEndpointDefinition extends EndpointDefinition
{

  public SchemaEndpointDefinition(ResourceTypeFactory resourceTypeFactory)
  {
    super(JsonHelper.loadJsonDocument(ClassPathReferences.SCHEMA_RESOURCE_TYPE_JSON),
          JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON), null,
          new SchemaHandler(resourceTypeFactory));
  }
}
