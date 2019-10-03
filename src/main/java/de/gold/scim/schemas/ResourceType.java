package de.gold.scim.schemas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.exceptions.InvalidResourceTypeException;
import de.gold.scim.exceptions.ScimException;
import de.gold.scim.utils.HttpStatus;
import de.gold.scim.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:36 <br>
 * <br>
 * represents a resource type in SCIM. A resource type defines an endpoint definition that can be used by
 * clients.
 */
@Getter
@Setter
@EqualsAndHashCode
public class ResourceType
{

  /**
   * used for unit tests in order to prevent application context pollution
   */
  @Setter(AccessLevel.PROTECTED) // this is explicitly for unit tests
  private static SchemaFactory schemaFactory = SchemaFactory.getInstance();

  /**
   * the references to the meta schemas that describe this endpoint definition
   */
  private final List<String> schemas;

  /**
   * the id that should point to the name of the resource itself described by this endpoint definition
   */
  private final String id;

  /**
   * the endpoint under which the resource can be accessed
   */
  private final String endpoint;

  /**
   * the name of the document which is normally the same as the id
   */
  private String name;

  /**
   * an optional description
   */
  private String description;

  /**
   * the reference to the resource schema
   */
  private String schema;

  /**
   * the extensions that are supported by this resource
   */
  private List<SchemaExtension> schemaExtensions;

  public ResourceType(String resourceDocument)
  {
    this(JsonHelper.readJsonDocument(resourceDocument));
  }

  public ResourceType(JsonNode resourceTypeDocument)
  {
    SchemaValidator.validateSchemaForResponse(null, resourceTypeDocument);
    this.schemas = JsonHelper.getSimpleAttributeArray(resourceTypeDocument, AttributeNames.SCHEMAS)
                             .orElse(Collections.singletonList(SchemaUris.RESOURCE_TYPE_URI));
    this.id = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.ID)
                        .orElseThrow(() -> getException(missingAttrMessage(AttributeNames.ID)));
    this.name = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.NAME)
                          .orElseThrow(() -> getException(missingAttrMessage(AttributeNames.NAME)));
    this.description = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.DESCRIPTION).orElse(null);
    this.endpoint = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.ENDPOINT)
                              .orElseThrow(() -> getException(missingAttrMessage(AttributeNames.ENDPOINT)));
    this.schema = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.SCHEMA)
                            .orElseThrow(() -> getException(missingAttrMessage(AttributeNames.SCHEMA)));
    schemaExtensions = new ArrayList<>();
    JsonHelper.getArrayAttribute(resourceTypeDocument, AttributeNames.SCHEMA_EXTENSIONS).ifPresent(jsonNodes -> {
      for ( JsonNode jsonNode : jsonNodes )
      {
        schemaExtensions.add(new SchemaExtension(jsonNode));
      }
    });
  }

  /**
   * @return the list of meta schemata that do define this resource
   */
  public List<Schema> getMetaSchemata()
  {
    return schemas.stream().map(id1 -> schemaFactory.getMetaSchema(id1)).collect(Collectors.toList());
  }

  /**
   * @return the resource schema that represents this resource type
   */
  public Schema getResourceSchema()
  {
    return schemaFactory.getResourceSchema(schema);
  }

  /**
   * @return the required resource schema extensions that represents this resource type
   */
  public List<Schema> getRequiredResourceSchemaExtensions()
  {
    return schemaExtensions.stream()
                           .filter(SchemaExtension::isRequired)
                           .map(SchemaExtension::getSchema)
                           .map(schemaFactory::getResourceSchema)
                           .collect(Collectors.toList());
  }

  /**
   * @return the not required resource schema extensions that represents this resource type
   */
  public List<Schema> getNotRequiredResourceSchemaExtensions()
  {
    return schemaExtensions.stream()
                           .filter(schemaExtension -> !schemaExtension.isRequired())
                           .map(SchemaExtension::getSchema)
                           .map(schemaFactory::getResourceSchema)
                           .collect(Collectors.toList());
  }

  /**
   * builds an error message in case of a required missing attribute
   *
   * @param attributeName the name of the attribute that is missing
   * @return the error message
   */
  private String missingAttrMessage(String attributeName)
  {
    return "missing '" + attributeName + "' attribute in resource type";
  }

  /**
   * builds an exception for resource types
   *
   * @param message the error message
   * @return the exception
   */
  private ScimException getException(String message)
  {
    return new InvalidResourceTypeException(message, null, HttpStatus.SC_INTERNAL_SERVER_ERROR, null);
  }

  /**
   * @return this object as json document
   */
  public JsonNode toJsonNode()
  {
    ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
    List<JsonNode> schemaNodes = schemas.stream().map(TextNode::new).collect(Collectors.toList());
    JsonHelper.addAttribute(objectNode, AttributeNames.SCHEMAS, new ArrayNode(JsonNodeFactory.instance, schemaNodes));
    JsonHelper.addAttribute(objectNode, AttributeNames.ID, new TextNode(id));
    JsonHelper.addAttribute(objectNode, AttributeNames.NAME, new TextNode(name));
    Optional.ofNullable(description)
            .ifPresent(s -> JsonHelper.addAttribute(objectNode, AttributeNames.DESCRIPTION, new TextNode(s)));
    JsonHelper.addAttribute(objectNode, AttributeNames.SCHEMA, new TextNode(schema));
    JsonHelper.addAttribute(objectNode, AttributeNames.ENDPOINT, new TextNode(endpoint));
    if (!schemaExtensions.isEmpty())
    {
      ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
      for ( SchemaExtension schemaExtension : schemaExtensions )
      {
        ObjectNode extensionNode = new ObjectNode(JsonNodeFactory.instance);
        JsonHelper.addAttribute(extensionNode, AttributeNames.SCHEMA, new TextNode(schemaExtension.getSchema()));
        JsonHelper.addAttribute(extensionNode,
                                AttributeNames.REQUIRED,
                                BooleanNode.valueOf(schemaExtension.isRequired()));
        arrayNode.add(extensionNode);
      }
      JsonHelper.addAttribute(objectNode, AttributeNames.SCHEMA_EXTENSIONS, arrayNode);
    }
    return objectNode;
  }

  /**
   * @return this object as json document
   */
  public String toString()
  {
    return toJsonNode().toString();
  }

  /**
   * a schema extension representation
   */
  @Data
  public class SchemaExtension
  {

    /**
     * the resource schema reference
     */
    private String schema;

    /**
     * if the extension is a required one or not
     */
    private boolean required;

    public SchemaExtension(JsonNode jsonNode)
    {
      this.schema = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.SCHEMA)
                              .orElseThrow(() -> getException(missingAttrMessage(AttributeNames.SCHEMA)));
      this.required = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.REQUIRED, Boolean.class)
                                .orElseThrow(() -> getException(missingAttrMessage(AttributeNames.REQUIRED)));
    }
  }
}
