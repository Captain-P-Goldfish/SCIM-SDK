package de.gold.scim.endpoints.base;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.endpoints.EndpointDefinition;
import de.gold.scim.endpoints.ResourceHandler;
import de.gold.scim.resources.Group;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 20:44 <br>
 * <br>
 * this is the default endpoint definition for the /Groups endpoint as it was defined in the RFC7643
 */
public class GroupResourceType extends EndpointDefinition
{

  public GroupResourceType(ResourceHandler<Group> resourceHandler)
  {
    super(JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_RESOURCE_TYPE_JSON),
          JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON), null, resourceHandler);
  }
}
