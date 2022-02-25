package de.captaingoldfish.scim.sdk.client.setup.scim.endpoints;

import de.captaingoldfish.scim.sdk.client.setup.FileReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;


/**
 * @author Pascal Knueppel
 * @since 25.02.2022
 */
public class TestSingletonEndpoint extends EndpointDefinition implements FileReferences
{


  public TestSingletonEndpoint(ResourceHandler resourceHandler)
  {
    super(JsonHelper.loadJsonDocument(TEST_SINGLTON_RESOURCE_TYPE_JSON),
          JsonHelper.loadJsonDocument(TEST_SINGLTON_RESOURCE_SCHEMA_JSON), null, resourceHandler);
  }
}
