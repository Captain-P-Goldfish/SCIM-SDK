package de.gold.scim.server.filter.antlr;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import de.gold.scim.server.exceptions.InvalidDateTimeRepresentationException;
import de.gold.scim.server.exceptions.InvalidFilterException;
import de.gold.scim.server.filter.AttributeExpressionLeaf;
import de.gold.scim.server.schemas.SchemaAttribute;
import de.gold.scim.server.utils.TimeUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 12:39 <br>
 * <br>
 * this class contains the value of an {@link AttributeExpressionLeaf}. It is used to check what type of value
 * was parsed and to get the value in the desired type
 */
@Slf4j
@EqualsAndHashCode(exclude = "context")
public class CompareValue
{

  /**
   * the antlr context node that is used to get the value-data of the expression
   */
  private ScimFilterParser.CompareValueContext context;

  /**
   * this is used as display variable for the {@link #toString()} method. It holds the value of the context
   */
  @Getter
  private String value;

  /**
   * @param compareValueContext the antlr context for this value
   * @param schemaAttribute the attributes meta information
   */
  public CompareValue(ScimFilterParser.CompareValueContext compareValueContext, SchemaAttribute schemaAttribute)
  {
    this.context = compareValueContext;
    if (compareValueContext.isNull != null)
    {
      this.value = null;
    }
    else
    {
      this.value = compareValueContext.getText().replaceFirst("^\"", "").replaceFirst("\"$", "");
    }
    validateCompareValue(schemaAttribute);
  }

  /**
   * this method will verify that the given comparison value does apply to the attributes data type
   *
   * @param schemaAttribute the attributes meta information
   */
  private void validateCompareValue(SchemaAttribute schemaAttribute)
  {
    switch (schemaAttribute.getType())
    {
      case DATE_TIME:
        validateAttributeType(schemaAttribute, () -> isDateTime() || isString() || isNull());
        break;
      case BOOLEAN:
        validateAttributeType(schemaAttribute, () -> isFalse() || isTrue() || isNull());
        break;
      case INTEGER:
      case DECIMAL:
        validateAttributeType(schemaAttribute, () -> isNumber() || isNull());
        break;
      case REFERENCE:
      case STRING:
        validateAttributeType(schemaAttribute, () -> isString() || isNull());
        break;
      default:
        // do nothing
    }
  }

  /**
   * checks if this instance is of the given type and throws an exception if not
   *
   * @param schemaAttribute meta information about the attribute
   * @param isOfType if the here given value is of the correct type
   */
  private void validateAttributeType(SchemaAttribute schemaAttribute, Supplier<Boolean> isOfType)
  {
    if (!isOfType.get())
    {
      throw new InvalidFilterException("attribute '" + schemaAttribute.getFullResourceName() + "' is of type '"
                                       + schemaAttribute.getType() + "' but the given value '" + value + "' does not "
                                       + "apply to this type", null);
    }
  }

  /**
   * @return if this node represents a true-boolean
   */
  private boolean isTrue()
  {
    return context.isTrue != null;
  }

  /**
   * @return if this node represents a false-boolean
   */
  private boolean isFalse()
  {
    return context.isFalse != null;
  }

  /**
   * @return if this value is null
   */
  public boolean isNull()
  {
    return context.isNull != null;
  }

  /**
   * @return if this value is a parsable number
   */
  public boolean isNumber()
  {
    return context.number != null;
  }

  /**
   * @return if this value is a string value. String value includes also dateTime values. So if
   *         {@link #isDateTime()} evaluates to true then this method will also evaluate to true
   */
  public boolean isString()
  {
    return context.string != null;
  }

  /**
   * @return if this value is a valid dateTime value
   */
  public boolean isDateTime()
  {
    return getDateTime().isPresent();
  }

  /**
   * @return if this value is of type boolean
   */
  public boolean isBoolean()
  {
    return isTrue() || isFalse();
  }

  /**
   * @return gets this node as boolean if it is a boolean
   */
  public Optional<Boolean> getBooleanValue()
  {
    boolean isBoolean = isBoolean();
    if (isBoolean)
    {
      return Optional.of(isTrue());
    }
    return Optional.empty();
  }

  /**
   * @return gets this node as double if it is a double
   */
  public Optional<BigDecimal> getNumberValue()
  {
    if (isNumber())
    {
      try
      {
        return Optional.of(new BigDecimal(context.number.getText()));
      }
      catch (NumberFormatException ex)
      {
        log.trace(ex.getMessage(), ex);
      }
    }
    return Optional.empty();
  }

  /**
   * @return gets this node as string value if it is a string
   */
  public Optional<String> getStringValue()
  {
    if (isString())
    {
      return Optional.of(value);
    }
    return Optional.empty();
  }

  /**
   * @return gets this node as dateTime if it is a dateTime
   */
  public Optional<Instant> getDateTime()
  {
    if (!isString())
    {
      return Optional.empty();
    }
    try
    {
      return Optional.of(TimeUtils.parseDateTime(value));
    }
    catch (InvalidDateTimeRepresentationException e)
    {
      log.trace(e.getMessage(), e);
      return Optional.empty();
    }
  }

  @Override
  public String toString()
  {
    if (isString())
    {
      return "\"" + value + "\"";
    }
    return value;
  }
}
