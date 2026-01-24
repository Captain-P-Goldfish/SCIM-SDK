package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.math.BigInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import de.captaingoldfish.scim.sdk.server.utils.SchemaAttributeBuilder;


/**
 * @author Pascal Knueppel
 * @since 11.04.2021
 */
public class ComplexAttributeValidatorTest
{

  /**
   * verifies that all attributes of a complex attribute are successfully validated if the attribute definitions
   * do match their nodes
   */
  @Test
  public void testComplexValidation()
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

    ContextValidator contextValidator = new ContextValidator(ServiceProvider.builder().build(),
                                                             ContextValidator.ValidationContextType.REQUEST)
    {

      @Override
      public boolean validateContext(SchemaAttribute schemaAttribute, JsonNode jsonNode)
        throws AttributeValidationException
      {
        return true;
      }
    };
    ObjectNode attribute = new ObjectNode(JsonNodeFactory.instance);
    attribute.set("firstname", new TextNode("Captain"));
    attribute.set("lastname", new TextNode("Goldfish"));
    attribute.set("age", new BigIntegerNode(BigInteger.valueOf(35)));

    JsonNode validatedNode = ComplexAttributeValidator.parseNodeTypeAndValidate(schemaAttribute,
                                                                                attribute,
                                                                                contextValidator);
    Assertions.assertNotNull(validatedNode);
    Assertions.assertEquals(attribute, validatedNode);
  }

  /**
   * verifies that readOnly attributes are removed from a complex structure if the {@link ContextValidator}
   * demands so
   */
  @Test
  public void testComplexValidationWithIgnoringReadOnlyAttributes()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder()
                                                         .name("age")
                                                         .type(Type.INTEGER)
                                                         .mutability(Mutability.READ_ONLY)
                                                         .build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = new ContextValidator(ServiceProvider.builder().build(),
                                                             ContextValidator.ValidationContextType.REQUEST)
    {

      @Override
      public boolean validateContext(SchemaAttribute schemaAttribute, JsonNode jsonNode)
        throws AttributeValidationException
      {
        return !Mutability.READ_ONLY.equals(schemaAttribute.getMutability());
      }
    };
    ObjectNode attribute = new ObjectNode(JsonNodeFactory.instance);
    attribute.set("firstname", new TextNode("Captain"));
    attribute.set("lastname", new TextNode("Goldfish"));
    attribute.set("age", new IntNode(35));

    JsonNode validatedNode = ComplexAttributeValidator.parseNodeTypeAndValidate(schemaAttribute,
                                                                                attribute,
                                                                                contextValidator);
    Assertions.assertNotNull(validatedNode);
    Assertions.assertNotEquals(attribute, validatedNode);
    // now remove the read-only attribute from the original request and compare again. Objects should be equal now
    attribute.remove("age");
    Assertions.assertEquals(attribute, validatedNode);
  }

  /**
   * verifies that the {@link ComplexAttributeValidator} returns null if all sub-attributes have been removed
   * during validation
   */
  @Test
  public void testComplexValidationWithAllAttributesIgnored()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder()
                                                               .name("firstname")
                                                               .type(Type.STRING)
                                                               .mutability(Mutability.READ_ONLY)
                                                               .build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder()
                                                              .name("lastname")
                                                              .type(Type.STRING)
                                                              .mutability(Mutability.READ_ONLY)
                                                              .build();
    SchemaAttribute ageAttribute = SchemaAttributeBuilder.builder()
                                                         .name("age")
                                                         .type(Type.INTEGER)
                                                         .mutability(Mutability.READ_ONLY)
                                                         .build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .subAttributes(firstnameAttribute,
                                                                           lastnameAttribute,
                                                                           ageAttribute)
                                                            .build();

    ContextValidator contextValidator = new ContextValidator(ServiceProvider.builder().build(),
                                                             ContextValidator.ValidationContextType.REQUEST)
    {

      @Override
      public boolean validateContext(SchemaAttribute schemaAttribute, JsonNode jsonNode)
        throws AttributeValidationException
      {
        return !Mutability.READ_ONLY.equals(schemaAttribute.getMutability());
      }
    };
    ObjectNode attribute = new ObjectNode(JsonNodeFactory.instance);
    attribute.set("firstname", new TextNode("Captain"));
    attribute.set("lastname", new TextNode("Goldfish"));
    attribute.set("age", new IntNode(35));

    JsonNode validatedNode = ComplexAttributeValidator.parseNodeTypeAndValidate(schemaAttribute,
                                                                                attribute,
                                                                                contextValidator);
    // all attributes in the complex attribute are readOnly so the returned complex attribute must be empty.
    Assertions.assertNull(validatedNode);
  }

  /**
   * verifies that an {@link AttributeValidationException} is thrown if the given node for the complex attribute
   * definition is not a complex node but a simple node
   */
  @Test
  public void testComplexValidationNoneComplexObject()
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

    ContextValidator contextValidator = new ContextValidator(ServiceProvider.builder().build(),
                                                             ContextValidator.ValidationContextType.REQUEST)
    {

      @Override
      public boolean validateContext(SchemaAttribute schemaAttribute, JsonNode jsonNode)
        throws AttributeValidationException
      {
        return true;
      }
    };
    TextNode attribute = new TextNode("Captain");

    try
    {
      ComplexAttributeValidator.parseNodeTypeAndValidate(schemaAttribute, attribute, contextValidator);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Attribute '%s' must be of type object but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          attribute);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }
}
