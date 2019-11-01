package de.gold.scim.endpoints.base;

import java.util.Arrays;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.endpoints.EndpointDefinition;
import de.gold.scim.endpoints.ResourceHandler;
import de.gold.scim.resources.User;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 20:44 <br>
 * <br>
 * this is the default endpoint definition for the /Users endpoint as it was defined in the RFC7643
 */
public class UserEndpointDefinition extends EndpointDefinition
{

  public UserEndpointDefinition(ResourceHandler<User> resourceHandler)
  {
    super(JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON),
          JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON),
          Arrays.asList(JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON)), resourceHandler);
  }
}
