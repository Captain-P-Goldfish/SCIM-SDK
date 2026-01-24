package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimBigIntegerNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimBooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimDecimalNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * This class handles default values of the {@link de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute}
 * implementation. This is a custom feature not defined by SCIM.
 *
 * @author Pascal Knueppel
 * @since 11.11.2023
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DefaultValueHandler
{

  /**
   * gets the original node or the configured default value if a default value is configured
   *
   * @param schemaAttribute the attributes definition that might contain a default value
   * @param attribute the attribute that belongs to the given attribute-definition
   * @return either the attribute from the parameter-list or the default value defined in the schema-attribute
   */
  public static JsonNode getOrGetDefault(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    final String defaultValue = schemaAttribute.getDefaultValue();
    final boolean isDefaultValueAssigned = StringUtils.isNotBlank(defaultValue);
    if (!isDefaultValueAssigned)
    {
      return attribute;
    }
    if (attribute != null && !attribute.isNull())
    {
      // attribute is set, so we do net need to set a default value
      return attribute;
    }

    JsonNode defaultNode = toJsonNode(schemaAttribute, defaultValue);
    return defaultNode;
  }

  /**
   * parses the default value of the {@link SchemaAttribute} into the appropriate json-node-type
   *
   * @param schemaAttribute the attributes definition
   * @param defaultValue the default value in string representation
   * @return the json node representation of the default-value
   */
  private static JsonNode toJsonNode(SchemaAttribute schemaAttribute, String defaultValue)
  {
    return toJsonNode(schemaAttribute, defaultValue, true);
  }

  /**
   * parses the default value of the {@link SchemaAttribute} into the appropriate json-node-type
   *
   * @param schemaAttribute the attributes definition
   * @param defaultValue the default value in string representation
   * @param allowArray whether to allow array processing
   * @return the json node representation of the default-value
   */
  private static JsonNode toJsonNode(SchemaAttribute schemaAttribute, String defaultValue, boolean allowArray)
  {
    switch (schemaAttribute.getType())
    {
      case BOOLEAN:
        return toArrayOrDefault(schemaAttribute,
                                defaultValue,
                                allowArray,
                                () -> new ScimBooleanNode(schemaAttribute, Boolean.parseBoolean(defaultValue)));
      case INTEGER:
        return toArrayOrDefault(schemaAttribute,
                                defaultValue,
                                allowArray,
                                () -> new ScimBigIntegerNode(schemaAttribute, new BigInteger(defaultValue)));
      case DECIMAL:
        return toArrayOrDefault(schemaAttribute,
                                defaultValue,
                                allowArray,
                                () -> new ScimDecimalNode(schemaAttribute, new BigDecimal(defaultValue)));
      case STRING:
      case REFERENCE:
      case DATE_TIME:
      {
        return toArrayOrDefault(schemaAttribute,
                                defaultValue,
                                allowArray,
                                () -> new ScimTextNode(schemaAttribute, defaultValue));
      }
      case COMPLEX:
      {
        return toArrayOrDefault(schemaAttribute, defaultValue, allowArray, () -> {
          try
          {
            JsonNode jsonNode = JsonHelper.readJsonDocument(defaultValue);
            if (jsonNode != null && jsonNode.isObject())
            {
              return jsonNode;
            }
            throw new IllegalArgumentException("Default value is not of type object");
          }
          catch (Exception ex)
          {
            throw new InternalServerException(String.format("Invalid configuration. Default value '%s' is not of type object.",
                                                            defaultValue),
                                              ex);
          }
        });
      }
      default:
        throw new InternalServerException(String.format("Invalid configuration. Default values are only supported for "
                                                        + "the following types: %s",
                                                        Arrays.asList(Type.BOOLEAN,
                                                                      Type.INTEGER,
                                                                      Type.DECIMAL,
                                                                      Type.STRING,
                                                                      Type.REFERENCE,
                                                                      Type.DATE_TIME,
                                                                      Type.COMPLEX)));
    }
  }

  private static JsonNode toArrayOrDefault(SchemaAttribute schemaAttribute,
                                           String defaultValue,
                                           boolean allowArray,
                                           Supplier<JsonNode> defaultNode)
  {
    if (!schemaAttribute.isMultiValued() || !allowArray)
    {
      return defaultNode.get();
    }
    ScimArrayNode scimArrayNode = new ScimArrayNode(schemaAttribute);
    if (defaultValue.startsWith("["))
    {
      try
      {
        JsonNode jsonNode = new ObjectMapper().readTree(defaultValue);
        if (jsonNode.isArray())
        {
          for ( JsonNode node : jsonNode )
          {
            try
            {
              String elementValue = node.isContainerNode() ? node.toString() : node.asText();
              scimArrayNode.add(toJsonNode(schemaAttribute, elementValue, false));
            }
            catch (Exception ex)
            {
              log.debug("Skipping invalid element '{}' in default value array for attribute '{}'",
                        node.asText(),
                        schemaAttribute.getName(),
                        ex);
            }
          }
          return scimArrayNode;
        }
      }
      catch (JsonProcessingException e)
      {
        log.trace("DefaultValue '{}' does not seem to be an array. Translating to single element", defaultValue, e);
      }
    }
    scimArrayNode.add(defaultNode.get());
    return scimArrayNode;
  }
}
