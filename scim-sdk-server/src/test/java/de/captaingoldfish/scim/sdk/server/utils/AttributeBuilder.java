package de.captaingoldfish.scim.sdk.server.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
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
    return build(null, type, value);
  }

  public static JsonNode build(SchemaAttribute schemaAttribute, Type type, String value)
  {
    switch (type)
    {
      case INTEGER:
        return new BigIntegerNode(new BigInteger(value));
      case DECIMAL:
        return new DecimalNode(new BigDecimal(value));
      case BOOLEAN:
        return BooleanNode.valueOf(Boolean.parseBoolean(value));
      case BINARY:
        return BinaryNode.valueOf(value.getBytes(StandardCharsets.UTF_8));
      case COMPLEX:
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        final String attributeName = Optional.ofNullable(schemaAttribute).map(SchemaAttribute::getName).orElse("key");
        objectNode.set(attributeName, new TextNode(value));
        return objectNode;
      default:
        return new TextNode(value);
    }
  }

}
