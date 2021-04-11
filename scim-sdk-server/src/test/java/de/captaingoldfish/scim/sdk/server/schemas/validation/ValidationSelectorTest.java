package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.utils.SchemaAttributeBuilder;


/**
 * @author Pascal Knueppel
 * @since 11.04.2021
 */
public class ValidationSelectorTest
{

  /**
   * verifies that the {@link ValidationSelector} returns an empty if the {@link ContextValidator} returns false
   */
  @Test
  public void testOnContextValidationFalseReturnEmpty()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").build();
    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> false;
    Optional<JsonNode> validatedNode = ValidationSelector.validateNode(schemaAttribute, null, contextValidator);
    Assertions.assertFalse(validatedNode.isPresent());
  }

  /**
   * verifies that the {@link ValidationSelector} validates successfully a simple attribute if the
   * {@link ContextValidator} returns true and the given attribute definition and its attribute are simple
   */
  @Test
  public void testValidateSimpleAttribute()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").build();
    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    JsonNode attribute = new TextNode("hello world");
    Optional<JsonNode> validatedNode = ValidationSelector.validateNode(schemaAttribute, attribute, contextValidator);
    Assertions.assertTrue(validatedNode.isPresent());
  }

  /**
   * verifies that the {@link ValidationSelector} validates successfully a simple multivalued attribute if the
   * {@link ContextValidator} returns true and the given attribute definition and its attribute is simple and
   * multivalued
   */
  @Test
  public void testValidateSimpleArrayAttribute()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.INTEGER)
                                                            .multivalued(true)
                                                            .build();
    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
    for ( Integer integer : Arrays.asList(1, 2, 3, 4, 5, 6) )
    {
      attribute.add(new IntNode(integer));
    }
    Optional<JsonNode> validatedNode = ValidationSelector.validateNode(schemaAttribute, attribute, contextValidator);
    Assertions.assertTrue(validatedNode.isPresent());
  }


}
