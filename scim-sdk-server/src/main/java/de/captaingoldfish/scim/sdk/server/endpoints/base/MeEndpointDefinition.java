package de.captaingoldfish.scim.sdk.server.endpoints.base;

import java.util.Arrays;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 20:44 <br>
 * <br>
 * this is the default endpoint definition for the /Me endpoint as it was defined in the RFC7643
 */
public class MeEndpointDefinition extends EndpointDefinition
{

  public MeEndpointDefinition(ResourceHandler<User> resourceHandler)
  {
    super(JsonHelper.loadJsonDocument(ClassPathReferences.ME_RESOURCE_TYPE_JSON),
          JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON),
          Arrays.asList(JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON)), resourceHandler);
  }
}
