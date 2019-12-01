package de.captaingoldfish.scim.sdk.common.utils;

import java.math.BigDecimal;
import java.time.Instant;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 30.11.2019 - 22:48 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttributeValidator
{

  /**
   * validates if the given value matches the requirements of the schema attribute
   *
   * @param schemaAttribute the attribute definition
   * @param value the value that should be checked
   */
  public static void validateTextNode(SchemaAttribute schemaAttribute, String value)
  {
    if (schemaAttribute == null)
    {
      return;
    }
    if (Type.STRING.equals(schemaAttribute.getType()) || Type.REFERENCE.equals(schemaAttribute.getType()))
    {
      validateStringTypes(schemaAttribute, value);
    }
    else if (Type.DATE_TIME.equals(schemaAttribute.getType()))
    {
      validateDateTimeTypes(schemaAttribute, value);
    }
  }

  /**
   * will verify dateTime type attributes
   *
   * @param schemaAttribute the attribute definition
   * @param value the dateTime value to validate
   */
  private static void validateDateTimeTypes(SchemaAttribute schemaAttribute, String value)
  {
    schemaAttribute.getNotBefore().ifPresent(notBefore -> {
      Instant val = TimeUtils.parseDateTime(value);
      if (val.isBefore(notBefore))
      {
        throw new DocumentValidationException("the attribute '" + schemaAttribute.getScimNodeName()
                                              + "' must not be before " + "'" + notBefore.toString() + "' but was '"
                                              + val.toString() + "'", HttpStatus.BAD_REQUEST,
                                              ScimType.RFC7644.INVALID_VALUE);
      }
    });
    schemaAttribute.getNotAfter().ifPresent(notAfter -> {
      Instant val = TimeUtils.parseDateTime(value);
      if (val.isAfter(notAfter))
      {
        throw new DocumentValidationException("the attribute '" + schemaAttribute.getScimNodeName()
                                              + "' must not be after " + "'" + notAfter.toString() + "' but was '"
                                              + val.toString() + "'", HttpStatus.BAD_REQUEST,
                                              ScimType.RFC7644.INVALID_VALUE);
      }
    });
  }

  /**
   * will verify string type attributes
   *
   * @param schemaAttribute the attribute definition
   * @param value the string value to validate
   */
  private static void validateStringTypes(SchemaAttribute schemaAttribute, String value)
  {
    schemaAttribute.getMinLength().ifPresent(minLength -> {
      if (minLength > StringUtils.length(value))
      {
        throw new DocumentValidationException("the attribute '" + schemaAttribute.getScimNodeName() + "' has a "
                                              + "minimum length of " + minLength + " characters but value is '" + value
                                              + "'", HttpStatus.BAD_REQUEST, ScimType.RFC7644.INVALID_VALUE);
      }
    });
    schemaAttribute.getMaxLength().ifPresent(maxLength -> {
      if (maxLength < StringUtils.length(value))
      {
        throw new DocumentValidationException("the attribute '" + schemaAttribute.getScimNodeName() + "' has a "
                                              + "maximum length of " + maxLength + " characters but value is '" + value
                                              + "'", HttpStatus.BAD_REQUEST, ScimType.RFC7644.INVALID_VALUE);
      }
    });
    schemaAttribute.getPattern().ifPresent(pattern -> {
      if (!pattern.matcher(value).matches())
      {
        throw new DocumentValidationException("the attribute '" + schemaAttribute.getScimNodeName()
                                              + "' must match the regular expression '" + pattern.pattern()
                                              + "' but value is '" + value + "'", HttpStatus.BAD_REQUEST,
                                              ScimType.RFC7644.INVALID_VALUE);
      }
    });
  }

  /**
   * verifies that the value of this node does match its requirements from the attribute
   *
   * @param schemaAttribute the attribute definition
   * @param value the value of this node
   */
  public static void validateNumberNode(SchemaAttribute schemaAttribute, double value)
  {
    if (schemaAttribute == null)
    {
      return;
    }
    schemaAttribute.getMinimum().ifPresent(minimum -> {
      if (minimum > value)
      {
        throw new DocumentValidationException("the attribute '" + schemaAttribute.getScimNodeName() + "' has a "
                                              + "minimum value of " + minimum + " but value is '" + value + "'",
                                              HttpStatus.BAD_REQUEST, ScimType.RFC7644.INVALID_VALUE);
      }
    });
    schemaAttribute.getMaximum().ifPresent(maximum -> {
      if (maximum < value)
      {
        throw new DocumentValidationException("the attribute '" + schemaAttribute.getScimNodeName() + "' has a "
                                              + "maximum value of " + maximum + " but value is '" + value + "'",
                                              HttpStatus.BAD_REQUEST, ScimType.RFC7644.INVALID_VALUE);
      }
    });
    schemaAttribute.getMultipleOf().ifPresent(multipleOf -> {
      if (BigDecimal.valueOf(value).remainder(BigDecimal.valueOf(multipleOf)).doubleValue() != 0)
      {
        throw new DocumentValidationException("the attribute '" + schemaAttribute.getScimNodeName() + "' must be "
                                              + "multiple of " + multipleOf + " but value is '" + value + "'",
                                              HttpStatus.BAD_REQUEST, ScimType.RFC7644.INVALID_VALUE);
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
    if (schemaAttribute == null)
    {
      return;
    }
    schemaAttribute.getMinItems().ifPresent(minItems -> {
      if (minItems > arrayNode.size())
      {
        throw new DocumentValidationException("the multivalued attribute '" + schemaAttribute.getScimNodeName() + "' "
                                              + "must have at least " + minItems + " items but array has "
                                              + arrayNode.size() + " items and is: " + arrayNode.toString(),
                                              HttpStatus.BAD_REQUEST, ScimType.RFC7644.INVALID_VALUE);
      }
    });
    schemaAttribute.getMaxItems().ifPresent(maxItems -> {
      if (maxItems < arrayNode.size())
      {
        throw new DocumentValidationException("the multivalued attribute '" + schemaAttribute.getScimNodeName() + "' "
                                              + "must not have more than " + maxItems + " items but array has "
                                              + arrayNode.size() + " items and is: " + arrayNode.toString(),
                                              HttpStatus.BAD_REQUEST, ScimType.RFC7644.INVALID_VALUE);
      }
    });
  }
}
