package de.captaingoldfish.scim.sdk.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 11.04.2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AttributeBuilder
{

  public static JsonNode build(Type type, String value)
  {
    switch (type)
    {
      case INTEGER:
        return new IntNode(Integer.parseInt(value));
      case DECIMAL:
        return new DoubleNode(Double.parseDouble(value));
      case BOOLEAN:
        return BooleanNode.valueOf(Boolean.parseBoolean(value));
      case COMPLEX:
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.set("key", new TextNode(value));
        return objectNode;
      default:
        return new TextNode(value == null ? null : String.valueOf(value));
    }
  }

}
