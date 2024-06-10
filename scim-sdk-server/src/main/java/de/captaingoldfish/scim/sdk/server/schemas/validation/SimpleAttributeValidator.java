package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidDateTimeRepresentationException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimBinaryNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimBooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimDoubleNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimIntNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimLongNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.TimeUtils;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knüppel
 * @since 09.04.2021
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class SimpleAttributeValidator
{

  /**
   * checks if the given node is a json leaf node
   *
   * @param attribute the attribute from the document
   * @return true if the attribute is a json leaf node, false else
   */
  public static boolean isSimpleNode(JsonNode attribute)
  {
    return attribute.isNull() || (!attribute.isArray() && !attribute.isObject());
  }

  /**
   * will parse the given node type into a json representation that also holds its schema attribute definition
   */
  @SneakyThrows
  public static JsonNode parseNodeTypeAndValidate(SchemaAttribute schemaAttribute, JsonNode jsonNode)
  {
    log.trace("Validating simple attribute '{}'", schemaAttribute.getScimNodeName());
    final JsonNode attribute;
    invalidAttributeBlock: if (!isSimpleNode(jsonNode))
    {
      // fallback. If an array is used, but it contains only one simple-valued element we will still accept it as
      // simple-attribute
      if (jsonNode.isArray() && jsonNode.size() == 1 && isSimpleNode(jsonNode.get(0)))
      {
        attribute = jsonNode.get(0);
        break invalidAttributeBlock;
      }
      String errorMessage = String.format("Attribute '%s' is expected to be a simple attribute of type '%s' but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          schemaAttribute.getType(),
                                          jsonNode);
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }
    else
    {
      attribute = jsonNode;
    }
    checkCanonicalValues(schemaAttribute, attribute);

    boolean isNodeTypeValid = false;
    Supplier<JsonNode> validatedNodeSupplier;
    Type type = schemaAttribute.getType();
    switch (type)
    {
      case STRING:
        isNodeTypeValid = attribute.isTextual() || attribute.isObject();
        final String value = attribute.isTextual() ? attribute.textValue() : attribute.toString();
        validatedNodeSupplier = () -> new ScimTextNode(schemaAttribute, value);
        break;
      case BINARY:
        isNodeTypeValid = isNodeTypeBinary(schemaAttribute, attribute);
        if (attribute.isBinary())
        {
          validatedNodeSupplier = () -> new ScimBinaryNode(schemaAttribute, getBinaryValueOfJsonNode(attribute));
        }
        else
        {
          validatedNodeSupplier = () -> new ScimTextNode(schemaAttribute, attribute.textValue());
        }
        break;
      case BOOLEAN:
        isNodeTypeValid = attribute.isBoolean();
        validatedNodeSupplier = () -> new ScimBooleanNode(schemaAttribute, attribute.booleanValue());
        break;
      case INTEGER:
      {
        isNodeTypeValid = attribute.isInt() || attribute.isLong() || attribute.isBigDecimal();
        if (attribute.intValue() == attribute.longValue())
        {
          validatedNodeSupplier = () -> new ScimIntNode(schemaAttribute, attribute.intValue());
        }
        else
        {
          validatedNodeSupplier = () -> new ScimLongNode(schemaAttribute, attribute.longValue());
        }
        break;
      }
      case DECIMAL:
        isNodeTypeValid = attribute.isInt() || attribute.isLong() || attribute.isFloat() || attribute.isDouble()
                          || attribute.isBigDecimal();
        validatedNodeSupplier = () -> new ScimDoubleNode(schemaAttribute, attribute.doubleValue());
        break;
      case DATE_TIME:
        isNodeTypeValid = attribute.isTextual();
        if (isNodeTypeValid)
        {
          parseDateTime(schemaAttribute, attribute.textValue());
        }
        validatedNodeSupplier = () -> new ScimTextNode(schemaAttribute, attribute.textValue());
        break;
      default:
        isNodeTypeValid = attribute.isTextual();
        if (isNodeTypeValid)
        {
          validateValueNodeWithReferenceTypes(schemaAttribute, attribute);
        }
        validatedNodeSupplier = () -> new ScimTextNode(schemaAttribute, attribute.textValue());
    }

    invalidNodeType: if (!isNodeTypeValid)
    {
      Optional<JsonNode> fallbackNode = tryToParseFromStringValue(schemaAttribute, attribute);
      if (fallbackNode.isPresent())
      {
        validatedNodeSupplier = fallbackNode::get;
        break invalidNodeType;
      }

      final String errorMessage = String.format("Value of attribute '%s' is not of type '%s' but of type '%s' with value '%s'",
                                                schemaAttribute.getFullResourceName(),
                                                type.getValue(),
                                                StringUtils.lowerCase(attribute.getNodeType().toString()),
                                                attribute);
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }
    return validatedNodeSupplier.get();
  }

  /**
   * a fallback method. If an attribute is not of the expected type but of type string, we will try to parse it
   * into its correct type from the string-value
   *
   * @param schemaAttribute the attributes definition that will tell us what the correct type is
   * @param valueNode the node that should be parsed into another type
   * @return the parse node or the original node
   */
  private static Optional<JsonNode> tryToParseFromStringValue(SchemaAttribute schemaAttribute, JsonNode valueNode)
  {
    try
    {
      switch (schemaAttribute.getType())
      {
        case BOOLEAN:
          boolean isBoolString = valueNode.isTextual() && Arrays.asList("true", "false").contains(valueNode.textValue().toLowerCase());
          if (isBoolString)
          {
            return Optional.of(new ScimBooleanNode(schemaAttribute, Boolean.parseBoolean(valueNode.textValue().toLowerCase())));
          }
          else
          {
            return Optional.empty();
          }
        case INTEGER:
          long longValue = Long.parseLong(valueNode.textValue());
          if (longValue == (int)longValue)
          {
            return Optional.of(new ScimIntNode(schemaAttribute, (int)longValue));
          }
          else
          {
            return Optional.of(new ScimLongNode(schemaAttribute, longValue));
          }
        case DECIMAL:
          return Optional.of(new ScimDoubleNode(schemaAttribute, new BigDecimal(valueNode.textValue()).doubleValue()));
      }
    }
    catch (Exception ex)
    {
      log.trace(ex.getMessage(), ex);
    }
    return Optional.empty();
  }

  /**
   * verifies if the given attribute node contains a valid binary value
   *
   * @param schemaAttribute the attributes definition
   * @param attribute the attribute to check
   * @return true if the node is compliant with the type binary
   */
  private static boolean isNodeTypeBinary(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    if (attribute.isBinary())
    {
      return true;
    }
    if (attribute.isTextual())
    {
      try
      {
        Base64.getDecoder().decode(attribute.textValue());
        return true;
      }
      catch (IllegalArgumentException ex)
      {
        log.trace(ex.getMessage(), ex);
        log.debug(String.format("Data of attribute '%s' is not valid Base64 encoded data",
                                schemaAttribute.getFullResourceName()));
        return false;
      }
    }
    return false;
  }

  /**
   * tries to parse the given text as a xsd:datetime representation as defined in RFC7643 chapter 2.3.5
   */
  private static void parseDateTime(SchemaAttribute schemaAttribute, String textValue)
  {
    try
    {
      TimeUtils.parseDateTime(textValue);
    }
    catch (InvalidDateTimeRepresentationException ex)
    {
      throw new AttributeValidationException(schemaAttribute,
                                             String.format("Given value is not a valid dateTime '%s'", textValue));
    }
  }

  /**
   * validates a simple value node against the valid resource types defined in the meta schema
   *
   * @param schemaAttribute the meta attribute definition
   * @param valueNode the value node
   */
  private static void validateValueNodeWithReferenceTypes(SchemaAttribute schemaAttribute, JsonNode valueNode)
  {
    boolean isValidReferenceType = false;
    for ( ReferenceTypes referenceType : schemaAttribute.getReferenceTypes() )
    {
      switch (referenceType)
      {
        case RESOURCE:
        case URI:
          isValidReferenceType = parseUri(valueNode.textValue());
          break;
        case URL:
          isValidReferenceType = parseUrl(valueNode.textValue());
          break;
        default:
          isValidReferenceType = true;
      }
      if (isValidReferenceType)
      {
        break;
      }
    }
    if (!isValidReferenceType)
    {
      String errorMessage = String.format("Attribute '%s' is a referenceType and must apply to one of the following "
                                          + "types '%s' but value is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          schemaAttribute.getReferenceTypes(),
                                          valueNode.textValue());
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }
  }

  /**
   * tries to parse the given text into a URL
   */
  private static boolean parseUrl(String textValue)
  {
    try
    {
      new URL(textValue);
      return true;
    }
    catch (MalformedURLException ex)
    {
      log.debug(ex.getMessage());
      return false;
    }
  }

  /**
   * tries to parse the given text into a URI
   */
  private static boolean parseUri(String textValue)
  {
    try
    {
      new URI(textValue);
      return true;
    }
    catch (URISyntaxException ex)
    {
      log.debug(ex.getMessage());
      return false;
    }
  }

  /**
   * will verify that the current value node does define one of the canonical values of the attribute definition
   * if some are defined
   *
   * @param schemaAttribute the attribute definition from the meta schema
   * @param valueNode the value that matches to this definition
   */
  protected static void checkCanonicalValues(SchemaAttribute schemaAttribute, JsonNode valueNode)
  {
    if (schemaAttribute.getCanonicalValues().isEmpty())
    {
      // all values are valid
      return;
    }
    final String value = valueNode.textValue();
    AtomicBoolean caseInsensitiveMatch = new AtomicBoolean(false);
    Predicate<String> compare = s -> {
      if (schemaAttribute.isCaseExact())
      {
        caseInsensitiveMatch.compareAndSet(false, StringUtils.equalsIgnoreCase(s, value));
        return StringUtils.equals(s, value);
      }
      else
      {
        return StringUtils.equalsIgnoreCase(s, value);
      }
    };

    if (schemaAttribute.getCanonicalValues().stream().noneMatch(compare))
    {
      String errorMessage;
      if (schemaAttribute.isCaseExact() && caseInsensitiveMatch.get())
      {
        errorMessage = String.format("Attribute '%s' is caseExact and does not match its canonicalValues "
                                     + "'%s' actual value is '%s'",
                                     schemaAttribute.getFullResourceName(),
                                     schemaAttribute.getCanonicalValues(),
                                     value);
      }
      else
      {
        errorMessage = String.format("Attribute '%s' does not match one of its canonicalValues "
                                     + "'%s' actual value is '%s'",
                                     schemaAttribute.getFullResourceName(),
                                     schemaAttribute.getCanonicalValues(),
                                     value);
      }
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }
  }

  /**
   * simple method to wrap away the checked exception
   */
  @SneakyThrows
  private static byte[] getBinaryValueOfJsonNode(JsonNode attribute)
  {
    return attribute.binaryValue();
  }
}
