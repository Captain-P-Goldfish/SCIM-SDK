package de.captaingoldfish.scim.sdk.server.custom.endpoints;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
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

  private static final JsonNode SCHEMA_EXTENSION;

  static
  {
    SCHEMA_EXTENSION = JsonHelper.loadJsonDocument(RESOURCE_SCHEMA_LOCATION);
    ((ObjectNode)SCHEMA_EXTENSION).set(AttributeNames.RFC7643.ID,
                                       new TextNode("custom:captaingoldfish:scim:schemas:extensions:2.0:BulkIdReferences"));
  }

  public BulkIdReferencesEndpointDefinition()
  {
    super(JsonHelper.loadJsonDocument(RESOURCE_TYPE_LOCATION), JsonHelper.loadJsonDocument(RESOURCE_SCHEMA_LOCATION),
          Arrays.asList(SCHEMA_EXTENSION), new BulkIdReferencesResourceHandler());
  }
}
