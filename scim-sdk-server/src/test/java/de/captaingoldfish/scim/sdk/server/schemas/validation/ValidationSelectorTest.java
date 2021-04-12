package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

  /**
   * verifies that the {@link ValidationSelector} validates successfully a complex attribute if the
   * {@link ContextValidator} returns true and the given attribute definition and its attribute are complex
   */
  @Test
  public void testValidateComplexAttribute()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder().name("age").type(Type.INTEGER).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    ObjectNode attribute = new ObjectNode(JsonNodeFactory.instance);
    attribute.set("firstname", new TextNode("Captain"));
    attribute.set("lastname", new TextNode("Goldfish"));
    attribute.set("age", new IntNode(35));

    Optional<JsonNode> validatedNode = ValidationSelector.validateNode(schemaAttribute, attribute, contextValidator);
    Assertions.assertTrue(validatedNode.isPresent());
    Assertions.assertEquals(attribute, validatedNode.get());
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "complex",
   *      "multiValued": true
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [
   *        {
   *           "firstname": "goldfish",
   *           "primary": true
   *        },
   *        {
   *           "firstname": "banjo",
   *           "primary": false
   *        },
   *        {
   *           "firstname": "kazooie"
   *        }
   *      ]
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidation()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute primaryAttribute = SchemaAttributeBuilder.builder().name("primary").type(Type.BOOLEAN).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .subAttributes(firstnameAttribute, primaryAttribute)
                                                            .build();

    // one of the primary values is not present and therefore null. jsonNode != null protects from NullPointer
    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> jsonNode != null;
    ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
    element.set("firstname", new TextNode("goldfish"));
    element.set("primary", BooleanNode.getTrue());
    attribute.add(element);

    ObjectNode element2 = new ObjectNode(JsonNodeFactory.instance);
    element2.set("firstname", new TextNode("banjo"));
    element2.set("primary", BooleanNode.getFalse());
    attribute.add(element2);

    ObjectNode element3 = new ObjectNode(JsonNodeFactory.instance);
    element3.set("firstname", new TextNode("kazooie"));
    attribute.add(element3);

    Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
      return ValidationSelector.validateNode(schemaAttribute, attribute, contextValidator);
    });
    Assertions.assertTrue(validatedNode.isPresent());
    Assertions.assertEquals(attribute, validatedNode.get());
  }
}
