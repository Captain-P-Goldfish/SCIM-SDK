package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;


/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 00:44 <br>
 * <br>
 * the endpoint definition is used to register new endpoints into the application context
 */
@Data
public class EndpointDefinition
{

  /**
   * the resource type definition that defines the SCIM endpoint
   */
  private JsonNode resourceType;

  /**
   * the main resource schema that represents the endpoint e.g. the User schema
   */
  private JsonNode resourceSchema;

  /**
   * the extensions that must or might be added to the resource that represents the endpoint
   */
  private List<JsonNode> resourceSchemaExtensions;

  /**
   * the implementation that handles the resources
   */
  private ResourceHandler resourceHandler;

  public EndpointDefinition(JsonNode resourceType,
                            JsonNode resourceSchema,
                            List<JsonNode> resourceSchemaExtensions,
                            ResourceHandler resourceHandler)
  {
    this.resourceType = resourceType;
    this.resourceSchema = resourceSchema;
    this.resourceSchemaExtensions = resourceSchemaExtensions == null ? new ArrayList<>() : resourceSchemaExtensions;
    this.resourceHandler = Objects.requireNonNull(resourceHandler, "the resource handler implementation is mandatory");
  }
}
