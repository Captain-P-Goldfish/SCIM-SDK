package de.captaingoldfish.scim.sdk.server.schemas.validation;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import de.captaingoldfish.scim.sdk.server.utils.SchemaAttributeBuilder;


/**
 * @author Pascal Knueppel
 * @since 12.04.2021
 */
public class MultivaluedComplexAttributeValidatorTest
{


  /**
   * verifies that an object is successfully validated as multivalued complex type since it is interpreted as an
   * array of length 1
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
   *      "array": {
   *        ...
   *      }
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithANoneArraySingleObjectAttribute()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder().name("age").type(Type.INTEGER).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    ObjectNode attribute = new ObjectNode(JsonNodeFactory.instance);
    attribute.set("firstname", new TextNode("Captain"));
    attribute.set("lastname", new TextNode("Goldfish"));
    attribute.set("age", new IntNode(35));

    JsonNode validatedNode = MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute,
                                                                                attribute,
                                                                                contextValidator);
    Assertions.assertNotNull(validatedNode);
    Assertions.assertEquals(1, validatedNode.size());
    Assertions.assertEquals(attribute, validatedNode.get(0));
  }

  /**
   * verifies that an array of length 1 is successfully validated as multivalued complex
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
   *          ...
   *        }
   *      ]
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithSingleElement()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder().name("age").type(Type.INTEGER).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
    element.set("firstname", new TextNode("Captain"));
    element.set("lastname", new TextNode("Goldfish"));
    element.set("age", new IntNode(35));
    attribute.add(element);

    JsonNode validatedNode = MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute,
                                                                                attribute,
                                                                                contextValidator);
    Assertions.assertNotNull(validatedNode);
    Assertions.assertEquals(1, validatedNode.size());
    Assertions.assertEquals(attribute, validatedNode);
  }

  /**
   * tests the following structure where element 1 and 2 of the array are identical
   *
   * <pre>
   *    {
   *      "type": "complex",
   *      "multiValued": true,
   *      "uniqueness": none
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [
   *        {
   *          ...
   *        },
   *        {
   *          ...
   *        }
   *      ]
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithDuplicateElementsAndNoUniqueness()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder().name("age").type(Type.INTEGER).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .uniqueness(Uniqueness.NONE)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
    element.set("firstname", new TextNode("Captain"));
    element.set("lastname", new TextNode("Goldfish"));
    element.set("age", new IntNode(35));
    attribute.add(element);
    attribute.add(element);

    JsonNode validatedNode = MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute,
                                                                                attribute,
                                                                                contextValidator);
    Assertions.assertNotNull(validatedNode);
    Assertions.assertEquals(2, validatedNode.size());
    Assertions.assertEquals(attribute, validatedNode);
  }

  /**
   * tests the following structure where element 1 and 2 of the array are identical
   *
   * <pre>
   *    {
   *      "type": "complex",
   *      "multiValued": true,
   *      "uniqueness": server|global
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [
   *        {
   *          ...
   *        },
   *        {
   *          ...
   *        }
   *      ]
   *    }
   * </pre>
   */
  @ParameterizedTest
  @ValueSource(strings = {"SERVER", "GLOBAL"})
  public void testComplexMultivaluedValidationWithDuplicateElementsAndUniqneness(Uniqueness uniqueness)
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder().name("age").type(Type.INTEGER).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .uniqueness(uniqueness)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
    element.set("firstname", new TextNode("Captain"));
    element.set("lastname", new TextNode("Goldfish"));
    element.set("age", new IntNode(35));
    attribute.add(element);
    attribute.add(element);

    try
    {
      MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute, attribute, contextValidator);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Array with uniqueness '%s' contains duplicate values '%s'",
                                          schemaAttribute.getUniqueness().getValue(),
                                          attribute);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
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
   *          ...
   *        },
   *        "I am an illegal value"
   *      ]
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithIllegalSimpleAttributeElement()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder().name("age").type(Type.INTEGER).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
    element.set("firstname", new TextNode("Captain"));
    element.set("lastname", new TextNode("Goldfish"));
    element.set("age", new IntNode(35));
    attribute.add(element);
    attribute.add(new TextNode("I am an illegal value"));

    try
    {
      MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute, attribute, contextValidator);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Attribute '%s' is expected to hold only complex attributes but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          attribute);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
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
   *      "array": "I am an illegal value"
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithSimpleAttributeAsArray()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder().name("age").type(Type.INTEGER).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    TextNode attribute = new TextNode("I am an illegal value");

    try
    {
      MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute, attribute, contextValidator);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Attribute '%s' is expected to be an array but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          attribute);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
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
   *           "lastname": true  // <==== illegal value
   *        }
   *      ]
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithIllegalSubelement()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder().name("age").type(Type.INTEGER).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
    element.set("firstname", new TextNode("Captain"));
    JsonNode illegalNode = BooleanNode.getTrue();
    element.set("lastname", illegalNode);
    attribute.add(element);

    try
    {
      MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute, attribute, contextValidator);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String errorMessage = String.format("Found unsupported value in multivalued complex attribute '%s'", attribute);
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
      {
        AttributeValidationException cause = (AttributeValidationException)ex.getCause();
        Assertions.assertEquals(lastnameAttribute, cause.getSchemaAttribute());
        String errorMessage = String.format("Value of attribute '%s' is not of type '%s' but of type '%s' with value '%s'",
                                            lastnameAttribute.getFullResourceName(),
                                            lastnameAttribute.getType().getValue(),
                                            StringUtils.lowerCase(illegalNode.getNodeType().toString()),
                                            illegalNode);
        Assertions.assertEquals(errorMessage, cause.getMessage());
      }
    }
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
   *           "primary": true
   *        }
   *      ]
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithDuplicatePrimaryElements()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute primaryAttribute = SchemaAttributeBuilder.builder().name("primary").type(Type.BOOLEAN).build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .subAttributes(firstnameAttribute, primaryAttribute)
                                                            .build();

    ContextValidator contextValidator = (attributeDefinition, jsonNode) -> true;
    ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
    element.set("firstname", new TextNode("goldfish"));
    element.set("primary", BooleanNode.getTrue());
    attribute.add(element);

    ObjectNode element2 = new ObjectNode(JsonNodeFactory.instance);
    element2.set("firstname", new TextNode("banjo"));
    element2.set("primary", BooleanNode.getTrue());
    attribute.add(element2);

    try
    {
      MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute, attribute, contextValidator);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String errorMessage = String.format("Attribute '%s' has at least two primary values but only one primary "
                                            + "is allowed '%s'",
                                            schemaAttribute.getFullResourceName(),
                                            attribute);
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
    }
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
  public void testComplexMultivaluedValidationWithSinglePrimaryValue()
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

    ScimArrayNode validatedNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute, attribute, contextValidator);
    });
    Assertions.assertEquals(attribute, validatedNode);
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
   *      "array": []
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithEmptyArray()
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

    ScimArrayNode validatedNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute, attribute, contextValidator);
    });
    Assertions.assertNull(validatedNode);
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
   *      "array": null
   *    }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithNullNode()
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

    ScimArrayNode validatedNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute,
                                                                NullNode.getInstance(),
                                                                contextValidator);
    });
    Assertions.assertNull(validatedNode);
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
   * {
   *
   * }
   * </pre>
   */
  @Test
  public void testComplexMultivaluedValidationWithNull()
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

    ScimArrayNode validatedNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute, null, contextValidator);
    });
    Assertions.assertNull(validatedNode);
  }
}
