package de.gold.scim.server.schemas;

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

import de.gold.scim.server.constants.AttributeNames;
import de.gold.scim.server.constants.HttpStatus;
import de.gold.scim.server.constants.enums.Mutability;
import de.gold.scim.server.constants.enums.ReferenceTypes;
import de.gold.scim.server.constants.enums.Returned;
import de.gold.scim.server.constants.enums.Type;
import de.gold.scim.server.constants.enums.Uniqueness;
import de.gold.scim.server.exceptions.InvalidSchemaException;
import de.gold.scim.server.resources.base.ScimObjectNode;
import de.gold.scim.server.utils.JsonHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;


/**
 * holds the data of an attribute definition from a schema type document
 */
@Getter
@EqualsAndHashCode(exclude = {"schema", "parent"}, callSuper = true)
public final class SchemaAttribute extends ScimObjectNode
{

  /**
   * a reference to the parent schema that holds this schema attribute
   */
  private final Schema schema;

  /**
   * is used in case of subAttributes
   */
  private final SchemaAttribute parent;

  /**
   * the uri of the resource to which this attribute belongs
   */
  private final String resourceUri;

  /**
   * an optional attribute that is used as a workaround. For example the meta attribute definition has been
   * separated from the normal resource schemata in order to prevent developers for having to define the
   * meta-attribute definition for each resource separately. But if this is done the name of the attributes is
   * not build correctly because meta definition is not a schema-definition and not an attribute definition
   * anymore. Therefore this name prefix can be used to build the attribute name correctly.<br>
   * in case of meta the attribute "created" would only get the name "created". But if this variable is set to
   * "meta" than the attribute will be accessible by the name "meta.created" instead of just "created"
   */
  private final String namePrefix;

  protected SchemaAttribute(Schema schema,
                            String resourceUri,
                            SchemaAttribute parent,
                            JsonNode jsonNode,
                            String namePrefix)
  {
    super(null);
    this.schema = schema;
    this.resourceUri = resourceUri;
    this.namePrefix = namePrefix;
    Function<String, String> errorMessageBuilder = attribute -> "could not find required attribute '" + attribute
                                                                + "' in meta-schema for attribute: "
                                                                + getScimNodeName();
    final String nameAttribute = AttributeNames.RFC7643.NAME;
    final String nameErrorMessage = errorMessageBuilder.apply(nameAttribute);
    setName(JsonHelper.getSimpleAttribute(jsonNode, nameAttribute)
                      .orElseThrow(() -> getException(nameErrorMessage, null)));
    final String typeAttribute = AttributeNames.RFC7643.TYPE;
    final String typeErrorMessage = errorMessageBuilder.apply(typeAttribute);
    final Type type = Type.getByValue(JsonHelper.getSimpleAttribute(jsonNode, typeAttribute)
                                                .orElseThrow(() -> getException(typeErrorMessage, null)));
    setType(type);
    final String descriptionAttribute = AttributeNames.RFC7643.DESCRIPTION;
    final String descriptionErrorMessage = errorMessageBuilder.apply(descriptionAttribute);
    setDescription(JsonHelper.getSimpleAttribute(jsonNode, descriptionAttribute)
                             .orElseThrow(() -> getException(descriptionErrorMessage, null)));
    setMutability(Mutability.getByValue(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.MUTABILITY)
                                                  .orElse(null)));
    setReturned(Returned.getByValue(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.RETURNED)
                                              .orElse(null)));
    setUniqueness(Uniqueness.getByValue(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.UNIQUENESS)
                                                  .orElse(Uniqueness.NONE.getValue())));
    setMultiValued(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.MULTI_VALUED, Boolean.class)
                             .orElse(false));
    setRequired(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.REQUIRED, Boolean.class).orElse(false));
    setCaseExact(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.CASE_EXACT, Boolean.class)
                           .orElse(false));
    setCanonicalValues(JsonHelper.getSimpleAttributeArray(jsonNode, AttributeNames.RFC7643.CANONICAL_VALUES)
                                 .orElse(Collections.emptyList()));
    setReferenceTypes(JsonHelper.getSimpleAttributeArray(jsonNode, AttributeNames.RFC7643.REFERENCE_TYPES)
                                .map(strings -> strings.stream()
                                                       .map(ReferenceTypes::getByValue)
                                                       .collect(Collectors.toList()))
                                .orElse(Type.REFERENCE.equals(type) ? Collections.singletonList(ReferenceTypes.EXTERNAL)
                                  : Collections.emptyList()));
    setSubAttributes(resolveSubAttributes(jsonNode));
    this.parent = parent;
    validateAttribute();
    schema.addSchemaAttribute(this);
  }


  public SchemaAttribute(Schema schema, String resourceUri, SchemaAttribute parent, JsonNode jsonNode)
  {
    this(schema, resourceUri, parent, jsonNode, null);
  }

  /**
   * @return the full resource node name e.g. User.name.givenName or Group.member.value
   */
  public String getFullResourceName()
  {
    return getParent() == null ? getResourceUri() + ":" + getScimNodeName()
      : getResourceUri() + ":" + getScimNodeName();
  }

  /**
   * @return the name scim node name of this attribute e.g. "name.givenName"
   */
  public String getScimNodeName()
  {
    return getParent() == null ? getNamePrefix() + getName() : getParent().getScimNodeName() + "." + getName();
  }

  /**
   * @see #namePrefix
   */
  public String getNamePrefix()
  {
    return namePrefix == null ? "" : namePrefix + ".";
  }

  /**
   * The attribute's name.
   */
  public String getName()
  {
    return getStringAttribute(AttributeNames.RFC7643.NAME).orElse(null);
  }

  /**
   * The attribute's name.
   */
  private void setName(String name)
  {
    setAttribute(AttributeNames.RFC7643.NAME, name);
  }

  // @formatter:off
  /**
   * The attribute's data type.  Valid values are "string",
   * "boolean", "decimal", "integer", "dateTime", "reference", and
   * "complex".  When an attribute is of type "complex", there
   * SHOULD be a corresponding schema attribute "subAttributes"
   * defined, listing the sub-attributes of the attribute.
   */
  // @formatter:on
  public Type getType()
  {
    return getStringAttribute(AttributeNames.RFC7643.TYPE).map(Type::getByValue).orElse(null);
  }

  // @formatter:off
  /**
   * The attribute's data type.  Valid values are "string",
   * "boolean", "decimal", "integer", "dateTime", "reference", and
   * "complex".  When an attribute is of type "complex", there
   * SHOULD be a corresponding schema attribute "subAttributes"
   * defined, listing the sub-attributes of the attribute.
   */
  // @formatter:on
  private void setType(Type type)
  {
    setAttribute(AttributeNames.RFC7643.TYPE, Optional.ofNullable(type).map(Type::getValue).orElse(null));
  }

  // @formatter:off
  /**
   * The attribute's human-readable description.  When
   * applicable, service providers MUST specify the description.
   */
  // @formatter:on
  public String getDescription()
  {
    return getStringAttribute(AttributeNames.RFC7643.DESCRIPTION).orElse(null);
  }

  // @formatter:off
  /**
   * The attribute's human-readable description.  When
   * applicable, service providers MUST specify the description.
   */
  // @formatter:on
  private void setDescription(String description)
  {
    setAttribute(AttributeNames.RFC7643.DESCRIPTION, description);
  }

  // @formatter:off
  /**
   * A single keyword indicating the circumstances under
   * which the value of the attribute can be (re)defined:
   *
   * readOnly  The attribute SHALL NOT be modified.
   *
   * readWrite  The attribute MAY be updated and read at any time.
   *             This is the default value.
   *
   * immutable  The attribute MAY be defined at resource creation
   *             (e.g., POST) or at record replacement via a request (e.g., a
   *             PUT).  The attribute SHALL NOT be updated.
   *
   * writeOnly  The attribute MAY be updated at any time.  Attribute
   *             values SHALL NOT be returned (e.g., because the value is a
   *             stored hash).  Note: An attribute with a mutability of
   *             "writeOnly" usually also has a returned setting of "never".
   */
  // @formatter:on
  public Mutability getMutability()
  {
    return getStringAttribute(AttributeNames.RFC7643.MUTABILITY).map(Mutability::getByValue).orElse(null);
  }

  // @formatter:off
  /**
   * A single keyword indicating the circumstances under
   * which the value of the attribute can be (re)defined:
   *
   * readOnly  The attribute SHALL NOT be modified.
   *
   * readWrite  The attribute MAY be updated and read at any time.
   *             This is the default value.
   *
   * immutable  The attribute MAY be defined at resource creation
   *             (e.g., POST) or at record replacement via a request (e.g., a
   *             PUT).  The attribute SHALL NOT be updated.
   *
   * writeOnly  The attribute MAY be updated at any time.  Attribute
   *             values SHALL NOT be returned (e.g., because the value is a
   *             stored hash).  Note: An attribute with a mutability of
   *             "writeOnly" usually also has a returned setting of "never".
   */
  // @formatter:on
  private void setMutability(Mutability mutability)
  {
    setAttribute(AttributeNames.RFC7643.MUTABILITY,
                 Optional.ofNullable(mutability).map(Mutability::getValue).orElse(null));
  }

  // @formatter:off
  /**
   * A single keyword that indicates when an attribute and
   * associated values are returned in response to a GET request or
   * in response to a PUT, POST, or PATCH request.  Valid keywords
   * are as follows:
   *
   * always   The attribute is always returned, regardless of the
   *          contents of the "attributes" parameter.  For example, "id"
   *          is always returned to identify a SCIM resource.
   *
   * never    The attribute is never returned.  This may occur because
   *          the original attribute value (e.g., a hashed value) is not
   *          retained by the service provider.  A service provider MAY
   *          allow attributes to be used in a search filter.
   *
   * default  The attribute is returned by default in all SCIM
   *          operation responses where attribute values are returned.  If
   *          the GET request "attributes" parameter is specified,
   *          attribute values are only returned if the attribute is named
   *          in the "attributes" parameter.  DEFAULT.
   *
   * request  The attribute is returned in response to any PUT,
   *          POST, or PATCH operations if the attribute was specified by
   *          the client (for example, the attribute was modified).  The
   *          attribute is returned in a SCIM query operation only if
   *          specified in the "attributes" parameter.
   */
  // @formatter:on
  public Returned getReturned()
  {
    return getStringAttribute(AttributeNames.RFC7643.RETURNED).map(Returned::getByValue).orElse(null);
  }

  // @formatter:off
  /**
   * A single keyword that indicates when an attribute and
   * associated values are returned in response to a GET request or
   * in response to a PUT, POST, or PATCH request.  Valid keywords
   * are as follows:
   *
   * always   The attribute is always returned, regardless of the
   *          contents of the "attributes" parameter.  For example, "id"
   *          is always returned to identify a SCIM resource.
   *
   * never    The attribute is never returned.  This may occur because
   *          the original attribute value (e.g., a hashed value) is not
   *          retained by the service provider.  A service provider MAY
   *          allow attributes to be used in a search filter.
   *
   * default  The attribute is returned by default in all SCIM
   *          operation responses where attribute values are returned.  If
   *          the GET request "attributes" parameter is specified,
   *          attribute values are only returned if the attribute is named
   *          in the "attributes" parameter.  DEFAULT.
   *
   * request  The attribute is returned in response to any PUT,
   *          POST, or PATCH operations if the attribute was specified by
   *          the client (for example, the attribute was modified).  The
   *          attribute is returned in a SCIM query operation only if
   *          specified in the "attributes" parameter.
   */
  // @formatter:on
  private void setReturned(Returned returned)
  {
    setAttribute(AttributeNames.RFC7643.RETURNED, Optional.ofNullable(returned).map(Returned::getValue).orElse(null));
  }

  // @formatter:off
  /**
   * A single keyword value that specifies how the service
   * provider enforces uniqueness of attribute values.  A server MAY
   * reject an invalid value based on uniqueness by returning HTTP
   * response code 400 (Bad Request).  A client MAY enforce
   * uniqueness on the client side to a greater degree than the
   * service provider enforces.  For example, a client could make a
   * value unique while the server has uniqueness of "none".  Valid
   * keywords are as follows:
   *
   * none  The values are not intended to be unique in any way.
   *       DEFAULT.
   *
   * server  The value SHOULD be unique within the context of the
   *         current SCIM endpoint (or tenancy) and MAY be globally
   *         unique (e.g., a "username", email address, or other
   *         server-generated key or counter).  No two resources on the
   *         same server SHOULD possess the same value.
   *
   * global  The value SHOULD be globally unique (e.g., an email
   *         address, a GUID, or other value).  No two resources on any
   *         server SHOULD possess the same value.
   */
  // @formatter:on
  public Uniqueness getUniqueness()
  {
    return getStringAttribute(AttributeNames.RFC7643.UNIQUENESS).map(Uniqueness::getByValue).orElse(null);
  }

  // @formatter:off
  /**
   * A single keyword value that specifies how the service
   * provider enforces uniqueness of attribute values.  A server MAY
   * reject an invalid value based on uniqueness by returning HTTP
   * response code 400 (Bad Request).  A client MAY enforce
   * uniqueness on the client side to a greater degree than the
   * service provider enforces.  For example, a client could make a
   * value unique while the server has uniqueness of "none".  Valid
   * keywords are as follows:
   *
   * none  The values are not intended to be unique in any way.
   *       DEFAULT.
   *
   * server  The value SHOULD be unique within the context of the
   *         current SCIM endpoint (or tenancy) and MAY be globally
   *         unique (e.g., a "username", email address, or other
   *         server-generated key or counter).  No two resources on the
   *         same server SHOULD possess the same value.
   *
   * global  The value SHOULD be globally unique (e.g., an email
   *         address, a GUID, or other value).  No two resources on any
   *         server SHOULD possess the same value.
   */
  // @formatter:on
  private void setUniqueness(Uniqueness uniqueness)
  {
    setAttribute(AttributeNames.RFC7643.UNIQUENESS,
                 Optional.ofNullable(uniqueness).map(Uniqueness::getValue).orElse(null));
  }

  /**
   * A Boolean value indicating the attribute's plurality.
   */
  public boolean isMultiValued()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.MULTI_VALUED).orElse(false);
  }

  /**
   * A Boolean value indicating the attribute's plurality.
   */
  private void setMultiValued(boolean multiValued)
  {
    setAttribute(AttributeNames.RFC7643.MULTI_VALUED, multiValued);
  }

  // @formatter:off
  /**
   * A Boolean value that specifies whether or not the
   * attribute is required.
   */
  // @formatter:on
  public boolean isRequired()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.REQUIRED).orElse(false);
  }

  // @formatter:off
  /**
   * A Boolean value that specifies whether or not the
   * attribute is required.
   */
  // @formatter:on
  private void setRequired(boolean required)
  {
    setAttribute(AttributeNames.RFC7643.REQUIRED, required);
  }

  // @formatter:off
  /**
   * A Boolean value that specifies whether or not a string
   * attribute is case sensitive.  The server SHALL use case
   * sensitivity when evaluating filters.  For attributes that are
   * case exact, the server SHALL preserve case for any value
   * submitted.  If the attribute is case insensitive, the server
   * MAY alter case for a submitted value.  Case sensitivity also
   * impacts how attribute values MAY be compared against filter
   * values (see Section 3.4.2.2 of [RFC7644]).
   */
  // @formatter:on
  public boolean isCaseExact()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.CASE_EXACT).orElse(false);
  }

  // @formatter:off
  /**
   * A Boolean value that specifies whether or not a string
   * attribute is case sensitive.  The server SHALL use case
   * sensitivity when evaluating filters.  For attributes that are
   * case exact, the server SHALL preserve case for any value
   * submitted.  If the attribute is case insensitive, the server
   * MAY alter case for a submitted value.  Case sensitivity also
   * impacts how attribute values MAY be compared against filter
   * values (see Section 3.4.2.2 of [RFC7644]).
   */
  // @formatter:on
  private void setCaseExact(boolean caseExact)
  {
    setAttribute(AttributeNames.RFC7643.CASE_EXACT, caseExact);
  }

// @formatter:off
  /**
   * A collection of suggested canonical values that
   * MAY be used (e.g., "work" and "home").  In some cases, service
   * providers MAY choose to ignore unsupported values.  OPTIONAL.
   */
  // @formatter:on
  public List<String> getCanonicalValues()
  {
    return getSimpleArrayAttribute(AttributeNames.RFC7643.CANONICAL_VALUES);
  }

  // @formatter:off
  /**
   * A collection of suggested canonical values that
   * MAY be used (e.g., "work" and "home").  In some cases, service
   * providers MAY choose to ignore unsupported values.  OPTIONAL.
   */
  // @formatter:on
  private void setCanonicalValues(List<String> canonicalValues)
  {
    setStringAttributeList(AttributeNames.RFC7643.CANONICAL_VALUES, canonicalValues);
  }

 // @formatter:off
  /**
   * A multi-valued array of JSON strings that indicate
   * the SCIM resource types that may be referenced.  Valid values
   * are as follows:
   *
   * +  A SCIM resource type (e.g., "User" or "Group"),
   *
   * +  "external" - indicating that the resource is an external
   *                 resource (e.g., a photo), or
   *
   * +  "uri" - indicating that the reference is to a service
   *            endpoint or an identifier (e.g., a schema URN).
   *
   * This attribute is only applicable for attributes that are of
   * type "reference" (Section 2.3.7).
   */
  // @formatter:on
  public List<ReferenceTypes> getReferenceTypes()
  {
    return getSimpleArrayAttribute(AttributeNames.RFC7643.REFERENCE_TYPES).stream()
                                                                          .map(ReferenceTypes::getByValue)
                                                                          .collect(Collectors.toList());
  }

  // @formatter:off
  /**
   * A multi-valued array of JSON strings that indicate
   * the SCIM resource types that may be referenced.  Valid values
   * are as follows:
   *
   * +  A SCIM resource type (e.g., "User" or "Group"),
   *
   * +  "external" - indicating that the resource is an external
   *                 resource (e.g., a photo), or
   *
   * +  "uri" - indicating that the reference is to a service
   *            endpoint or an identifier (e.g., a schema URN).
   *
   * This attribute is only applicable for attributes that are of
   * type "reference" (Section 2.3.7).
   */
  // @formatter:on
  private void setReferenceTypes(List<ReferenceTypes> referenceTypes)
  {
    setStringAttributeList(AttributeNames.RFC7643.REFERENCE_TYPES,
                           referenceTypes.stream().map(ReferenceTypes::getValue).collect(Collectors.toList()));
  }

  // @formatter:off
  /**
   * When an attribute is of type "complex",
   * "subAttributes" defines a set of sub-attributes.
   * "subAttributes" has the same schema sub-attributes as
   * "attributes".
   */
  // @formatter:on
  public List<SchemaAttribute> getSubAttributes()
  {
    return getArrayAttribute(AttributeNames.RFC7643.SUB_ATTRIBUTES, SchemaAttribute.class);
  }

  // @formatter:off
  /**
   * When an attribute is of type "complex",
   * "subAttributes" defines a set of sub-attributes.
   * "subAttributes" has the same schema sub-attributes as
   * "attributes".
   */
  // @formatter:on
  private void setSubAttributes(List<SchemaAttribute> subAttributes)
  {
    setAttribute(AttributeNames.RFC7643.SUB_ATTRIBUTES, subAttributes);
  }

  /**
   * tries to parse the sub attributes of complex type definition
   *
   * @param jsonNode the complex type definition node
   * @return a list of the aub attributes of this complex node
   */
  private List<SchemaAttribute> resolveSubAttributes(JsonNode jsonNode)
  {
    if (!Type.COMPLEX.equals(this.getType()))
    {
      return Collections.emptyList();
    }
    List<SchemaAttribute> schemaAttributeList = new ArrayList<>();
    final String subAttributeName = AttributeNames.RFC7643.SUB_ATTRIBUTES;
    String errorMessage = "missing attribute '" + subAttributeName + "' on '" + getType() + "'-attribute with name: "
                          + getName();
    ArrayNode subAttributesArray = JsonHelper.getArrayAttribute(jsonNode, subAttributeName)
                                             .orElseThrow(() -> getException(errorMessage, null));
    Set<String> attributeNameSet = new HashSet<>();
    for ( JsonNode subAttribute : subAttributesArray )
    {
      SchemaAttribute schemaAttribute = new SchemaAttribute(schema, resourceUri, this, subAttribute, namePrefix);
      if (attributeNameSet.contains(schemaAttribute.getScimNodeName()))
      {
        String duplicateNameMessage = "the attribute with the name '" + schemaAttribute.getFullResourceName()
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
    final Mutability mutability = getMutability();
    final Returned returned = getReturned();
    if (Mutability.READ_ONLY.equals(mutability) && Returned.NEVER.equals(returned))
    {
      String errorMessage = "the attribute with the name '" + getFullResourceName() + "' has an invalid declaration. "
                            + "mutability 'readOnly' and returned 'never' are an illegal combination. The client is "
                            + "not able to write to the given attribute and the server will never return it.";
      throw getException(errorMessage, null);
    }
    else if (Mutability.WRITE_ONLY.equals(mutability) && !Returned.NEVER.equals(returned))
    {
      String errorMessage = "the attribute with the name '" + getFullResourceName() + "' has an invalid declaration. "
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
    return new InvalidSchemaException(errorMessage, cause, HttpStatus.INTERNAL_SERVER_ERROR, null);
  }
}
