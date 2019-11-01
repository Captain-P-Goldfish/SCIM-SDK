package de.gold.scim.server.endpoints.base;

import de.gold.scim.server.constants.ClassPathReferences;
import de.gold.scim.server.endpoints.EndpointDefinition;
import de.gold.scim.server.endpoints.handler.SchemaHandler;
import de.gold.scim.server.schemas.ResourceTypeFactory;
import de.gold.scim.server.utils.JsonHelper;


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
