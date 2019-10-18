package de.gold.scim.schemas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.exceptions.InvalidSchemaException;
import de.gold.scim.resources.base.ScimObjectNode;
import de.gold.scim.utils.JsonHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 13:24 <br>
 * <br>
 * this class will represent a SCIM schema definition
 */
@Getter
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

  /**
   * this register shall be a simple reference map that is used for scim filter expressions to find the
   * attributes fast without iterating again and again of the attributes list.<br>
   * All attributes will be added with the value output of the method {@link SchemaAttribute#getScimNodeName()}
   * in lowercase
   */
  private Map<String, SchemaAttribute> attributeRegister = new HashMap<>();

  protected Schema(JsonNode jsonNode, String namePrefix)
  {

    this.schemas = JsonHelper.getSimpleAttributeArray(jsonNode, AttributeNames.RFC7643.SCHEMAS)
                             .orElse(Collections.emptyList());
    String errorMessage = "attribute '" + AttributeNames.RFC7643.ID + "' is missing cannot resolve schema";
    this.id = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.ID)
                        .orElseThrow(() -> new InvalidSchemaException(errorMessage, null,
                                                                      HttpStatus.SC_INTERNAL_SERVER_ERROR, null));
    this.name = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.NAME).orElse(null);
    this.description = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.DESCRIPTION).orElse(null);
    this.attributes = new ArrayList<>();
    String noAttributesErrorMessage = "schema with id '" + id + "' does not have attributes";
    ArrayNode attributes = JsonHelper.getArrayAttribute(jsonNode, AttributeNames.RFC7643.ATTRIBUTES)
                                     .orElseThrow(() -> new InvalidSchemaException(noAttributesErrorMessage, null,
                                                                                   HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                                                                   null));
    Set<String> attributeNameSet = new HashSet<>();
    for ( JsonNode node : attributes )
    {
      SchemaAttribute schemaAttribute = new SchemaAttribute(this, this.id, null, node, namePrefix);
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

  public Schema(JsonNode jsonNode)
  {
    this(jsonNode, null);
  }

  /**
   * @see #attributes
   */
  public List<String> getSchemas()
  {
    return Collections.unmodifiableList(schemas);
  }

  /**
   * @see #attributes
   */
  public List<SchemaAttribute> getAttributes()
  {
    return Collections.unmodifiableList(attributes);
  }

  /**
   * gets a {@link SchemaAttribute} definition by its scimNodeName e.g. "userName" or "name.givenName". <br>
   * This method is for resolving filter expressions and therefore the {@code scimNodeName} values are evaluated
   * as case insensitive.<br>
   *
   * <pre>
   *    Attribute names and attribute operators used in filters are case
   *    insensitive.  For example, the following two expressions will
   *    evaluate to the same logical value:
   *
   *    filter=userName Eq "john"
   *
   *    filter=Username eq "john"
   * </pre>
   */
  public SchemaAttribute getSchemaAttribute(String scimNodeName)
  {
    return attributeRegister.get(StringUtils.stripToEmpty(scimNodeName).toLowerCase());
  }

  /**
   * allows the child {@link SchemaAttribute}s to add themselves to this schema
   */
  protected void addSchemaAttribute(SchemaAttribute schemaAttribute)
  {
    String scimNodeName = schemaAttribute.getScimNodeName().toLowerCase();
    if (attributeRegister.containsKey(scimNodeName))
    {
      throw new InvalidSchemaException("schema '" + id + "' has an duplicate attribute name: '" + scimNodeName + "'",
                                       null, null, null);
    }
    attributeRegister.put(scimNodeName, schemaAttribute);
  }

  /**
   * @return the schema as json document
   */
  public JsonNode toJsonNode()
  {
    ScimObjectNode objectNode = new ScimObjectNode(null);
    List<JsonNode> schemas = getSchemas().stream().map(TextNode::new).collect(Collectors.toList());
    JsonHelper.addAttribute(objectNode,
                            AttributeNames.RFC7643.SCHEMAS,
                            new ArrayNode(JsonNodeFactory.instance, schemas));
    JsonHelper.addAttribute(objectNode, AttributeNames.RFC7643.ID, new TextNode(id));
    Optional.ofNullable(getName())
            .ifPresent(s -> JsonHelper.addAttribute(objectNode, AttributeNames.RFC7643.NAME, new TextNode(s)));
    Optional.ofNullable(getDescription())
            .ifPresent(s -> JsonHelper.addAttribute(objectNode, AttributeNames.RFC7643.DESCRIPTION, new TextNode(s)));
    List<JsonNode> attributes = getAttributes().stream().map(SchemaAttribute::toJsonNode).collect(Collectors.toList());
    JsonHelper.addAttribute(objectNode,
                            AttributeNames.RFC7643.ATTRIBUTES,
                            new ArrayNode(JsonNodeFactory.instance, attributes));
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
