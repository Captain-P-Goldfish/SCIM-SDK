package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.TimeUtils;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 30.11.2019 - 22:48 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CustomAttributeValidator
{

  /**
   * validates a simple attribute that is neither an object nor an array against its custom schema validation
   *
   * @param schemaAttribute the attributes definition
   * @param validatedAttribute the attribute to validate
   */
  public static void validateSimpleNode(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    if (schemaAttribute == null || attribute == null)
    {
      return;
    }

    if (attribute.isTextual())
    {
      validateTextNode(schemaAttribute, (TextNode)attribute);
    }
    else if (attribute.isBigDecimal())
    {
      validateNumberNode(schemaAttribute, (DecimalNode)attribute);
    }
    else if (attribute.isLong())
    {
      validateNumberNode(schemaAttribute, (LongNode)attribute);
    }
    else if (attribute.isNumber())
    {
      validateNumberNode(schemaAttribute, new DecimalNode(attribute.decimalValue()));
    }

  }

  /**
   * validates if the given value matches the requirements of the schema attribute
   *
   * @param schemaAttribute the attribute definition
   * @param value the value that should be checked
   */
  protected static void validateTextNode(SchemaAttribute schemaAttribute, TextNode valueNode)
  {
    if (Type.STRING.equals(schemaAttribute.getType()) || Type.REFERENCE.equals(schemaAttribute.getType()))
    {
      validateStringTypes(schemaAttribute, valueNode);
    }
    else if (Type.DATE_TIME.equals(schemaAttribute.getType()))
    {
      validateDateTimeTypes(schemaAttribute, valueNode);
    }
  }

  /**
   * will verify dateTime type attributes
   *
   * @param schemaAttribute the attribute definition
   * @param value the dateTime value to validate
   */
  private static void validateDateTimeTypes(SchemaAttribute schemaAttribute, TextNode valueNode)
  {
    final String value = valueNode.textValue();
    schemaAttribute.getNotBefore().ifPresent(notBefore -> {
      Instant val = TimeUtils.parseDateTime(value);
      if (val.isBefore(notBefore))
      {
        String errorMessage = String.format("The '%s'-attribute '%s' with value '%s' must not be before '%s'",
                                            schemaAttribute.getType(),
                                            schemaAttribute.getScimNodeName(),
                                            val,
                                            notBefore);
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
    schemaAttribute.getNotAfter().ifPresent(notAfter -> {
      Instant val = TimeUtils.parseDateTime(value);
      if (val.isAfter(notAfter))
      {
        String errorMessage = String.format("The '%s'-attribute '%s' with value '%s' must not be after '%s'",
                                            schemaAttribute.getType(),
                                            schemaAttribute.getScimNodeName(),
                                            val,
                                            notAfter);
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
  }

  /**
   * will verify string type attributes
   *
   * @param schemaAttribute the attribute definition
   * @param value the string value to validate
   */
  private static void validateStringTypes(SchemaAttribute schemaAttribute, TextNode valueNode)
  {
    final String value = valueNode.textValue();
    schemaAttribute.getMinLength().ifPresent(minLength -> {
      if (minLength > StringUtils.length(value))
      {
        String errorMessage = String.format("The '%s'-attribute '%s' with value '%s' must have a minimum length of "
                                            + "'%s' characters but is '%s' characters long",
                                            schemaAttribute.getType(),
                                            schemaAttribute.getScimNodeName(),
                                            value,
                                            minLength,
                                            value.length());
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
    schemaAttribute.getMaxLength().ifPresent(maxLength -> {
      if (maxLength < StringUtils.length(value))
      {
        String errorMessage = String.format("The '%s'-attribute '%s' with value '%s' must not be longer than "
                                            + "'%s' characters but is '%s' characters long",
                                            schemaAttribute.getType(),
                                            schemaAttribute.getScimNodeName(),
                                            value,
                                            maxLength,
                                            value.length());
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
    schemaAttribute.getPattern().ifPresent(pattern -> {
      if (!pattern.matcher(value).matches())
      {
        String errorMessage = String.format("The '%s'-attribute '%s' with value '%s' must match the regular expression "
                                            + "of '%s'",
                                            schemaAttribute.getType(),
                                            schemaAttribute.getScimNodeName(),
                                            value,
                                            pattern);
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
  }

  /**
   * verifies that the value of this node does match its requirements from the attribute
   *
   * @param schemaAttribute the attribute definition
   * @param value the value of this node
   */
  protected static void validateNumberNode(SchemaAttribute schemaAttribute, IntNode valueNode)
  {
    validateNumberNode(schemaAttribute, new DecimalNode(valueNode.decimalValue()));
  }

  /**
   * verifies that the value of this node does match its requirements from the attribute
   *
   * @param schemaAttribute the attribute definition
   * @param value the value of this node
   */
  protected static void validateNumberNode(SchemaAttribute schemaAttribute, LongNode valueNode)
  {
    validateNumberNode(schemaAttribute, new DecimalNode(valueNode.decimalValue()));
  }

  /**
   * verifies that the value of this node does match its requirements from the attribute
   *
   * @param schemaAttribute the attribute definition
   * @param value the value of this node
   */
  protected static void validateNumberNode(SchemaAttribute schemaAttribute, DoubleNode valueNode)
  {
    validateNumberNode(schemaAttribute, new DecimalNode(valueNode.decimalValue()));
  }

  /**
   * verifies that the value of this node does match its requirements from the attribute
   *
   * @param schemaAttribute the attribute definition
   * @param value the value of this node
   */
  protected static void validateNumberNode(SchemaAttribute schemaAttribute, DecimalNode valueNode)
  {
    final boolean isInteger = schemaAttribute.getType().equals(Type.INTEGER);
    final Function<BigDecimal, String> toNumberType = (value) -> isInteger ? String.valueOf(value.longValue())
      : String.valueOf(value);

    final BigDecimal value = valueNode.decimalValue();
    schemaAttribute.getMinimum().ifPresent(minimum -> {
      if (BigDecimal.valueOf(minimum).compareTo(value) > 0)
      {
        String errorMessage = String.format("The '%s'-attribute '%s' with value '%s' must have at least a value of '%s'",
                                            schemaAttribute.getType(),
                                            schemaAttribute.getScimNodeName(),
                                            toNumberType.apply(value),
                                            toNumberType.apply(BigDecimal.valueOf(minimum)));
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
    schemaAttribute.getMaximum().ifPresent(maximum -> {
      if (BigDecimal.valueOf(maximum).compareTo(value) < 0)
      {
        String errorMessage = String.format("The '%s'-attribute '%s' with value '%s' must not be greater than '%s'",
                                            schemaAttribute.getType(),
                                            schemaAttribute.getScimNodeName(),
                                            toNumberType.apply(value),
                                            toNumberType.apply(BigDecimal.valueOf(maximum)));
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
    schemaAttribute.getMultipleOf().ifPresent(multipleOf -> {
      if (value.remainder(BigDecimal.valueOf(multipleOf)).doubleValue() != 0)
      {
        String errorMessage = String.format("The '%s'-attribute '%s' with value '%s' must be a multiple of '%s'",
                                            schemaAttribute.getType(),
                                            schemaAttribute.getScimNodeName(),
                                            toNumberType.apply(value),
                                            multipleOf);
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
  }

  /**
   * verifies that the given array node does match its requirements if requirements have been set
   *
   * @param schemaAttribute the attribute definition
   * @param arrayNode the array node that must match the requirements of the definition
   */
  public static void validateArrayNode(SchemaAttribute schemaAttribute, ArrayNode arrayNode)
  {
    if (schemaAttribute == null || arrayNode == null)
    {
      return;
    }
    schemaAttribute.getMinItems().ifPresent(minItems -> {
      if (minItems > arrayNode.size())
      {
        String errorMessage = String.format("The 'ARRAY'-attribute '%s' with value '%s' must have at least '%s' items "
                                            + "but only '%s' items are present",
                                            schemaAttribute.getScimNodeName(),
                                            arrayNode,
                                            minItems,
                                            arrayNode.size());
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
    schemaAttribute.getMaxItems().ifPresent(maxItems -> {
      if (maxItems < arrayNode.size())
      {
        String errorMessage = String.format("The 'ARRAY'-attribute '%s' with value '%s' must not have more than '%s' "
                                            + "items. Items found '%s'",
                                            schemaAttribute.getScimNodeName(),
                                            arrayNode,
                                            maxItems,
                                            arrayNode.size());
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
    });
  }
}
