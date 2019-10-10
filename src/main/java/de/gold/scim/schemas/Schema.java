package de.gold.scim.schemas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.exceptions.InvalidSchemaException;
import de.gold.scim.utils.HttpStatus;
import de.gold.scim.utils.JsonHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 13:24 <br>
 * <br>
 * this class will represent a SCIM schema definition
 */
@Getter
@Setter
@EqualsAndHashCode
public class Schema
{

  /**
   * a list of defined schemas that will describe this schema resource. If the collection is empty this will be
   * a meta schema on top level
   */
  private final List<String> schemas;

  /**
   * the id of the schema
   */
  private final String id;

  /**
   * the name of the schema
   */
  private String name;

  /**
   * the description of the schema
   */
  private String description;

  /**
   * the list of attributes defined by this schema
   */
  private List<SchemaAttribute> attributes;

  public Schema(JsonNode jsonNode)
  {
    this.schemas = JsonHelper.getSimpleAttributeArray(jsonNode, AttributeNames.SCHEMAS).orElse(Collections.emptyList());
    String errorMessage = "attribute '" + AttributeNames.ID + "' is missing cannot resolve schema";
    this.id = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.ID)
                        .orElseThrow(() -> new InvalidSchemaException(errorMessage, null,
                                                                      HttpStatus.SC_INTERNAL_SERVER_ERROR, null));
    this.name = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.NAME).orElse(null);
    this.description = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.DESCRIPTION).orElse(null);
    this.attributes = new ArrayList<>();
    String noAttributesErrorMessage = "schema with id '" + id + "' does not have attributes";
    ArrayNode attributes = JsonHelper.getArrayAttribute(jsonNode, AttributeNames.ATTRIBUTES)
                                     .orElseThrow(() -> new InvalidSchemaException(noAttributesErrorMessage, null,
                                                                                   HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                                                                   null));
    Set<String> attributeNameSet = new HashSet<>();
    for ( JsonNode node : attributes )
    {
      SchemaAttribute schemaAttribute = new SchemaAttribute(null, node);
      if (attributeNameSet.contains(schemaAttribute.getName()))
      {
        String duplicateNameMessage = "the attribute with the name '" + schemaAttribute.getName() + "' was found "
                                      + "twice within the given schema declaration";
        throw new InvalidSchemaException(duplicateNameMessage, null, null, null);
      }
      attributeNameSet.add(schemaAttribute.getName());
      this.attributes.add(schemaAttribute);
    }
  }

  /**
   * @return the schema as json document
   */
  public JsonNode toJsonNode()
  {
    ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
    List<JsonNode> schemas = getSchemas().stream().map(TextNode::new).collect(Collectors.toList());
    JsonHelper.addAttribute(objectNode, AttributeNames.SCHEMAS, new ArrayNode(JsonNodeFactory.instance, schemas));
    JsonHelper.addAttribute(objectNode, AttributeNames.ID, new TextNode(id));
    Optional.ofNullable(getName())
            .ifPresent(s -> JsonHelper.addAttribute(objectNode, AttributeNames.NAME, new TextNode(s)));
    Optional.ofNullable(getDescription())
            .ifPresent(s -> JsonHelper.addAttribute(objectNode, AttributeNames.DESCRIPTION, new TextNode(s)));
    List<JsonNode> attributes = getAttributes().stream().map(SchemaAttribute::toJsonNode).collect(Collectors.toList());
    JsonHelper.addAttribute(objectNode, AttributeNames.ATTRIBUTES, new ArrayNode(JsonNodeFactory.instance, attributes));
    return objectNode;
  }

  /**
   * @return the schema as json document
   */
  @Override
  public String toString()
  {
    return toJsonNode().toString();
  }
}
