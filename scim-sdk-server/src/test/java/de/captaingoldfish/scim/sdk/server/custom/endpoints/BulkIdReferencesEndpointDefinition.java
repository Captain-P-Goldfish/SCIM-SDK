package de.captaingoldfish.scim.sdk.server.custom.endpoints;

import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.custom.resourcehandler.BulkIdReferencesResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;


public class BulkIdReferencesEndpointDefinition extends EndpointDefinition
{

  // @formatter:off
  private static final String RESOURCE_TYPE_LOCATION =
    "/de/captaingoldfish/scim/sdk/server/files/types/bulkId-references-resource-type.json";
  private static final String RESOURCE_SCHEMA_LOCATION =
    "/de/captaingoldfish/scim/sdk/server/files/schemas/bulk-id-references.json";
  // @formatter:on

  public BulkIdReferencesEndpointDefinition()
  {
    super(JsonHelper.loadJsonDocument(RESOURCE_TYPE_LOCATION), JsonHelper.loadJsonDocument(RESOURCE_SCHEMA_LOCATION),
          null, new BulkIdReferencesResourceHandler());
  }
}
