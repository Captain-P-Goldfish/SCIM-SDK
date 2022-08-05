package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import de.captaingoldfish.scim.sdk.server.utils.AttributeBuilder;
import de.captaingoldfish.scim.sdk.server.utils.SchemaAttributeBuilder;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 11.04.2021
 */
@Slf4j
public class RequestAttributeValidatorTest
{


  /**
   * test validating a nested complex object. This is actually forbidden by RFC7643 but this should be a
   * restriction based on the service providers decisions when creating the schema.
   *
   * <pre>
   *    {
   *      "type": "complex",
   *      "required": true
   *      "subAttributes": [
   *        {
   *          "name": "firstname",
   *          "type": "string",
   *          ...
   *        },
   *        {
   *          "name": "nested",
   *          "type": "complex",
   *          "subAttributes": [
   *              "name": "lastname",
   *              "type": "string",
   *              ...
   *          ]
   *          ...
   *
   *        }
   *      ]
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   * {
   *   "object": {
   *     "firstname": "captain"
   *     "nested": {
   *       "lastname": "goldfish"
   *     }
   *   }
   * }
   * </pre>
   */
  @Test
  public void testNestedComplexValidation()
  {
    SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
    SchemaAttribute lastnameAttribute = SchemaAttributeBuilder.builder().name("lastname").type(Type.STRING).build();
    SchemaAttribute nestedAttribute = SchemaAttributeBuilder.builder()
                                                            .name("nested")
                                                            .type(Type.COMPLEX)
                                                            .subAttributes(lastnameAttribute)
                                                            .build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("person")
                                                            .type(Type.COMPLEX)
                                                            .required(true)
                                                            .subAttributes(firstnameAttribute, nestedAttribute)
                                                            .build();

    ObjectNode attribute = new ObjectNode(JsonNodeFactory.instance);
    attribute.set("firstname", new TextNode("captain"));
    ObjectNode nested = new ObjectNode(JsonNodeFactory.instance);
    nested.set("lastname", new TextNode("goldfish"));
    attribute.set("nested", nested);


    Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
      return RequestAttributeValidator.validateAttribute(schemaAttribute, attribute, HttpMethod.POST);
    });
    Assertions.assertTrue(validatedNode.isPresent());
    Assertions.assertEquals(attribute, validatedNode.get());
  }

  // -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------

  @Nested
  public class ReadOnlyTests
  {

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "mutability": "readOnly"
     *      "multivalued": "true|false"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * null
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN", "DATE_TIME", "REFERENCE", "COMPLEX"})
    public void testReadOnlyAttributeWithAllTypesOnNullNode(Type type)
    {
      boolean[] multiValuedArray = new boolean[]{true, false};
      for ( boolean multiValued : multiValuedArray )
      {
        SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                                .name("id")
                                                                .type(type)
                                                                .mutability(Mutability.READ_ONLY)
                                                                .multivalued(multiValued)
                                                                .build();
        Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
          return RequestAttributeValidator.validateAttribute(schemaAttribute, null, HttpMethod.POST);
        });
        Assertions.assertFalse(validatedNode.isPresent());
      }
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "mutability": "readOnly"
     *      "multivalued": true|false,
     *      "required": true|false
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     *   {
     *      "id": "123456"
     *   }
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN", "DATE_TIME", "REFERENCE", "COMPLEX"})
    public void testReadOnlyAttributeWithAllTypesOnExistingNode(Type type)
    {
      boolean[] requiredArray = new boolean[]{true, false};
      boolean[] multiValuedArray = new boolean[]{true, false};

      for ( boolean required : requiredArray )
      {
        for ( boolean multiValued : multiValuedArray )
        {
          SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                                  .name("id")
                                                                  .type(type)
                                                                  .mutability(Mutability.READ_ONLY)
                                                                  .multivalued(multiValued)
                                                                  .required(required)
                                                                  .build();
          JsonNode attribute = AttributeBuilder.build(type, "2");
          Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
            return RequestAttributeValidator.validateAttribute(schemaAttribute, attribute, HttpMethod.POST);
          });
          // read only attributes will be ignored even if they do not match its schema validation
          Assertions.assertFalse(validatedNode.isPresent());
        }
      }
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "complex",
     *      "multiValued": true,
     *      "mutability": "readOnly"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "array": [
     *      {
     *        ...
     *      }
     *   ]
     * }
     * </pre>
     */
    @Test
    public void testComplexMultivaluedValidationAsReadOnly()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
      SchemaAttribute primaryAttribute = SchemaAttributeBuilder.builder().name("primary").type(Type.BOOLEAN).build();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("person")
                                                              .type(Type.COMPLEX)
                                                              .multivalued(true)
                                                              .mutability(Mutability.READ_ONLY)
                                                              .subAttributes(firstnameAttribute, primaryAttribute)
                                                              .build();

      // one of the primary values is not present and therefore null. jsonNode != null protects from NullPointer
      ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
      ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
      element.set("firstname", new TextNode("Captain"));
      element.set("primary", BooleanNode.getTrue());
      attribute.add(element);

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return RequestAttributeValidator.validateAttribute(schemaAttribute, attribute, HttpMethod.POST);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure and expects an empty to be returned due to the read only nature of the
     * sub-attributes
     *
     * <pre>
     *    {
     *      "type": "complex",
     *      "multiValued": true,
     *      "subAttributes": [
     *        {
     *          "name": "firstname",
     *          "mutability": "readOnly",
     *          ...
     *        }
     *      ]
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "array": [
     *      {
     *        "firstname": "goldfish"
     *      }
     *   ]
     * }
     * </pre>
     */
    @Test
    public void testComplexMultivaluedValidationWithReadOnlySubAttributes()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder()
                                                                 .name("firstname")
                                                                 .type(Type.STRING)
                                                                 .mutability(Mutability.READ_ONLY)
                                                                 .build();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("person")
                                                              .type(Type.COMPLEX)
                                                              .multivalued(true)
                                                              .subAttributes(firstnameAttribute)
                                                              .build();

      // one of the primary values is not present and therefore null. jsonNode != null protects from NullPointer
      ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
      ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
      element.set("firstname", new TextNode("goldfish"));
      attribute.add(element);

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return RequestAttributeValidator.validateAttribute(schemaAttribute, attribute, HttpMethod.POST);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure and expects an empty to be returned due to the read only nature of the
     * sub-attributes
     *
     * <pre>
     *    {
     *      "type": "complex",
     *      "multiValued": false,
     *      "mutability": "readOnly",
     *      "subAttributes": [
     *        {
     *          "name": "firstname",
     *          "mutability": "readOnly",
     *          ...
     *        }
     *      ]
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "object": {
     *     "firstname": "goldfish"
     *   }
     * }
     * </pre>
     */
    @Test
    public void testComplexValidationWithReadOnlySubAttributes()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder()
                                                                 .name("firstname")
                                                                 .type(Type.STRING)
                                                                 .mutability(Mutability.READ_ONLY)
                                                                 .build();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("person")
                                                              .type(Type.COMPLEX)
                                                              .subAttributes(firstnameAttribute)
                                                              .build();

      // one of the primary values is not present and therefore null. jsonNode != null protects from NullPointer
      ObjectNode attribute = new ObjectNode(JsonNodeFactory.instance);
      attribute.set("firstname", new TextNode("goldfish"));

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return RequestAttributeValidator.validateAttribute(schemaAttribute, attribute, HttpMethod.POST);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }
  }

  // -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------

  @Nested
  public class RequiredFalseTests
  {

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "string",
     *      "mutability": "readWrite",
     *      "required": false
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * null
     * </pre>
     */
    @Test
    public void testNonePresentNoneRequiredReadWriteAttribute()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.STRING)
                                                              .mutability(Mutability.READ_WRITE)
                                                              .required(false)
                                                              .build();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return RequestAttributeValidator.validateAttribute(schemaAttribute, null, HttpMethod.POST);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "string",
     *      "mutability": "readWrite",
     *      "required": false
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "id": "123456"
     * }
     * </pre>
     */
    @Test
    public void testPresentNoneRequiredReadWriteAttribute()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.STRING)
                                                              .mutability(Mutability.READ_WRITE)
                                                              .required(false)
                                                              .build();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return RequestAttributeValidator.validateAttribute(schemaAttribute, new TextNode("123456"), HttpMethod.POST);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      ScimTextNode scimTextNode = (ScimTextNode)validatedNode.get();
      Assertions.assertEquals(schemaAttribute, scimTextNode.getSchemaAttribute());
      Assertions.assertEquals("123456", scimTextNode.textValue());
    }
  }

  @Nested
  public class RequiredTrueTests
  {

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "string",
     *      "required": true,
     *      "mutability": "readWrite|writeOnly"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * null
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"READ_WRITE", "WRITE_ONLY"})
    public void testRequiredReadWriteAndWriteOnlyAttributeWithMissingAttribute(Mutability mutability)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .mutability(mutability)
                                                              .build();
      try
      {
        RequestAttributeValidator.validateAttribute(schemaAttribute, null, HttpMethod.POST);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String errorMessage = String.format("Required '%s' attribute '%s' is missing",
                                            schemaAttribute.getMutability(),
                                            schemaAttribute.getFullResourceName());
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "string",
     *      "required": true,
     *      "mutability": "readWrite|writeOnly"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "id": null
     * }
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"READ_WRITE", "WRITE_ONLY"})
    public void testRequiredReadWriteAndWriteOnlyAttributeWithNullAttribute(Mutability mutability)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .mutability(mutability)
                                                              .build();
      try
      {
        RequestAttributeValidator.validateAttribute(schemaAttribute, NullNode.getInstance(), HttpMethod.POST);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String errorMessage = String.format("Required '%s' attribute '%s' is missing",
                                            schemaAttribute.getMutability(),
                                            schemaAttribute.getFullResourceName());
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "string",
     *      "required": true,
     *      "mutability": "immutable"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * null
     * </pre>
     */
    @Test
    public void testRequiredImmutableMissingAttributeOnPostRequest()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .mutability(Mutability.IMMUTABLE)
                                                              .build();
      try
      {
        RequestAttributeValidator.validateAttribute(schemaAttribute, null, HttpMethod.POST);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String errorMessage = String.format("Required '%s' attribute '%s' must be set on object creation",
                                            schemaAttribute.getMutability(),
                                            schemaAttribute.getFullResourceName());
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "string",
     *      "required": true,
     *      "mutability": "immutable"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "id": null
     * }
     * </pre>
     */
    @Test
    public void testRequiredImmutableNullAttributeOnPostRequest()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .mutability(Mutability.IMMUTABLE)
                                                              .build();
      try
      {
        RequestAttributeValidator.validateAttribute(schemaAttribute, NullNode.getInstance(), HttpMethod.POST);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String errorMessage = String.format("Required '%s' attribute '%s' must be set on object creation",
                                            schemaAttribute.getMutability(),
                                            schemaAttribute.getFullResourceName());
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": true,
     *      "mutability": "immutable"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     *   {
     *     "id": ${2}
     *   }
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testRequiredImmutableAttributeOnPutRequest(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .required(true)
                                                              .mutability(Mutability.IMMUTABLE)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return RequestAttributeValidator.validateAttribute(schemaAttribute, attribute, HttpMethod.POST);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }

    /**
     * tests the following structure and expects an empty to be returned due to the read only nature of the
     * sub-attributes
     *
     * <pre>
     *    {
     *      "type": "complex",
     *      "required": true
     *      "subAttributes": [
     *        {
     *          "name": "firstname",
     *          "mutability": "readOnly",
     *          ...
     *        }
     *      ]
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "object": {
     *     "firstname": "goldfish"
     *   }
     * }
     * </pre>
     */
    @Test
    public void testComplexValidationWithReadOnlySubAttributes()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder()
                                                                 .name("firstname")
                                                                 .type(Type.STRING)
                                                                 .mutability(Mutability.READ_ONLY)
                                                                 .build();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("person")
                                                              .type(Type.COMPLEX)
                                                              .required(true)
                                                              .subAttributes(firstnameAttribute)
                                                              .build();

      // one of the primary values is not present and therefore null. jsonNode != null protects from NullPointer
      ObjectNode attribute = new ObjectNode(JsonNodeFactory.instance);
      attribute.set("firstname", new TextNode("goldfish"));


      try
      {
        RequestAttributeValidator.validateAttribute(schemaAttribute, attribute, HttpMethod.POST);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        {
          Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
          String errorMessage = String.format("The required attribute '%s' was evaluated to an empty during "
                                              + "schema validation but the attribute is required '%s'",
                                              schemaAttribute.getFullResourceName(),
                                              attribute);
          Assertions.assertEquals(errorMessage, ex.getMessage());
        }
        {
          AttributeValidationException cause = (AttributeValidationException)ex.getCause();
          String errorMessage = String.format("Required '%s' attribute '%s' is missing",
                                              schemaAttribute.getMutability(),
                                              schemaAttribute.getFullResourceName());
          Assertions.assertEquals(errorMessage, cause.getMessage());
        }
      }
    }
  }
}
