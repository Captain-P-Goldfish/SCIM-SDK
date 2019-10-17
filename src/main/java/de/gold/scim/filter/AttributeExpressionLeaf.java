package de.gold.scim.filter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.constants.enums.Mutability;
import de.gold.scim.constants.enums.ReferenceTypes;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.constants.enums.Uniqueness;
import de.gold.scim.exceptions.InvalidFilterException;
import de.gold.scim.filter.antlr.AttributeName;
import de.gold.scim.filter.antlr.Comparator;
import de.gold.scim.filter.antlr.CompareValue;
import de.gold.scim.filter.antlr.ScimFilterParser;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.Schema;
import de.gold.scim.schemas.SchemaAttribute;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 12:37 <br>
 * <br>
 * Represents a comparable expression in the scim filter language like "userName eq 'chuck_norris'"
 */
@EqualsAndHashCode
public class AttributeExpressionLeaf extends FilterNode
{

  /**
   * the scim attribute name. This must be an attribute name that was previously registered with a resource
   * schema. if the attribute cannot be found in the represented {@link de.gold.scim.schemas.ResourceType} an
   * {@link de.gold.scim.exceptions.InvalidFilterException} is thrown
   */
  private AttributeName attributeName;

  /**
   * the comparator that tells us how the comparison should be executed
   */
  @Getter(AccessLevel.PUBLIC)
  private Comparator comparator;

  /**
   * the value of the comparison itself
   */
  private CompareValue compareValue;

  /**
   * the meta information of this attribute
   */
  private SchemaAttribute schemaAttribute;

  public AttributeExpressionLeaf(ScimFilterParser.AttributeExpressionContext context, ResourceType resourceType)
  {
    this.attributeName = new AttributeName(context.attributePath());
    this.comparator = Comparator.valueOf(getCompareOperatorValue(context));
    this.schemaAttribute = getSchemaAttribute(resourceType, attributeName);
    this.compareValue = context.compareValue() == null ? null
      : new CompareValue(context.compareValue(), schemaAttribute);
    validateFilterComparator();
  }

  /**
   * if the schema attribute is of {@link Type#BOOLEAN} than several operators are not allowed and must throw an
   * exception
   */
  private void validateFilterComparator()
  {
    if (Type.BOOLEAN.equals(schemaAttribute.getType()))
    {
      switch (comparator)
      {
        case GE:
        case GT:
        case LE:
        case LT:
          throw new InvalidFilterException("the comparator '" + comparator + "' is not allowed on attribute type '"
                                           + schemaAttribute.getType() + "'", null);
      }
    }
  }

  /**
   * gets the {@link SchemaAttribute} from the given {@link ResourceType}
   *
   * @param resourceType the resource type from which the attribute definition should be extracted
   * @param attributeName this instance holds the attribute name to extract the {@link SchemaAttribute} from the
   *          {@link ResourceType}
   * @return the found {@link SchemaAttribute} definition
   * @throws de.gold.scim.exceptions.InvalidFilterException if no {@link SchemaAttribute} was found for the
   *           given name attribute
   */
  private SchemaAttribute getSchemaAttribute(ResourceType resourceType, AttributeName attributeName)
  {
    final boolean resourceUriPresent = StringUtils.isNotBlank(attributeName.getResourceUri());
    final String scimNodeName = attributeName.getShortName();
    List<Schema> resourceTypeSchemas = resourceType.getAllSchemas();

    List<SchemaAttribute> schemaAttributeList;
    if (resourceUriPresent)
    {
      schemaAttributeList = resourceTypeSchemas.stream()
                                               .filter(schema -> schema.getId().equals(attributeName.getResourceUri()))
                                               .map(schema -> schema.getSchemaAttribute(scimNodeName))
                                               .filter(Objects::nonNull)
                                               .collect(Collectors.toList());
    }
    else
    {
      schemaAttributeList = resourceTypeSchemas.stream()
                                               .map(schema -> schema.getSchemaAttribute(scimNodeName))
                                               .filter(Objects::nonNull)
                                               .collect(Collectors.toList());
    }
    if (schemaAttributeList.isEmpty())
    {
      throw new InvalidFilterException("the attribute with the name '" + attributeName.getShortName() + "' is "
                                       + "unknown to resource type '" + resourceType.getName() + "'", null);
    }
    else if (schemaAttributeList.size() > 1)
    {
      String schemaIds = schemaAttributeList.stream()
                                            .map(schemaAttribute -> schemaAttribute.getSchema().getId())
                                            .collect(Collectors.joining(","));
      String exampleAttributeName = schemaAttributeList.get(0).getSchema().getId() + ":" + attributeName.getShortName();
      throw new InvalidFilterException("the attribute with the name '" + attributeName.getShortName() + "' is "
                                       + "ambiguous it was found in the schemas with the ids [" + schemaIds + "]. "
                                       + "Please use the fully qualified Uri for this attribute e.g.: "
                                       + exampleAttributeName, null);
    }
    else
    {
      return schemaAttributeList.get(0);
    }
  }

  /**
   * tries to get the compare operator. This must be handled differently in cases when it is the
   * {@link Comparator#PR} operator because no value will be present then and the present comparator will be a
   * {@link org.antlr.v4.runtime.tree.TerminalNode} instead of a
   * {@link de.gold.scim.filter.antlr.ScimFilterParser.CompareOperatorContext} node
   *
   * @param context the antlr context to extract the {@link Comparator} value
   * @return the {@link Comparator} value as string in upper case
   */
  private String getCompareOperatorValue(ScimFilterParser.AttributeExpressionContext context)
  {
    if (context.compareOperator() == null)
    {
      return context.children.get(1).getText().toUpperCase();
    }
    return context.compareOperator().getText().toUpperCase();
  }

  public String getResourceUri()
  {
    return attributeName.getResourceUri();
  }

  public String getShortName()
  {
    return attributeName.getShortName();
  }

  public String getFullName()
  {
    return attributeName.getFullName();
  }

  public String getValue()
  {
    return compareValue == null ? null : compareValue.getValue();
  }

  public Optional<Boolean> getBooleanValue()
  {
    return compareValue == null ? Optional.empty() : compareValue.getBooleanValue();
  }

  public Optional<Integer> getIntegerValue()
  {
    return compareValue == null ? Optional.empty() : compareValue.getIntegerValue();
  }

  public Optional<Double> getDoubleValue()
  {
    return compareValue == null ? Optional.empty() : compareValue.getDoubleValue();
  }

  public Optional<String> getStringValue()
  {
    return compareValue == null ? Optional.empty() : compareValue.getStringValue();
  }

  public Optional<Instant> getDateTime()
  {
    return compareValue == null ? Optional.empty() : compareValue.getDateTime();
  }

  public Type getType()
  {
    return schemaAttribute.getType();
  }

  public Mutability getMutability()
  {
    return schemaAttribute.getMutability();
  }

  public Uniqueness getUniqueness()
  {
    return schemaAttribute.getUniqueness();
  }

  public boolean isMultiValued()
  {
    return schemaAttribute.isMultiValued();
  }

  public boolean isRequired()
  {
    return schemaAttribute.isRequired();
  }

  public boolean isCaseExact()
  {
    return schemaAttribute.isCaseExact();
  }

  public List<String> getCanonicalValues()
  {
    return schemaAttribute.getCanonicalValues();
  }

  public List<ReferenceTypes> getReferenceTypes()
  {
    return schemaAttribute.getReferenceTypes();
  }

  @Override
  public String toString()
  {
    return attributeName + " " + comparator + (compareValue == null ? "" : " " + compareValue);
  }
}
