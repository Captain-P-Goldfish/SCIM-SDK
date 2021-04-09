package de.captaingoldfish.scim.sdk.server.utils;

import java.time.Instant;
import java.util.Optional;

import org.junit.platform.commons.util.StringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * @author Pascal Knueppel
 * @since 08.04.2021
 */
public class SchemaAttributeBuilder
{

  /**
   * the parent attribute of the attribute that is being created
   */
  private SchemaAttribute parent;

  /**
   * the uri of the resource to which this attribute belongs e.g.: urn:ietf:params:scim:schemas:core:2.0:User
   */
  private String resourceUri;

  /**
   * an optional attribute that is used as a workaround. For example the meta attribute definition has been
   * separated from the normal resource schemata in order to prevent developers for having to define the
   * meta-attribute definition for each resource separately. But if this is done the name of the attributes is
   * not build correctly because meta definition is not a schema-definition and not an attribute definition
   * anymore. Therefore this name prefix can be used to build the attribute name correctly.<br>
   * in case of meta the attribute "created" would only get the name "created". But if this variable is set to
   * "meta" than the attribute will be accessible by the name "meta.created" instead of just "created"
   */
  private String namePrefix;

  private String name;

  private Type type;

  private String description;

  private Mutability mutability;

  private Returned returned;

  private Uniqueness uniqueness;

  private boolean multivalued;

  private boolean required;

  private boolean caseExact;

  private ArrayNode canonicalValues;

  private ArrayNode referenceTypes;

  private Double multipleOf;

  private Double minimum;

  private Double maximum;

  private Long maxLength;

  private Long minLength;

  private String pattern;

  private Integer minItems;

  private Integer maxItems;

  private Instant notBefore;

  private Instant notAfter;


  public static SchemaAttributeBuilder builder()
  {
    return new SchemaAttributeBuilder();
  }

  public SchemaAttributeBuilder parent(SchemaAttribute parent)
  {
    this.parent = parent;
    return this;
  }

  public SchemaAttributeBuilder resourceUri(String resourceUri)
  {
    this.resourceUri = resourceUri;
    return this;
  }

  public SchemaAttributeBuilder namePrefix(String namePrefix)
  {
    this.namePrefix = namePrefix;
    return this;
  }

  public SchemaAttributeBuilder name(String name)
  {
    this.name = name;
    return this;
  }

  public SchemaAttributeBuilder type(Type type)
  {
    this.type = type;
    return this;
  }

  public SchemaAttributeBuilder description(String description)
  {
    this.description = description;
    return this;
  }

  public SchemaAttributeBuilder mutability(Mutability mutability)
  {
    this.mutability = mutability;
    return this;
  }

  public SchemaAttributeBuilder returned(Returned returned)
  {
    this.returned = returned;
    return this;
  }

  public SchemaAttributeBuilder uniqueness(Uniqueness uniqueness)
  {
    this.uniqueness = uniqueness;
    return this;
  }

  public SchemaAttributeBuilder multivalued(boolean multivalued)
  {
    this.multivalued = multivalued;
    return this;
  }

  public SchemaAttributeBuilder required(boolean required)
  {
    this.required = required;
    return this;
  }

  public SchemaAttributeBuilder caseExact(boolean caseExact)
  {
    this.caseExact = caseExact;
    return this;
  }

  public SchemaAttributeBuilder canonicalValues(String... canonicalValues)
  {
    Optional.ofNullable(canonicalValues).ifPresent(values -> {
      this.canonicalValues = new ArrayNode(JsonNodeFactory.instance);
      for ( String value : values )
      {
        this.canonicalValues.add(value);
      }
    });
    return this;
  }

  public SchemaAttributeBuilder referenceTypes(ReferenceTypes... referenceTypes)
  {
    Optional.ofNullable(referenceTypes).ifPresent(values -> {
      this.referenceTypes = new ArrayNode(JsonNodeFactory.instance);
      for ( ReferenceTypes value : values )
      {
        this.referenceTypes.add(value.getValue());
      }
    });
    return this;
  }

  public SchemaAttributeBuilder multipleOf(double multipleOf)
  {
    this.multipleOf = multipleOf;
    return this;
  }

  public SchemaAttributeBuilder minimum(double minimum)
  {
    this.minimum = minimum;
    return this;
  }

  public SchemaAttributeBuilder maximum(double maximum)
  {
    this.maximum = maximum;
    return this;
  }

  public SchemaAttributeBuilder maxLength(long maxLength)
  {
    this.maxLength = maxLength;
    return this;
  }

  public SchemaAttributeBuilder minLength(long minLength)
  {
    this.minLength = minLength;
    return this;
  }

  public SchemaAttributeBuilder minItems(int minItems)
  {
    this.minItems = minItems;
    return this;
  }

  public SchemaAttributeBuilder pattern(String pattern)
  {
    this.pattern = pattern;
    return this;
  }

  public SchemaAttributeBuilder maxItems(int maxItems)
  {
    this.maxItems = maxItems;
    return this;
  }

  public SchemaAttributeBuilder notBefore(Instant notBefore)
  {
    this.notBefore = notBefore;
    return this;
  }

  public SchemaAttributeBuilder notAfter(Instant notAfter)
  {
    this.notAfter = notAfter;
    return this;
  }

  public SchemaAttribute build()
  {
    validateBuild();
    this.resourceUri = Optional.ofNullable(resourceUri).orElse("urn:captaingoldfish:test:attribute:UnitTest");
    SchemaAttribute schemaAttribute = new SchemaAttribute(parent, resourceUri, namePrefix);
    schemaAttribute.set(AttributeNames.RFC7643.NAME, new TextNode(name));
    schemaAttribute.set(AttributeNames.RFC7643.TYPE,
                        new TextNode(Optional.ofNullable(type)
                                             .map(Type::getValue)
                                             .orElse(Type.STRING.name().toLowerCase())));
    schemaAttribute.set(AttributeNames.RFC7643.DESCRIPTION,
                        new TextNode(Optional.ofNullable(description).orElse("A description")));
    schemaAttribute.set(AttributeNames.RFC7643.MUTABILITY,
                        new TextNode(Optional.ofNullable(mutability)
                                             .map(Mutability::getValue)
                                             .orElse(Mutability.READ_WRITE.name().toLowerCase())));
    schemaAttribute.set(AttributeNames.RFC7643.RETURNED,
                        new TextNode(Optional.ofNullable(returned)
                                             .map(Returned::getValue)
                                             .orElse(Returned.DEFAULT.name().toLowerCase())));
    schemaAttribute.set(AttributeNames.RFC7643.UNIQUENESS,
                        new TextNode(Optional.ofNullable(uniqueness)
                                             .map(Uniqueness::getValue)
                                             .orElse(Uniqueness.NONE.name().toLowerCase())));
    schemaAttribute.set(AttributeNames.RFC7643.MULTI_VALUED, BooleanNode.valueOf(multivalued));
    schemaAttribute.set(AttributeNames.RFC7643.REQUIRED, BooleanNode.valueOf(required));
    schemaAttribute.set(AttributeNames.RFC7643.CASE_EXACT, BooleanNode.valueOf(caseExact));
    Optional.ofNullable(canonicalValues)
            .ifPresent(values -> schemaAttribute.set(AttributeNames.RFC7643.CANONICAL_VALUES, values));
    Optional.ofNullable(referenceTypes)
            .ifPresent(values -> schemaAttribute.set(AttributeNames.RFC7643.REFERENCE_TYPES, values));

    Optional.ofNullable(multipleOf).ifPresent(schemaAttribute::setMultipleOf);
    Optional.ofNullable(minimum).ifPresent(schemaAttribute::setMinimum);
    Optional.ofNullable(maximum).ifPresent(schemaAttribute::setMaximum);
    Optional.ofNullable(maxLength).ifPresent(schemaAttribute::setMaxLength);
    Optional.ofNullable(minLength).ifPresent(schemaAttribute::setMinLength);
    Optional.ofNullable(pattern).ifPresent(schemaAttribute::setPattern);
    Optional.ofNullable(minItems).ifPresent(schemaAttribute::setMinItems);
    Optional.ofNullable(maxItems).ifPresent(schemaAttribute::setMaxItems);
    Optional.ofNullable(notBefore).ifPresent(schemaAttribute::setNotBefore);
    Optional.ofNullable(notAfter).ifPresent(schemaAttribute::setNotAfter);
    return schemaAttribute;
  }

  private void validateBuild()
  {
    if (StringUtils.isBlank(name))
    {
      throw new IllegalArgumentException("Argument 'name' is mandatory");
    }
  }
}
