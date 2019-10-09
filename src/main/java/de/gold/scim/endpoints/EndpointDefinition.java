package de.gold.scim.endpoints;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 00:44 <br>
 * <br>
 */
@Data
@AllArgsConstructor
public class EndpointDefinition
{

  private JsonNode resourceType;

  private JsonNode resourceSchema;

  private List<JsonNode> resourceSchemaExtensions;

  private ResourceHandler resourceHandler;

}
