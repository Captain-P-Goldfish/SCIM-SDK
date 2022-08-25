package de.captaingoldfish.scim.sdk.common.schemas;

import java.time.LocalDateTime;
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

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidSchemaException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 13:24 <br>
 * <br>
 * this class will represent a SCIM schema definition
 */
@Getter
@NoArgsConstructor
public class Schema extends ResourceNode
{

  /**
   * the attribute definition for the schemas-attribute that is part of each schema
   */
  public static final SchemaAttribute SCHEMAS_ATTRIBUTE = new SchemaAttribute(JsonHelper.loadJsonDocument(ClassPathReferences.SCHEMAS_ATTRIBUTE_DEFINITION));

  /**
   * this register shall be a simple reference map that is used for scim filter expressions to find the
   * attributes fast without iterating again and again of the attributes list.<br>
   * All attributes will be added with the value output of the method {@link SchemaAttribute#getScimNodeName()}
   * in lowercase
   */
  private Map<String, SchemaAttribute> attributeRegister = new HashMap<>();

  /**
   * this list will hold references to all schema attributes that might be used to set bulkId references in a
   * bulk request. The condition to get added into this list for an attribute is: be of type
   * {@link Type#COMPLEX}, mutability of other than {@link Mutability#READ_ONLY} and define the following three
   * attributes: {@link AttributeNames.RFC7643#VALUE}, {@link AttributeNames.RFC7643#TYPE} and
   * {@link AttributeNames.RFC7643#REF} as a resource-reference
   */
  private List<SchemaAttribute> complexBulkIdCandidates = new ArrayList<>();

  /**
   * this list will hold references to all schema attributes that define itself as type 'reference' with
   * 'referenceType=resource'. Such reference fields will then also be usable for bulkId-resolving and such
   * fields must possess an additional attribute 'resourceType=${resourceName}' (name of the resourceType not
   * resource) in order to make the resource accessible by the bulk endpoint. <br>
   * The condition to get added into this list for an attribute is: be of type other than {@link Type#COMPLEX},
   * mutability of other than {@link Mutability#READ_ONLY} and by of type {@link Type#REFERENCE} and of
   * referenceType {@link de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes#RESOURCE} and define
   * the custom-attribute {@link AttributeNames.Custom#RESOURCE_TYPE_REFERENCE_NAME}
   *
   * <pre>
   *   {
   *     "name": "userId",
   *     "type": "reference",
   *     "referenceTypes": [
   *       "resource"
   *     ]
   *     "resourceType": "User"
   *   }
   * </pre>
   */
  private List<SchemaAttribute> simpleBulkIdCandidates = new ArrayList<>();

  public Schema(JsonNode jsonNode, String namePrefix)
  {
    setSchemas(JsonHelper.getSimpleAttributeArray(jsonNode, AttributeNames.RFC7643.SCHEMAS)
                         .orElse(Collections.emptyList()));
    String errorMessage = "attribute '" + AttributeNames.RFC7643.ID + "' is missing cannot resolve schema";
    setId(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.ID)
                    .orElseThrow(() -> new InvalidSchemaException(errorMessage, null, HttpStatus.INTERNAL_SERVER_ERROR,
                                                                  null)));
    setName(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.NAME).orElse(null));
    setDescription(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.DESCRIPTION).orElse(null));
    List<SchemaAttribute> attributeList = new ArrayList<>();
    String noAttributesErrorMessage = "schema with id '" + getId().orElse(null) + "' does not have attributes";
    ArrayNode attributes = JsonHelper.getArrayAttribute(jsonNode, AttributeNames.RFC7643.ATTRIBUTES)
                                     .orElseThrow(() -> new InvalidSchemaException(noAttributesErrorMessage, null,
                                                                                   HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                   null));
    Set<String> attributeNameSet = new HashSet<>();
    for ( JsonNode node : attributes )
    {
      SchemaAttribute schemaAttribute = new SchemaAttribute(this, getNonNullId(), null, node, namePrefix);
      if (attributeNameSet.contains(schemaAttribute.getName()))
      {
        String duplicateNameMessage = "the attribute with the name '" + schemaAttribute.getName() + "' was found "
                                      + "twice within the given schema declaration";
        throw new InvalidSchemaException(duplicateNameMessage, null, null, null);
      }
      attributeNameSet.add(schemaAttribute.getName());
      attributeList.add(schemaAttribute);
    }
    setAttributes(attributeList);
    initMeta(jsonNode.get(AttributeNames.RFC7643.META));
  }

  public Schema(JsonNode jsonNode)
  {
    this(jsonNode, null);
  }

  /**
   * @param jsonNode
   */
  private void initMeta(JsonNode jsonNode)
  {
    Meta meta;
    if (jsonNode == null)
    {
      LocalDateTime now = LocalDateTime.now();
      meta = Meta.builder().created(now).lastModified(now).resourceType(ResourceTypeNames.SCHEMA).build();
    }
    else
    {
      meta = JsonHelper.copyResourceToObject(jsonNode, Meta.class);
    }
    setMeta(meta);
  }

  /**
   * used explicitly for schema validation for easier code reading
   */
  public String getNonNullId()
  {
    String errorMessage = "attribute '" + AttributeNames.RFC7643.ID + "' is missing cannot resolve schema";
    return getId().orElseThrow(() -> new InvalidSchemaException(errorMessage, null, HttpStatus.INTERNAL_SERVER_ERROR,
                                                                null));
  }

  /**
   * The schema's human-readable name. When applicable, service providers MUST specify the name, e.g., "User" or
   * "Group". OPTIONAL.
   */
  public Optional<String> getName()
  {
    return getStringAttribute(AttributeNames.RFC7643.NAME);
  }

  /**
   * The schema's human-readable name. When applicable, service providers MUST specify the name, e.g., "User" or
   * "Group". OPTIONAL.
   */
  private void setName(String name)
  {
    setAttribute(AttributeNames.RFC7643.NAME, name);
  }

  /**
   * The schema's human-readable description. When applicable, service providers MUST specify the description.
   * OPTIONAL.
   */
  public Optional<String> getDescription()
  {
    return getStringAttribute(AttributeNames.RFC7643.DESCRIPTION);
  }

  /**
   * The schema's human-readable description. When applicable, service providers MUST specify the description.
   * OPTIONAL.
   */
  private void setDescription(String description)
  {
    setAttribute(AttributeNames.RFC7643.DESCRIPTION, description);
  }

  /**
   * gets the schema attributes of this schema
   */
  public List<SchemaAttribute> getAttributes()
  {
    return super.getArrayAttribute(AttributeNames.RFC7643.ATTRIBUTES, SchemaAttribute.class);
  }

  /**
   * sets the attributes into this json object
   */
  private void setAttributes(List<SchemaAttribute> attributes)
  {
    setAttribute(AttributeNames.RFC7643.ATTRIBUTES, attributes);
  }

  /**
   * adds a new attribute definition to this schema
   */
  public void addAttribute(JsonNode schemaAttribute)
  {
    List<SchemaAttribute> attributes = getAttributes();
    attributes.add(new SchemaAttribute(this, getNonNullId(), null, schemaAttribute));
    setAttributes(attributes);
  }

  /**
   * the names of the attributes of this schema inclusive the subattribute names. Subattributes will be
   * displayed in their scimNodeName notation separated with a dot e.g. "name.givenName"
   *
   * @return a set of attributes that belongs to this schema
   */
  public Set<String> getAttributeNames()
  {
    return getAttributes().stream()
                          .flatMap(attribute -> attribute.getSubAttributes().stream())
                          .map(SchemaAttribute::getScimNodeName)
                          .collect(Collectors.toSet());
  }

  /**
   * removes an attribute definition from this schema
   */
  public void removeAttribute(SchemaAttribute schemaAttribute)
  {
    List<SchemaAttribute> attributes = getAttributes();
    attributeRegister.remove(schemaAttribute.getScimNodeName());
    attributes.remove(schemaAttribute);
    setAttributes(attributes);
  }

  /**
   * gets a {@link SchemaAttribute} definition by its scimNodeName e.g. "userName" or "name.givenName". <br>
   * <br>
   * This method is for resolving filter expressions and therefore the {@code scimNodeName} values are evaluated
   * as case-insensitive. It is also allowed to use the complete schema-uri as prefix before the attributes
   * name<br>
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
    return Optional.ofNullable(attributeRegister.get(StringUtils.stripToEmpty(scimNodeName).toLowerCase()))
                   .orElseGet(() -> {
                     String nodeName = scimNodeName.replaceFirst(String.format("^%s:", getNonNullId()), "");
                     return attributeRegister.get(StringUtils.stripToEmpty(nodeName).toLowerCase());
                   });
  }

  /**
   * allows the child {@link SchemaAttribute}s to add themselves to this schema into the
   * {@link #attributeRegister}
   */
  public void addSchemaAttribute(SchemaAttribute schemaAttribute)
  {
    String scimNodeName = schemaAttribute.getScimNodeName().toLowerCase();
    if (attributeRegister.containsKey(scimNodeName))
    {
      throw new InvalidSchemaException("schema '" + getNonNullId() + "' has an duplicate attribute name: '"
                                       + scimNodeName + "'", null, null, null);
    }
    attributeRegister.put(scimNodeName, schemaAttribute);
  }

}
