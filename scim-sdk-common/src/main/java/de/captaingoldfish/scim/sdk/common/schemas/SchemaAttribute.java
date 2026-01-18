package de.captaingoldfish.scim.sdk.common.schemas;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidSchemaException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * holds the data of an attribute definition from a schema type document
 */
@Slf4j
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
   * this map contains the subAttributes of this attribute definition
   */
  private final Map<String, SchemaAttribute> subAttributes = new HashMap<>();

  /**
   * the uri of the resource to which this attribute belongs e.g.: urn:ietf:params:scim:schemas:core:2.0:User
   */
  private final String resourceUri;

  /**
   * an optional attribute that is used as a workaround. For example the meta attribute definition has been
   * separated from the normal resource schemata in order to prevent developers for having to define the
   * meta-attribute definition for each resource separately. But if this is done the name of the attributes is
   * not build correctly because meta definition is not a schema-definition and not an attribute definition
   * anymore. Therefore, this name prefix can be used to build the attribute name correctly.<br>
   * in case of meta the attribute "created" would only get the name "created". But if this variable is set to
   * "meta" than the attribute will be accessible by the name "meta.created" instead of just "created"
   */
  private final String namePrefix;

  /**
   * the compiled pattern attribute that is stored as a member to prevent the pattern from getting compiled
   * again and again
   */
  private Pattern pattern;

  /**
   * this constructor is necessary for the SCIM-SDK-client. It is used if the meta-config-data of a SCIM
   * provider is loaded and evaluated
   */
  public SchemaAttribute()
  {
    this.schema = null;
    this.parent = null;
    this.resourceUri = null;
    this.namePrefix = null;
  }

  /**
   * a constructor used for unit tests only to create schema-attribute instances for testing
   */
  public SchemaAttribute(SchemaAttribute parent, String resourceUri, String namePrefix)
  {
    this.schema = null;
    this.parent = parent;
    this.resourceUri = resourceUri;
    this.namePrefix = namePrefix;
  }

  protected SchemaAttribute(JsonNode attributeDefinition)
  {
    this(null, null, null, attributeDefinition, null);
  }

  protected SchemaAttribute(Schema schema,
                            String resourceUri,
                            SchemaAttribute parent,
                            JsonNode jsonNode,
                            String namePrefix)
  {
    super(null);
    this.schema = schema;
    this.resourceUri = resourceUri;
    this.parent = parent;
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
    setResourceTypeReferenceName(JsonHelper.getSimpleAttribute(jsonNode,
                                                               AttributeNames.Custom.RESOURCE_TYPE_REFERENCE_NAME)
                                           .orElse(null));
    setValidationAttributes(jsonNode);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.DEFAULT_VALUE, String.class)
              .ifPresent(this::setDefaultValue);
    final List<SchemaAttribute> subAttributes = resolveSubAttributes(jsonNode);
    setSubAttributes(subAttributes);
    validateAttribute();
    Optional.ofNullable(schema).ifPresent(schemaDefinition -> schemaDefinition.addSchemaAttribute(this));
    checkForDirectResourceReference();

    if (schema != null && isMultivaluedComplexAttribute())
    {
      schema.getMultivaluedComplexRegister().put(this, subAttributes);
    }
    else if (schema != null && isComplexAttribute())
    {
      schema.getComplexRegister().put(this, subAttributes);
    }
    if (schema != null && isReadOnly())
    {
      schema.getReadOnlyAttributeRegister().add(this);
    }
    else if (schema != null && isImmutable())
    {
      schema.getImmutableAttributeRegister().add(this);
    }
  }

  public SchemaAttribute(Schema schema, String resourceUri, SchemaAttribute parent, JsonNode jsonNode)
  {
    this(schema, resourceUri, parent, jsonNode, null);
  }

  /**
   * gets the attribute with the given name from this schemaAttribute
   *
   * @param attributeName the simple name of the sub-attribute e.g. "value" or "primary"
   * @return the attribute if it does exist as child of this attribute
   */
  public SchemaAttribute getSubAttribute(String attributeName)
  {
    SchemaAttribute schemaAttribute = subAttributes.get(attributeName.toLowerCase());
    if (schemaAttribute == null)
    {
      // maybe the fully qualified name of the attribute was used. In this case we will try to retrieve it from the
      // schema
      schemaAttribute = schema.getSchemaAttribute(attributeName);
      if (schemaAttribute != null)
      {
        if (schemaAttribute.getParent() != null && schemaAttribute.getParent() == this)
        {
          return schemaAttribute;
        }
      }
      return null;
    }
    else
    {
      return schemaAttribute;
    }
  }

  /**
   * @return true if this attribute is of type complex
   */
  public boolean isComplexAttribute()
  {
    return parent == null && Type.COMPLEX.equals(getType());
  }

  /**
   * @return true if this attribute is of type complex and multivalued
   */
  public boolean isMultivaluedComplexAttribute()
  {
    return parent == null && Type.COMPLEX.equals(getType()) && isMultiValued();
  }

  /**
   * @return true if this attribute has a parent
   */
  public boolean isChildOfComplexAttribute()
  {
    return parent != null;
  }

  /**
   * @return true if this attribute has a parent and the parent is multivalued
   */
  public boolean isChildOfMultivaluedComplexAttribute()
  {
    return isChildOfComplexAttribute() && parent.isMultiValued();
  }

  /**
   * @return true if this is a readOnly attribute
   */
  public boolean isReadOnly()
  {
    return getMutability().equals(Mutability.READ_ONLY);
  }

  /**
   * @return true if this is an immutable attribute
   */
  public boolean isImmutable()
  {
    return getMutability().equals(Mutability.IMMUTABLE);
  }

  /**
   * @return true if this is a read-write attribute
   */
  public boolean isReadWrite()
  {
    return getMutability().equals(Mutability.READ_WRITE);
  }

  /**
   * @return true if this is a writeOnly attribute
   */
  public boolean isWriteOnly()
  {
    return getMutability().equals(Mutability.WRITE_ONLY);
  }

  /**
   * this method will verify if this attribute is a direct resource-reference. Which means it must look like
   * this:
   *
   * <pre>
   *         {
   *           "name": "userId",
   *           "type": "reference",
   *           "mutability": "readWrite | writeOnly"
   *           "referenceTypes": [
   *             "resource"
   *           ]
   *           "resourceType": "User"
   *         }
   * </pre>
   *
   * Attributes created this way will allow to use bulkId-references and can be utilized with the
   * {@link BulkConfig#isSupportBulkGet()}-feature.<br>
   * <br>
   * the attribute "resourceType" is mandatory in this case, and it must point to an existing resource-type
   */
  private void checkForDirectResourceReference()
  {
    // checks if the schema attribute is a direct resource-reference
    {
      if (!Type.REFERENCE.equals(getType()))
      {
        return;
      }
      List<ReferenceTypes> referenceTypes = getReferenceTypes();
      if (referenceTypes.size() != 1 || !referenceTypes.contains(ReferenceTypes.RESOURCE))
      {
        return;
      }
      if (Mutability.READ_ONLY.equals(getMutability()))
      {
        return;
      }
      if (!getResourceTypeReferenceName().isPresent())
      {
        // it is not possible to validate the resource type reference name because we have no access to the
        // resource-types from here. So we need to verify the existence of the resource-type name during
        // schema-registration within the ResourceTypeFactory
        return;
      }
    }
    schema.getSimpleBulkIdCandidates().add(this);
  }

  /**
   * this method will check for present validation attributes as "multipleOf", "minimum" etc. and will validate
   * them and set them if applicable
   *
   * @param jsonNode the current schema attribute that might hold validation attributes
   */
  private void setValidationAttributes(JsonNode jsonNode)
  {
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.MINIMUM, Double.class).ifPresent(this::setMinimum);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.MAXIMUM, Double.class).ifPresent(this::setMaximum);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.MULTIPLE_OF, Double.class)
              .ifPresent(this::setMultipleOf);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.MIN_LENGTH, Long.class).ifPresent(this::setMinLength);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.MAX_LENGTH, Long.class).ifPresent(this::setMaxLength);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.PATTERN).ifPresent(this::setPattern);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.MIN_ITEMS, Integer.class)
              .ifPresent(this::setMinItems);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.MAX_ITEMS, Integer.class)
              .ifPresent(this::setMaxItems);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.NOT_BEFORE, String.class)
              .ifPresent(this::setNotBefore);
    JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.Custom.NOT_AFTER, String.class).ifPresent(this::setNotAfter);
  }

  /**
   * @return if this attribute is a complex bulk candidate e.g. enterprise.user.manger. A complex bulk-candidate
   *         is a complex attribute that contains a value a type and a $ref attribute. The $ref attribute must
   *         be of type "reference" and "referenceType=resource"
   */
  public boolean isComplexBulkCandidate()
  {
    return schema.getComplexBulkIdCandidates().contains(this);
  }

  /**
   * if this attribute is a simple bulk candidate. This is a custom feature not defined in the SCIM
   * specification. A simple bulk candidate must fulfill the following conditions:
   * <ol>
   * <li>be of type "reference"</li>
   * <li>have as one and only "referenceType" the value "resource"</li>
   * <li>define the custom attribute "resourceType" that references the name of resource-type.json file</li>
   * </ol>
   *
   * <pre>
   *         {
   *           "name": "userId",
   *           "type": "reference",
   *           "referenceTypes": [
   *             "resource"
   *           ]
   *           "resourceType": "User"
   *         }
   * </pre>
   */
  public boolean isSimpleValueBulkCandidate()
  {
    return schema.getSimpleBulkIdCandidates().contains(this);
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
  public void setName(String name)
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
  public void setType(Type type)
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
  public void setDescription(String description)
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
  public void setMutability(Mutability mutability)
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
  public void setReturned(Returned returned)
  {
    setAttribute(AttributeNames.RFC7643.RETURNED, Optional.ofNullable(returned).map(Returned::getValue).orElse(null));
  }

  /**
   * A single keyword value that specifies how the service provider enforces uniqueness of attribute values. A
   * server MAY reject an invalid value based on uniqueness by returning HTTP response code 400 (Bad Request). A
   * client MAY enforce uniqueness on the client side to a greater degree than the service provider enforces.
   * For example, a client could make a value unique while the server has uniqueness of "none". Valid keywords
   * are as follows: none The values are not intended to be unique in any way. DEFAULT. server The value SHOULD
   * be unique within the context of the current SCIM endpoint (or tenancy) and MAY be globally unique (e.g., a
   * "username", email address, or other server-generated key or counter). No two resources on the same server
   * SHOULD possess the same value. global The value SHOULD be globally unique (e.g., an email address, a GUID,
   * or other value). No two resources on any server SHOULD possess the same value.
   */
  public Uniqueness getUniqueness()
  {
    return getStringAttribute(AttributeNames.RFC7643.UNIQUENESS).map(Uniqueness::getByValue).orElse(null);
  }

  /**
   * A single keyword value that specifies how the service provider enforces uniqueness of attribute values. A
   * server MAY reject an invalid value based on uniqueness by returning HTTP response code 400 (Bad Request). A
   * client MAY enforce uniqueness on the client side to a greater degree than the service provider enforces.
   * For example, a client could make a value unique while the server has uniqueness of "none". Valid keywords
   * are as follows: none The values are not intended to be unique in any way. DEFAULT. server The value SHOULD
   * be unique within the context of the current SCIM endpoint (or tenancy) and MAY be globally unique (e.g., a
   * "username", email address, or other server-generated key or counter). No two resources on the same server
   * SHOULD possess the same value. global The value SHOULD be globally unique (e.g., an email address, a GUID,
   * or other value). No two resources on any server SHOULD possess the same value.
   */
  public void setUniqueness(Uniqueness uniqueness)
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
  public void setMultiValued(boolean multiValued)
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
  public void setRequired(boolean required)
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
  public void setCaseExact(boolean caseExact)
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
  public void setCanonicalValues(List<String> canonicalValues)
  {
    setStringAttributeList(AttributeNames.RFC7643.CANONICAL_VALUES, canonicalValues);
  }

  /**
   * A multi-valued array of JSON strings that indicate the SCIM resource types that may be referenced. Valid
   * values are as follows: + A SCIM resource type (e.g., "User" or "Group"), + "external" - indicating that the
   * resource is an external resource (e.g., a photo), or + "uri" - indicating that the reference is to a
   * service endpoint or an identifier (e.g., a schema URN). This attribute is only applicable for attributes
   * that are of type "reference" (Section 2.3.7).
   */
  public List<ReferenceTypes> getReferenceTypes()
  {
    return getSimpleArrayAttribute(AttributeNames.RFC7643.REFERENCE_TYPES).stream()
                                                                          .map(ReferenceTypes::getByValue)
                                                                          .collect(Collectors.toList());
  }

  /**
   * A multi-valued array of JSON strings that indicate the SCIM resource types that may be referenced. Valid
   * values are as follows: + A SCIM resource type (e.g., "User" or "Group"), + "external" - indicating that the
   * resource is an external resource (e.g., a photo), or + "uri" - indicating that the reference is to a
   * service endpoint or an identifier (e.g., a schema URN). This attribute is only applicable for attributes
   * that are of type "reference" (Section 2.3.7).
   */
  public void setReferenceTypes(List<ReferenceTypes> referenceTypes)
  {
    setStringAttributeList(AttributeNames.RFC7643.REFERENCE_TYPES,
                           referenceTypes.stream().map(ReferenceTypes::getValue).collect(Collectors.toList()));
  }

  /**
   * Only usable in combination with 'type=reference' and 'resourceTypes=['resource']'. It will bind the
   * attribute to the ID of a specific resource. The value must match the name of a registered 'resourceType'
   * not a 'resource'-name! In case of the /Me endpoint use the value 'Me' not the value 'User'
   */
  public Optional<String> getResourceTypeReferenceName()
  {
    return getStringAttribute(AttributeNames.Custom.RESOURCE_TYPE_REFERENCE_NAME);
  }

  /**
   * Only usable in combination with 'type=reference' and 'resourceTypes=['resource']'. It will bind the
   * attribute to the ID of a specific resource. The value must match the name of a registered 'resourceType'
   * not a 'resource'-name! In case of the /Me endpoint use the value 'Me' not the value 'User'
   */
  public void setResourceTypeReferenceName(String resourceTypeReferenceName)
  {
    setAttribute(AttributeNames.Custom.RESOURCE_TYPE_REFERENCE_NAME, resourceTypeReferenceName);
  }

  // @formatter:off
  /**
   * The value of "multipleOf" MUST be a number, strictly greater than 0.
   *
   * A numeric instance is valid only if division by this keyword's value
   * results in an integer.
   */
  // @formatter:on
  public Optional<Double> getMultipleOf()
  {
    return getDoubleAttribute(AttributeNames.Custom.MULTIPLE_OF);
  }

  // @formatter:off
  /**
   * The value of "multipleOf" MUST be a number, strictly greater than 0.
   *
   * A numeric instance is valid only if division by this keyword's value
   * results in an integer.
   */
  // @formatter:on
  public void setMultipleOf(double multipleOf)
  {
    if (Type.INTEGER.equals(getType()) || Type.DECIMAL.equals(getType()))
    {
      setAttribute(AttributeNames.Custom.MULTIPLE_OF, multipleOf);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.MULTIPLE_OF
                                       + "' is only applicable to 'integer' and 'decimal' types");
    }
  }

  // @formatter:off
  /**
   * The value of "minimum" MUST be a number, representing an inclusive
   * lower limit for a numeric instance.
   *
   * If the instance is a number, then this keyword validates only if the
   * instance is greater than or exactly equal to "minimum".
   */
  // @formatter:on
  public Optional<Double> getMinimum()
  {
    return getDoubleAttribute(AttributeNames.Custom.MINIMUM);
  }

  // @formatter:off
  /**
   * The value of "minimum" MUST be a number, representing an inclusive
   * lower limit for a numeric instance.
   *
   * If the instance is a number, then this keyword validates only if the
   * instance is greater than or exactly equal to "minimum".
   */
  // @formatter:on
  public void setMinimum(double minimum)
  {
    if (Type.INTEGER.equals(getType()) || Type.DECIMAL.equals(getType()))
    {
      setAttribute(AttributeNames.Custom.MINIMUM, minimum);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.MINIMUM + "' is only applicable to"
                                       + " 'integer' and 'decimal' types");
    }
  }

  // @formatter:off
  /**
   * The value of "maximum" MUST be a number, representing an inclusive
   * upper limit for a numeric instance.
   *
   * If the instance is a number, then this keyword validates only if the
   * instance is less than or exactly equal to "maximum".
   */
  // @formatter:on
  public Optional<Double> getMaximum()
  {
    return getDoubleAttribute(AttributeNames.Custom.MAXIMUM);
  }

  // @formatter:off
  /**
   * The value of "maximum" MUST be a number, representing an inclusive
   * upper limit for a numeric instance.
   *
   * If the instance is a number, then this keyword validates only if the
   * instance is less than or exactly equal to "maximum".
   */
  // @formatter:on
  public void setMaximum(double maximum)
  {
    if (Type.INTEGER.equals(getType()) || Type.DECIMAL.equals(getType()))
    {
      setAttribute(AttributeNames.Custom.MAXIMUM, maximum);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.MAXIMUM + "' is only applicable to"
                                       + " 'integer' and 'decimal' types");
    }
  }

  // @formatter:off
  /**
   * The value of this keyword MUST be a non-negative integer.
   *
   * A string instance is valid against this keyword if its length is less
   * than, or equal to, the value of this keyword.
   *
   * The length of a string instance is defined as the number of its
   * characters as defined by RFC 8259 [RFC8259].
   */
  // @formatter:on
  public Optional<Long> getMaxLength()
  {
    return getLongAttribute(AttributeNames.Custom.MAX_LENGTH);
  }

  // @formatter:off
  /**
   * The value of this keyword MUST be a non-negative integer.
   *
   * A string instance is valid against this keyword if its length is less
   * than, or equal to, the value of this keyword.
   *
   * The length of a string instance is defined as the number of its
   * characters as defined by RFC 8259 [RFC8259].
   */
  // @formatter:on
  public void setMaxLength(Long maxLength)
  {
    if (Type.STRING.equals(getType()) || Type.REFERENCE.equals(getType()))
    {
      setAttribute(AttributeNames.Custom.MAX_LENGTH, maxLength);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.MAX_LENGTH
                                       + "' is only applicable to 'string' and 'reference' types");
    }
  }

  // @formatter:off
  /**
   * The value of this keyword MUST be a non-negative integer.
   *
   * A string instance is valid against this keyword if its length is
   * greater than, or equal to, the value of this keyword.
   *
   * The length of a string instance is defined as the number of its
   * characters as defined by RFC 8259 [RFC8259].
   *
   * Omitting this keyword has the same behavior as a value of 0.
   */
  // @formatter:on
  public Optional<Long> getMinLength()
  {
    return getLongAttribute(AttributeNames.Custom.MIN_LENGTH);
  }

  // @formatter:off
  /**
   * The value of this keyword MUST be a non-negative integer.
   *
   * A string instance is valid against this keyword if its length is
   * greater than, or equal to, the value of this keyword.
   *
   * The length of a string instance is defined as the number of its
   * characters as defined by RFC 8259 [RFC8259].
   *
   * Omitting this keyword has the same behavior as a value of 0.
   */
  // @formatter:on
  public void setMinLength(Long minLength)
  {
    if (Type.STRING.equals(getType()) || Type.REFERENCE.equals(getType()))
    {
      setAttribute(AttributeNames.Custom.MIN_LENGTH, minLength);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.MIN_LENGTH
                                       + "' is only applicable to 'string' and 'reference' types");
    }
  }

  // @formatter:off
  /**
   * The value of this keyword MUST be a string.  This string SHOULD be a
   * valid regular expression, according to the Java regular
   * expression dialect.
   *
   * A string instance is considered valid if the regular expression
   * matches the instance successfully.  Recall: regular expressions are
   * not implicitly anchored.
   */
  // @formatter:on
  public Optional<Pattern> getPattern()
  {
    return Optional.ofNullable(pattern);
  }

  // @formatter:off
  /**
   * The value of this keyword MUST be a string.  This string SHOULD be a
   * valid regular expression, according to the Java regular
   * expression dialect.
   *
   * A string instance is considered valid if the regular expression
   * matches the instance successfully.  Recall: regular expressions are
   * not implicitly anchored.
   */
  // @formatter:on
  public void setPattern(String pattern)
  {
    if (pattern == null)
    {
      setAttribute(AttributeNames.Custom.PATTERN, (String)null);
      return;
    }
    Type type = getType();
    if (Type.STRING.equals(type) || Type.REFERENCE.equals(type))
    {
      try
      {
        this.pattern = Pattern.compile(pattern);
      }
      catch (PatternSyntaxException ex)
      {
        log.error(ex.getMessage(), ex);
        throw new InvalidSchemaException("the given pattern is not a valid regular expression '" + pattern + "'");
      }
      setAttribute(AttributeNames.Custom.PATTERN, pattern);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.PATTERN
                                       + "' is only applicable to 'string' and 'reference' types");
    }
  }

  // @formatter:off
  /**
   * The value of this keyword MUST be a non-negative integer.
   *
   * An array instance is valid against "minItems" if its size is greater
   * than, or equal to, the value of this keyword.
   *
   * Omitting this keyword has the same behavior as a value of 0.
   */
  // @formatter:on
  public Optional<Integer> getMinItems()
  {
    return getIntegerAttribute(AttributeNames.Custom.MIN_ITEMS);
  }

  // @formatter:off
  /**
   * The value of this keyword MUST be a non-negative integer.
   *
   * An array instance is valid against "minItems" if its size is greater
   * than, or equal to, the value of this keyword.
   *
   * Omitting this keyword has the same behavior as a value of 0.
   */
  // @formatter:on
  public void setMinItems(Integer minItems)
  {
    if (isMultiValued())
    {
      setAttribute(AttributeNames.Custom.MIN_ITEMS, minItems);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.MIN_ITEMS
                                       + "' is only applicable to 'multivalued' types");
    }
  }

  /**
   * The value of this keyword MUST be a non-negative integer. An array instance is valid against "maxItems" if
   * its size is less than, or equal to, the value of this keyword.
   */
  public Optional<Integer> getMaxItems()
  {
    return getIntegerAttribute(AttributeNames.Custom.MAX_ITEMS);
  }

  /**
   * The value of this keyword MUST be a non-negative integer. An array instance is valid against "maxItems" if
   * its size is less than, or equal to, the value of this keyword.
   */
  public void setMaxItems(Integer maxItems)
  {
    if (isMultiValued())
    {
      setAttribute(AttributeNames.Custom.MAX_ITEMS, maxItems);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.MAX_ITEMS
                                       + "' is only applicable to 'multivalued' types");
    }
  }

  /**
   * a dateTime validation attribute that must not be before the value of this attribute
   */
  public Optional<Instant> getNotBefore()
  {
    return getDateTimeAttribute(AttributeNames.Custom.NOT_BEFORE);
  }

  /**
   * a dateTime validation attribute that must not be before the value of this attribute
   */
  public void setNotBefore(String notBefore)
  {
    if (Type.DATE_TIME.equals(getType()))
    {
      setAttribute(AttributeNames.Custom.NOT_BEFORE, notBefore);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.NOT_BEFORE + "' is only "
                                       + "applicable to 'dateTime' types");
    }
  }

  /**
   * a dateTime validation attribute that must not be before the value of this attribute
   */
  public void setNotBefore(Instant notBefore)
  {
    if (Type.DATE_TIME.equals(getType()))
    {
      setDateTimeAttribute(AttributeNames.Custom.NOT_BEFORE, notBefore);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.NOT_BEFORE + "' is only "
                                       + "applicable to 'dateTime' types");
    }
  }

  /**
   * a dateTime validation attribute that must not be before the value of this attribute
   */
  public void setNotBefore(LocalDateTime notBefore)
  {
    if (Type.DATE_TIME.equals(getType()))
    {
      setDateTimeAttribute(AttributeNames.Custom.NOT_BEFORE, notBefore);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.NOT_BEFORE + "' is only "
                                       + "applicable to 'dateTime' types");
    }
  }

  /**
   * a dateTime validation attribute that must not be before the value of this attribute
   */
  public void setNotBefore(OffsetDateTime notBefore)
  {
    if (Type.DATE_TIME.equals(getType()))
    {
      setDateTimeAttribute(AttributeNames.Custom.NOT_BEFORE, notBefore);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.NOT_BEFORE + "' is only "
                                       + "applicable to 'dateTime' types");
    }
  }

  /**
   * a dateTime validation attribute that must not be after the value of this attribute
   */
  public Optional<Instant> getNotAfter()
  {
    return getDateTimeAttribute(AttributeNames.Custom.NOT_AFTER);
  }

  /**
   * a dateTime validation attribute that must not be after the value of this attribute
   */
  public void setNotAfter(String notAfter)
  {
    if (Type.DATE_TIME.equals(getType()))
    {
      setAttribute(AttributeNames.Custom.NOT_AFTER, notAfter);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.NOT_AFTER + "' is only "
                                       + "applicable to 'dateTime' types");
    }
  }

  /**
   * a dateTime validation attribute that must not be after the value of this attribute
   */
  public void setNotAfter(Instant notAfter)
  {
    if (Type.DATE_TIME.equals(getType()))
    {
      setDateTimeAttribute(AttributeNames.Custom.NOT_AFTER, notAfter);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.NOT_AFTER + "' is only "
                                       + "applicable to 'dateTime' types");
    }
  }

  /**
   * a dateTime validation attribute that must not be after the value of this attribute
   */
  public void setNotAfter(LocalDateTime notAfter)
  {
    if (Type.DATE_TIME.equals(getType()))
    {
      setDateTimeAttribute(AttributeNames.Custom.NOT_AFTER, notAfter);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.NOT_AFTER + "' is only "
                                       + "applicable to 'dateTime' types");
    }
  }

  /**
   * a dateTime validation attribute that must not be after the value of this attribute
   */
  public void setNotAfter(OffsetDateTime notAfter)
  {
    if (Type.DATE_TIME.equals(getType()))
    {
      setDateTimeAttribute(AttributeNames.Custom.NOT_AFTER, notAfter);
    }
    else
    {
      throw new InvalidSchemaException("The attribute '" + AttributeNames.Custom.NOT_AFTER + "' is only "
                                       + "applicable to 'dateTime' types");
    }
  }

  /**
   * A default value that is set if the attribute has no value
   */
  public String getDefaultValue()
  {
    return getStringAttribute(AttributeNames.Custom.DEFAULT_VALUE).orElse(null);
  }

  /**
   * A default value that is set if the attribute has no value
   */
  public void setDefaultValue(String defaultValue)
  {
    if (StringUtils.isBlank(defaultValue))
    {
      remove(AttributeNames.Custom.DEFAULT_VALUE);
      return;
    }

    if (isMultiValued() && defaultValue.trim().startsWith("["))
    {
      try
      {
        JsonNode jsonNode = JsonHelper.readJsonDocument(defaultValue);
        if (jsonNode == null)
        {
          return;
        }
        if (jsonNode.isArray())
        {
          List<String> validElements = new ArrayList<>();
          for ( JsonNode node : jsonNode )
          {
            String val = node.isContainerNode() ? node.toString() : node.asText();
            if (validateDefaultValue(val))
            {
              validElements.add(val);
            }
          }
          if (validElements.isEmpty())
          {
            log.warn("Ignoring default value for attribute '{}' because no element of the array '{}' applies to type '{}'",
                     getName(),
                     defaultValue,
                     getType());
            return;
          }
          String cleanedDefaultValue = validElements.stream().map(val -> {
            return Type.INTEGER.equals(getType()) || Type.DECIMAL.equals(getType()) || Type.BOOLEAN.equals(getType())
                   || Type.COMPLEX.equals(getType()) ? val : "\"" + val + "\"";
          }).collect(Collectors.joining(", ", "[", "]"));
          setAttribute(AttributeNames.Custom.DEFAULT_VALUE, cleanedDefaultValue);
          return;
        }
      }
      catch (Exception ex)
      {
        log.debug("Failed to parse default value as array: {}", defaultValue, ex);
      }
    }

    boolean doesDefaultValueApplyToType = validateDefaultValue(defaultValue);
    if (doesDefaultValueApplyToType)
    {
      setAttribute(AttributeNames.Custom.DEFAULT_VALUE, defaultValue);
    }
    else
    {
      log.warn("Ignoring default value for attribute '{}' because the value '{}' does not apply to type '{}'",
               getName(),
               defaultValue,
               getType());
    }
  }

  /**
   * When an attribute is of type "complex", "subAttributes" defines a set of sub-attributes. "subAttributes"
   * has the same schema sub-attributes as "attributes".
   */
  // @formatter:on
  public List<SchemaAttribute> getSubAttributes()
  {
    return getArrayAttribute(AttributeNames.RFC7643.SUB_ATTRIBUTES, SchemaAttribute.class);
  }

  /**
   * When an attribute is of type "complex", "subAttributes" defines a set of sub-attributes. "subAttributes"
   * has the same schema sub-attributes as "attributes".
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
    boolean hasValueAttribute = false;
    boolean isResourceReference = false;
    for ( JsonNode subAttribute : subAttributesArray )
    {
      SchemaAttribute schemaAttribute = new SchemaAttribute(schema, resourceUri, this, subAttribute, namePrefix);
      if (attributeNameSet.contains(schemaAttribute.getScimNodeName()))
      {
        String duplicateNameMessage = "The attribute with the name '" + schemaAttribute.getFullResourceName()
                                      + "' was found twice within the given schema declaration";
        throw new InvalidSchemaException(duplicateNameMessage, null, null, null);
      }
      if (!Mutability.READ_ONLY.equals(this.getMutability()))
      {
        if (AttributeNames.RFC7643.VALUE.equals(schemaAttribute.getName()))
        {
          hasValueAttribute = true;
        }
        if (AttributeNames.RFC7643.REF.equals(schemaAttribute.getName())
            && schemaAttribute.getReferenceTypes().contains(ReferenceTypes.RESOURCE))
        {
          isResourceReference = true;
        }
      }
      attributeNameSet.add(schemaAttribute.getScimNodeName());
      schemaAttributeList.add(schemaAttribute);
      subAttributes.put(schemaAttribute.getName().toLowerCase(), schemaAttribute);
    }
    if (hasValueAttribute && isResourceReference)
    {
      this.schema.getComplexBulkIdCandidates().add(this);
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
   * the last problem is that an attribute with the same name was declared twice. This problem will be handled
   * in another method
   */
  private void validateAttribute()
  {
    final Mutability mutability = getMutability();
    final Returned returned = getReturned();
    if (Mutability.READ_ONLY.equals(mutability) && Returned.NEVER.equals(returned))
    {
      String errorMessage = "The attribute with the name '" + getFullResourceName() + "' has an invalid declaration. "
                            + "mutability 'readOnly' and returned 'never' are an illegal combination. The client is "
                            + "not able to write to the given attribute and the server will never return it.";
      throw getException(errorMessage, null);
    }
    else if (Mutability.WRITE_ONLY.equals(mutability) && !Returned.NEVER.equals(returned))
    {
      String errorMessage = "The attribute with the name '" + getFullResourceName() + "' has an invalid declaration. "
                            + "mutability 'writeOnly' must have a returned value of 'never' are an illegal in "
                            + "combination. The client should only write to this attribute but should never have it "
                            + "returned. The mutability writeOnly makes only sense for sensitive application data "
                            + "like passwords or other secrets.";
      throw getException(errorMessage, null);
    }
    // binaries must be case exact
    if (Type.BINARY.equals(getType()) && !isCaseExact())
    {
      String errorMessage = "The attribute with the name '" + getFullResourceName() + "' has an invalid declaration. "
                            + "Binaries have to be case-exact by definition.";
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

  /**
   * gets the attributes in an upside down list with its parents on index 0 until index n which is the leaf node
   */
  public List<SchemaAttribute> getParentHierarchy()
  {
    List<SchemaAttribute> parentHierarchy = new ArrayList<>();
    SchemaAttribute currentNode = this;
    while (currentNode != null)
    {
      parentHierarchy.add(0, currentNode);
      currentNode = currentNode.getParent();
    }
    return parentHierarchy;
  }

  /**
   * verifies that the given default-value applies to the {@link SchemaAttribute}s definition
   *
   * @param schemaAttribute the attributes definition
   * @param defaultValue the default-value that is being assigned to the attributes-definition
   */
  private boolean validateDefaultValue(String defaultValue)
  {
    try
    {
      switch (getType())
      {
        case BOOLEAN:
          return StringUtils.equalsAnyIgnoreCase(defaultValue, "true", "false");
        case INTEGER:
          Integer.parseInt(defaultValue);
          return true;
        case DECIMAL:
          Double.parseDouble(defaultValue);
          return true;
        case COMPLEX:
        {
          try
          {
            JsonNode jsonNode = JsonHelper.readJsonDocument(defaultValue);
            return jsonNode == null || jsonNode.isObject() || jsonNode.isArray();
          }
          catch (Exception ex)
          {
            return false;
          }
        }
        case BINARY:
        case ANY:
          throw new InternalServerException(String.format("Invalid configuration. Default values are only supported for "
                                                          + "the following types: %s",
                                                          Arrays.asList(Type.BOOLEAN,
                                                                        Type.INTEGER,
                                                                        Type.DECIMAL,
                                                                        Type.STRING,
                                                                        Type.REFERENCE,
                                                                        Type.DATE_TIME,
                                                                        Type.COMPLEX)));
        default:
          return true;
      }
    }
    catch (Exception ex)
    {
      log.debug(ex.getMessage(), ex);
      return false;
    }
  }
}
