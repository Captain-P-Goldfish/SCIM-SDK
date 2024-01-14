package de.captaingoldfish.scim.sdk.server.utils;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.resources.base.ScimBooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimDoubleNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimIntNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimLongNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.SneakyThrows;


/**
 * @author Pascal Knueppel
 * @since 06.01.2024
 */
public interface ScimAttributeHelper
{

  /**
   * converts the given JsonNode into the corresponding javaType value
   *
   * @param schemaAttribute the SCIM definition of the attribute
   * @param jsonNode the JSON representation of the attribute
   * @return the java value of the json-node if possible or the json-node itself
   */
  @SneakyThrows
  default Object getValueOfJsonNode(SchemaAttribute schemaAttribute, JsonNode jsonNode)
  {
    Object object;
    switch (schemaAttribute.getType())
    {
      case COMPLEX:
      case ANY:
        object = jsonNode;
        break;
      case INTEGER:
        object = Optional.ofNullable(jsonNode.numberValue()).map(Number::longValue).orElse(null);
        break;
      case DECIMAL:
        object = Optional.ofNullable(jsonNode.numberValue()).map(Number::doubleValue).orElse(null);
        break;
      case BOOLEAN:
        object = jsonNode.isBoolean() ? jsonNode.booleanValue() : null;
        break;
      default:
        object = jsonNode.textValue();
    }
    return object;
  }

  /**
   * creates a new json node with the given value
   *
   * @param schemaAttribute the attribute schema definition
   * @param value the value that should be added into the node
   * @return the simple json node
   */
  default JsonNode parseToJsonNode(SchemaAttribute schemaAttribute, String value)
  {
    switch (schemaAttribute.getType())
    {
      case COMPLEX:
        return JsonHelper.readJsonDocument(value);
      case STRING:
      case DATE_TIME:
      case REFERENCE:
      case BINARY:
        return new ScimTextNode(schemaAttribute, value);
      case BOOLEAN:
        return new ScimBooleanNode(schemaAttribute, Boolean.parseBoolean(value));
      case INTEGER:
        Long longVal = Long.parseLong(value);
        if (longVal == longVal.intValue())
        {
          return new ScimIntNode(schemaAttribute, longVal.intValue());
        }
        else
        {
          return new ScimLongNode(schemaAttribute, longVal);
        }
      default:
        return new ScimDoubleNode(schemaAttribute, Double.parseDouble(value));
    }
  }
}
