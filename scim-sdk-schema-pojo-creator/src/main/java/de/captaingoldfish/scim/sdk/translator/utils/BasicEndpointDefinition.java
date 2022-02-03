package de.captaingoldfish.scim.sdk.translator.utils;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;


/**
 * @author Pascal Knueppel
 * @since 28.01.2022
 */
public class BasicEndpointDefinition extends EndpointDefinition
{

  public BasicEndpointDefinition(JsonNode resourceType,
                                 JsonNode resourceSchema,
                                 List<JsonNode> resourceSchemaExtensions)
  {
    super(resourceType, resourceSchema, resourceSchemaExtensions, new BasicResourceHandler());
  }
}
