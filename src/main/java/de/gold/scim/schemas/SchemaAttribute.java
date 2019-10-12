package de.gold.scim.schemas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.enums.Mutability;
import de.gold.scim.constants.enums.ReferenceTypes;
import de.gold.scim.constants.enums.Returned;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.constants.enums.Uniqueness;
import de.gold.scim.exceptions.InvalidSchemaException;
import de.gold.scim.utils.JsonHelper;
import lombok.Getter;


/**
 * holds the data of an attribute definition from a schema type document
 */
@Getter
public class SchemaAttribute
{

  /**
   * is used in case of subAttributes
   */
  private SchemaAttribute parent;

  /**
   * the name of the attribute
   */
  private String name;

  private Type type;

  private String description;

  private Mutability mutability;

  private Returned returned;

  private Uniqueness uniqueness;

  private boolean multiValued;

  private boolean required;

  /**
   * if the value of the attribute is case insensitive or not
   */
  private boolean caseExact;

  /**
   * the canonical values do represent an list of valid values that may be entered into the field within the
   * document. If no canonical values are set any value is valid in the field
   */
  private List<String> canonicalValues;

  /**
   * a list of valid reference types that may be valid within a reference type field
   */
  private List<ReferenceTypes> referenceTypes;

  /**
   * contains the sub attributes if this attribute is a complex type
   */
  private List<SchemaAttribute> subAttributes;

  public SchemaAttribute(SchemaAttribute parent, JsonNode jsonNode)
  {
    Function<String, String> errorMessageBuilder = attribute -> "could not find required attribute '" + attribute
                                                                + "' in meta-schema";
    final String nameAttribute = AttributeNames.NAME;
    final String nameErrorMessage = errorMessageBuilder.apply(nameAttribute);
    this.name = JsonHelper.getSimpleAttribute(jsonNode, nameAttribute)
                          .orElseThrow(() -> getException(nameErrorMessage, null));
    final String typeAttribute = AttributeNames.TYPE;
    final String typeErrorMessage = errorMessageBuilder.apply(typeAttribute);
    this.type = Type.getByValue(JsonHelper.getSimpleAttribute(jsonNode, typeAttribute)
                                          .orElseThrow(() -> getException(typeErrorMessage, null)));
    final String descriptionAttribute = AttributeNames.DESCRIPTION;
    final String descriptionErrorMessage = errorMessageBuilder.apply(descriptionAttribute);
    this.description = JsonHelper.getSimpleAttribute(jsonNode, descriptionAttribute)
                                 .orElseThrow(() -> getException(descriptionErrorMessage, null));
    this.mutability = Mutability.getByValue(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.MUTABILITY)
                                                      .orElse(null));
    this.returned = Returned.getByValue(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RETURNED).orElse(null));
    this.uniqueness = Uniqueness.getByValue(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.UNIQUENESS)
                                                      .orElse(Uniqueness.NONE.getValue()));
    this.multiValued = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.MULTI_VALUED, Boolean.class)
                                 .orElse(false);
    this.required = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.REQUIRED, Boolean.class).orElse(false);
    this.caseExact = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.CASE_EXACT, Boolean.class).orElse(false);
    this.canonicalValues = JsonHelper.getSimpleAttributeArray(jsonNode, AttributeNames.CANONICAL_VALUES)
                                     .orElse(Collections.emptyList());
    this.referenceTypes = JsonHelper.getSimpleAttributeArray(jsonNode, AttributeNames.REFERENCE_TYPES)
                                    .map(strings -> strings.stream()
                                                           .map(ReferenceTypes::getByValue)
                                                           .collect(Collectors.toList()))
                                    .orElse(Type.REFERENCE.equals(type)
                                      ? Collections.singletonList(ReferenceTypes.EXTERNAL) : Collections.emptyList());
    this.subAttributes = resolveSubAttributes(jsonNode);
    this.parent = parent;
    validateAttribute();
  }

  /**
   * @return the name scim node name of this attribute e.g. "name.givenName"
   */
  public String getScimNodeName()
  {
    return getParent() == null ? getName() : getParent().getScimNodeName() + "." + getName();
  }

  /**
   * tries to parse the sub attributes of complex type definition
   *
   * @param jsonNode the complex type definition node
   * @return a list of the aub attributes of this complex node
   */
  private List<SchemaAttribute> resolveSubAttributes(JsonNode jsonNode)
  {
    if (!Type.COMPLEX.equals(this.type))
    {
      return Collections.emptyList();
    }
    List<SchemaAttribute> schemaAttributeList = new ArrayList<>();
    final String subAttributeName = AttributeNames.SUB_ATTRIBUTES;
    String errorMessage = "missing attribute '" + subAttributeName + "' on '" + type + "'-attribute";
    ArrayNode subAttributesArray = JsonHelper.getArrayAttribute(jsonNode, subAttributeName)
                                             .orElseThrow(() -> getException(errorMessage, null));
    Set<String> attributeNameSet = new HashSet<>();
    for ( JsonNode subAttribute : subAttributesArray )
    {
      SchemaAttribute schemaAttribute = new SchemaAttribute(this, subAttribute);
      if (attributeNameSet.contains(schemaAttribute.getScimNodeName()))
      {
        String duplicateNameMessage = "the attribute with the name '" + schemaAttribute.getScimNodeName()
                                      + "' was found twice within the given schema declaration";
        throw new InvalidSchemaException(duplicateNameMessage, null, null, null);
      }
      attributeNameSet.add(schemaAttribute.getScimNodeName());
      schemaAttributeList.add(schemaAttribute);
    }
    return schemaAttributeList;
  }

  /**
   * this method will decide if the attribute definition makes sense. Some attribute combinations are simply
   * senseless and might cause confusable situations that would not be easily identifiable.<br>
   * the known senseless attribute combinations are the following: <br>
   *
   * <pre>
   *     {
   *       "name": "senseless",
   *       "type": "string",
   *       "description": "senseless declaration: client cannot write to it and server cannot return it",
   *       "mutability": "readOnly",
   *       "returned": "never"
   *     },
   *     {
   *       "name": "senseless",
   *       "type": "string",
   *       "description": "senseless declaration: writeOnly must have a returned value of 'never'.",
   *       "mutability": "writeOnly",
   *       "returned": "always"
   *     }
   * </pre>
   *
   * this combination shows 3 problems but the following method will only handle two of theses problems: <br>
   * <ul>
   * <li><b>mutability:</b> readOnly</li>
   * <li><b>returned:</b> never</li>
   * <li>the client can never write to this attribute and the server will never return it. The server may use
   * this attribute but it simply makes no sense to declare it within the schema</li>
   * <li>----------------------------</li>
   * <li>and</li>
   * <li>----------------------------</li>
   * <li><b>mutability:</b> writeOnly</li>
   * <li><b>returned:</b> something else than "never"</li>
   * <li>This is also defined in RFC7643 chapter 7: <br>
   * <b>writeOnly</b> The attribute MAY be updated at any time. Attribute values SHALL NOT be returned (e.g.,
   * because the value is a stored hash). Note: An attribute with a mutability of "writeOnly" usually also has a
   * returned setting of "never"</li>
   * </ul>
   * the last problem is that the an attribute attribute with the same name was declared twice. This problem
   * will be handled in another method
   */
  private void validateAttribute()
  {
    if (Mutability.READ_ONLY.equals(mutability) && Returned.NEVER.equals(returned))
    {
      String errorMessage = "the attribute with the name '" + getScimNodeName() + "' has an invalid declaration. "
                            + "mutability 'readOnly' and returned 'never' are an illegal in combination. The client is "
                            + "not able to write to the given attribute and the server will never return it.";
      throw getException(errorMessage, null);
    }
    else if (Mutability.WRITE_ONLY.equals(mutability) && !Returned.NEVER.equals(returned))
    {
      String errorMessage = "the attribute with the name '" + getScimNodeName() + "' has an invalid declaration. "
                            + "mutability 'writeOnly' must have a returned value of 'never' are an illegal in "
                            + "combination. The client should only write to this attribute but should never have it "
                            + "returned. The mutability writeOnly makes only sense for sensitive application data "
                            + "like passwords or other secrets.";
      throw getException(errorMessage, null);
    }
  }

  /**
   * builds an exception
   *
   * @param errorMessage the error message of the exception
   * @param cause the cause of this exception, may be null
   * @return a new exception instance
   */
  private InvalidSchemaException getException(String errorMessage, Exception cause)
  {
    return new InvalidSchemaException(errorMessage, cause, HttpStatus.SC_INTERNAL_SERVER_ERROR, null);
  }

  /**
   * @return the attribute as json document
   */
  public JsonNode toJsonNode()
  {
    ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);

    JsonHelper.addAttribute(objectNode, AttributeNames.NAME, new TextNode(name));
    JsonHelper.addAttribute(objectNode, AttributeNames.TYPE, new TextNode(type.getValue()));
    Optional.ofNullable(description)
            .ifPresent(s -> JsonHelper.addAttribute(objectNode,
                                                    AttributeNames.DESCRIPTION,
                                                    new TextNode(this.description)));
    JsonHelper.addAttribute(objectNode, AttributeNames.MUTABILITY, new TextNode(mutability.getValue()));
    JsonHelper.addAttribute(objectNode, AttributeNames.RETURNED, new TextNode(returned.getValue()));
    JsonHelper.addAttribute(objectNode, AttributeNames.UNIQUENESS, new TextNode(uniqueness.getValue()));
    JsonHelper.addAttribute(objectNode, AttributeNames.MULTI_VALUED, BooleanNode.valueOf(multiValued));
    JsonHelper.addAttribute(objectNode, AttributeNames.REQUIRED, BooleanNode.valueOf(required));
    JsonHelper.addAttribute(objectNode, AttributeNames.CASE_EXACT, BooleanNode.valueOf(caseExact));
    List<JsonNode> canonValues = canonicalValues.stream().map(TextNode::new).collect(Collectors.toList());
    if (!canonValues.isEmpty())
    {
      JsonHelper.addAttribute(objectNode,
                              AttributeNames.CANONICAL_VALUES,
                              new ArrayNode(JsonNodeFactory.instance, canonValues));
    }
    List<JsonNode> referType = referenceTypes.stream()
                                             .map(ReferenceTypes::getValue)
                                             .map(TextNode::new)
                                             .collect(Collectors.toList());
    if (!referType.isEmpty())
    {
      JsonHelper.addAttribute(objectNode,
                              AttributeNames.REFERENCE_TYPES,
                              new ArrayNode(JsonNodeFactory.instance, referType));
    }
    List<JsonNode> subAttr = subAttributes.stream()
                                          .map(SchemaAttribute::toString)
                                          .map(JsonHelper::readJsonDocument)
                                          .collect(Collectors.toList());
    if (!subAttr.isEmpty())
    {
      JsonHelper.addAttribute(objectNode,
                              AttributeNames.SUB_ATTRIBUTES,
                              new ArrayNode(JsonNodeFactory.instance, subAttr));
    }
    return objectNode;
  }

  /**
   * @return the attribute as json document
   */
  @Override
  public String toString()
  {
    return toJsonNode().toString();
  }
}
