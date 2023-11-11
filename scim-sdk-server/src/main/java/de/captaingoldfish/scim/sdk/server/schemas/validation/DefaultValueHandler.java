package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimBooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimDoubleNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimIntNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
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
    if (!isDefaultValueAssigned
        || Type.COMPLEX.equals(schemaAttribute.getType())/* default values on complex types are not supported */)
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
    switch (schemaAttribute.getType())
    {
      case BOOLEAN:
        return new ScimBooleanNode(schemaAttribute, Boolean.parseBoolean(defaultValue));
      case INTEGER:
        return new ScimIntNode(schemaAttribute, Integer.parseInt(defaultValue));
      case DECIMAL:
        return new ScimDoubleNode(schemaAttribute, Double.parseDouble(defaultValue));
      case STRING:
      case REFERENCE:
      case DATE_TIME:
        return new ScimTextNode(schemaAttribute, defaultValue);
      default:
        throw new InternalServerException(String.format("Invalid configuration. Default values are only supported for "
                                                        + "the following types: %s",
                                                        Arrays.asList(Type.BOOLEAN,
                                                                      Type.INTEGER,
                                                                      Type.DECIMAL,
                                                                      Type.STRING,
                                                                      Type.REFERENCE,
                                                                      Type.DATE_TIME)));
    }
  }

}
