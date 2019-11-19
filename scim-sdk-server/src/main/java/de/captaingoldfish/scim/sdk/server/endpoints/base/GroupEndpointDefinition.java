package de.captaingoldfish.scim.sdk.server.endpoints.base;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 20:44 <br>
 * <br>
 * this is the default endpoint definition for the /Groups endpoint as it was defined in the RFC7643
 */
public class GroupEndpointDefinition extends EndpointDefinition
{

  public GroupEndpointDefinition(ResourceHandler<Group> resourceHandler)
  {
    super(JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_RESOURCE_TYPE_JSON),
          JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON), null, resourceHandler);
  }
}
