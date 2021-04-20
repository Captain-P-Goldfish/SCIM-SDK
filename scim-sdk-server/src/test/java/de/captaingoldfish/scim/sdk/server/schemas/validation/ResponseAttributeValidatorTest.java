package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Collections;
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

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import de.captaingoldfish.scim.sdk.server.utils.AttributeBuilder;
import de.captaingoldfish.scim.sdk.server.utils.SchemaAttributeBuilder;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 20.04.2021
 */
@Slf4j
public class ResponseAttributeValidatorTest
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
      return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
    });
    Assertions.assertTrue(validatedNode.isPresent());
    Assertions.assertEquals(attribute, validatedNode.get());
  }

  @Nested
  public class WriteOnlyTests
  {

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "mutability": "writeOnly"
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
    public void testWriteOnlyAttributeWithAllTypesOnNullNode(Type type)
    {
      boolean[] multiValuedArray = new boolean[]{true, false};
      for ( boolean multiValued : multiValuedArray )
      {
        SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                                .name("id")
                                                                .type(type)
                                                                .mutability(Mutability.WRITE_ONLY)
                                                                .multivalued(multiValued)
                                                                .build();
        Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
          return ResponseAttributeValidator.validateAttribute(schemaAttribute, null, null, null);
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
     *      "mutability": "writeOnly"
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
    public void testWriteOnlyAttributeWithAllTypesOnExistingNode(Type type)
    {
      boolean[] requiredArray = new boolean[]{true, false};
      boolean[] multiValuedArray = new boolean[]{true, false};

      for ( boolean required : requiredArray )
      {
        for ( boolean multiValued : multiValuedArray )
        {
          SchemaAttributeBuilder builder = SchemaAttributeBuilder.builder()
                                                                 .name("id")
                                                                 .type(type)
                                                                 .mutability(Mutability.WRITE_ONLY)
                                                                 .multivalued(multiValued)
                                                                 .required(required);
          if (type.equals(Type.COMPLEX))
          {
            SchemaAttribute subAttribute = builder.build();
            subAttribute.set(AttributeNames.RFC7643.TYPE, new TextNode(Type.STRING.getValue()));
            subAttribute.set(AttributeNames.RFC7643.MUTABILITY, new TextNode(Mutability.READ_WRITE.getValue()));
            builder.subAttributes(subAttribute);
          }
          SchemaAttribute schemaAttribute = builder.build();
          JsonNode attribute = AttributeBuilder.build(schemaAttribute, type, "2");
          Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
            return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
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
     *      "mutability": "writeOnly"
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
    public void testComplexMultivaluedValidationAsWriteOnly()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
      SchemaAttribute primaryAttribute = SchemaAttributeBuilder.builder().name("primary").type(Type.BOOLEAN).build();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("person")
                                                              .type(Type.COMPLEX)
                                                              .multivalued(true)
                                                              .mutability(Mutability.WRITE_ONLY)
                                                              .subAttributes(firstnameAttribute, primaryAttribute)
                                                              .build();

      // one of the primary values is not present and therefore null. jsonNode != null protects from NullPointer
      ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
      ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
      element.set("firstname", new TextNode("Captain"));
      element.set("primary", BooleanNode.getTrue());
      attribute.add(element);

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure and expects an empty to be returned due to the write only nature of the
     * sub-attributes
     *
     * <pre>
     *    {
     *      "type": "complex",
     *      "multiValued": true,
     *      "subAttributes": [
     *        {
     *          "name": "firstname",
     *          "mutability": "writeOnly",
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
    public void testComplexMultivaluedValidationWithWriteOnlySubAttributes()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder()
                                                                 .name("firstname")
                                                                 .type(Type.STRING)
                                                                 .mutability(Mutability.WRITE_ONLY)
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
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
     *      "mutability": "writeOnly",
     *      "subAttributes": [
     *        {
     *          "name": "firstname",
     *          "mutability": "writeOnly",
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
    public void testComplexValidationWithWriteOnlySubAttributes()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder()
                                                                 .name("firstname")
                                                                 .type(Type.STRING)
                                                                 .mutability(Mutability.WRITE_ONLY)
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }
  }

  @Nested
  public class ReturnedNeverTests
  {

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "returned": "never"
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
    public void testReturnedNeverAttributeWithAllTypesOnNullNode(Type type)
    {
      boolean[] multiValuedArray = new boolean[]{true, false};
      for ( boolean multiValued : multiValuedArray )
      {
        SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                                .name("id")
                                                                .type(type)
                                                                .returned(Returned.NEVER)
                                                                .multivalued(multiValued)
                                                                .build();
        Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
          return ResponseAttributeValidator.validateAttribute(schemaAttribute, null, null, null);
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
     *      "returned": "never"
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
    public void testReturnedNeverAttributeWithAllTypesOnExistingNode(Type type)
    {
      boolean[] requiredArray = new boolean[]{true, false};
      boolean[] multiValuedArray = new boolean[]{true, false};

      for ( boolean required : requiredArray )
      {
        for ( boolean multiValued : multiValuedArray )
        {
          SchemaAttributeBuilder builder = SchemaAttributeBuilder.builder()
                                                                 .name("id")
                                                                 .type(type)
                                                                 .returned(Returned.NEVER)
                                                                 .multivalued(multiValued)
                                                                 .required(required);
          if (type.equals(Type.COMPLEX))
          {
            SchemaAttribute subAttribute = builder.build();
            subAttribute.set(AttributeNames.RFC7643.TYPE, new TextNode(Type.STRING.getValue()));
            subAttribute.set(AttributeNames.RFC7643.RETURNED, new TextNode(Returned.DEFAULT.getValue()));
            builder.subAttributes(subAttribute);
          }
          SchemaAttribute schemaAttribute = builder.build();
          JsonNode attribute = AttributeBuilder.build(schemaAttribute, type, "2");
          Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
            return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
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
     *      "returned": "never"
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
    public void testComplexMultivaluedValidationAsReturnedNever()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder().name("firstname").type(Type.STRING).build();
      SchemaAttribute primaryAttribute = SchemaAttributeBuilder.builder().name("primary").type(Type.BOOLEAN).build();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("person")
                                                              .type(Type.COMPLEX)
                                                              .multivalued(true)
                                                              .returned(Returned.NEVER)
                                                              .subAttributes(firstnameAttribute, primaryAttribute)
                                                              .build();

      // one of the primary values is not present and therefore null. jsonNode != null protects from NullPointer
      ArrayNode attribute = new ArrayNode(JsonNodeFactory.instance);
      ObjectNode element = new ObjectNode(JsonNodeFactory.instance);
      element.set("firstname", new TextNode("Captain"));
      element.set("primary", BooleanNode.getTrue());
      attribute.add(element);

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure and expects an empty to be returned due to the write only nature of the
     * sub-attributes
     *
     * <pre>
     *    {
     *      "type": "complex",
     *      "multiValued": true,
     *      "subAttributes": [
     *        {
     *          "name": "firstname",
     *          "returned": "never",
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
    public void testComplexMultivaluedValidationWithReturnedNeverSubAttributes()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder()
                                                                 .name("firstname")
                                                                 .type(Type.STRING)
                                                                 .returned(Returned.NEVER)
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
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
     *      "returned": "never",
     *      "subAttributes": [
     *        {
     *          "name": "firstname",
     *          "returned": "never",
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
    public void testComplexValidationWithReturnedNeverSubAttributes()
    {
      SchemaAttribute firstnameAttribute = SchemaAttributeBuilder.builder()
                                                                 .name("firstname")
                                                                 .type(Type.STRING)
                                                                 .returned(Returned.NEVER)
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
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
     *      "mutability": "readWrite|readOnly|immutable"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * null
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"READ_WRITE", "READ_ONLY", "IMMUTABLE"})
    public void testRequiredAttributeWithMissingAttribute(Mutability mutability)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .mutability(mutability)
                                                              .build();
      try
      {
        ResponseAttributeValidator.validateAttribute(schemaAttribute, null, null, null);
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
     *      "mutability": "readWrite|readOnly|immutable"
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
    @ValueSource(strings = {"READ_WRITE", "READ_ONLY", "IMMUTABLE"})
    public void testRequiredAttributeWithNullAttribute(Mutability mutability)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .mutability(mutability)
                                                              .build();
      try
      {
        ResponseAttributeValidator.validateAttribute(schemaAttribute, NullNode.getInstance(), null, null);
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
     *      "type": "*",
     *      "required": true,
     *      "mutability": "readOnly"
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
    public void testRequiredReadOnlyAttributeOnPutRequest(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .required(true)
                                                              .mutability(Mutability.READ_ONLY)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": true,
     *      "mutability": "readWrite"
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
    public void testRequiredReadWriteAttributeOnPutRequest(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .required(true)
                                                              .mutability(Mutability.READ_WRITE)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }

    /**
     * tests the following structure and expects an empty to be returned due to the write only nature of the
     * sub-attributes
     *
     * <pre>
     *    {
     *      "type": "complex",
     *      "required": true
     *      "subAttributes": [
     *        {
     *          "name": "firstname",
     *          "mutability": "writeOnly",
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
                                                                 .mutability(Mutability.WRITE_ONLY)
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
        ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
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

  @Nested
  public class ReturnedAlwaysTests
  {

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "always"
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
    public void testReturnedAlwaysAttribute(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.ALWAYS)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": true
     *      "returned": "always"
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
    public void testRequiredReturnedAlwaysAttribute(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .required(true)
                                                              .returned(Returned.ALWAYS)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "always"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * null
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testReturnedAlwaysAttributeAsNull(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.ALWAYS)
                                                              .build();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, null, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "always"
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
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testReturnedAlwaysAttributeAsNullNode(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.ALWAYS)
                                                              .build();
      JsonNode attribute = NullNode.getInstance();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   attributes: ["id"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "always"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "id": 2
     * }
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testReturnedAlwaysAttributeWithAttributesParameterContainingParamName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.ALWAYS)
                                                              .build();
      JsonNode attribute = NullNode.getInstance();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            Collections.singletonList(schemaAttribute),
                                                            null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   attributes: ["another"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "always"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "id": 2
     * }
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testReturnedAlwaysAttributeWithAttributesParameterNotContainingParamName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.ALWAYS)
                                                              .build();
      JsonNode attribute = NullNode.getInstance();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()),
                                                            null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   excludedAttributes: ["id"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "always"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * {
     *   "id": 2
     * }
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testReturnedAlwaysAttributeWithExcludedAttributesParameterContainingParamName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.ALWAYS)
                                                              .build();
      JsonNode attribute = NullNode.getInstance();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            Collections.singletonList(schemaAttribute));
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }
  }

  @Nested
  public class ReturnedDefaultTests
  {

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "default"
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
    public void testReturnedDefaultAttribute(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.DEFAULT)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "default"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * null
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testReturnedDefaultAttributeAsNull(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.DEFAULT)
                                                              .build();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, null, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "default"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     *   {
     *     "id": null
     *   }
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testReturnedDefaultAttributeAsNullNode(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.DEFAULT)
                                                              .build();
      JsonNode attribute = NullNode.getInstance();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   attributes: ["id"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "default"
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
    public void testReturnedDefaultAttributeWithAttributesParamContainingTheName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.DEFAULT)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            Collections.singletonList(schemaAttribute),
                                                            null);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   attributes: ["another"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "default"
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
    public void testReturnedDefaultAttributeWithAttributesParamNotContainingTheName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.DEFAULT)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()),
                                                            null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   excludedAttributes: ["id"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "default"
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
    public void testReturnedDefaultAttributeWithExcludedAttributesParamContainingTheName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.DEFAULT)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            Collections.singletonList(schemaAttribute));
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   excludedAttributes: ["another"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "default"
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
    public void testReturnedDefaultAttributeWithExcludedAttributesParamNotContainingTheName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.DEFAULT)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()));
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }
  }

  @Nested
  public class ReturnedRequestsTests
  {

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "request"
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
    public void testReturnedRequestAttribute(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.REQUEST)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "request"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     * null
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testReturnedRequestAttributeAsNull(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.REQUEST)
                                                              .build();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, null, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     *
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "request"
     *      ...
     *    }
     * </pre>
     *
     * <pre>
     *   {
     *     "id": null
     *   }
     * </pre>
     */
    @ParameterizedTest
    @ValueSource(strings = {"ANY", "STRING", "INTEGER", "DECIMAL", "BOOLEAN"})
    public void testReturnedRequestAttributeAsNullNode(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.REQUEST)
                                                              .build();
      JsonNode attribute = NullNode.getInstance();
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute, attribute, null, null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   attributes: ["id"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "request"
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
    public void testReturnedRequestAttributeWithAttributesParamContainingTheName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.REQUEST)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            Collections.singletonList(schemaAttribute),
                                                            null);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   attributes: ["another"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "request"
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
    public void testReturnedRequestAttributeWithAttributesParamNotContainingTheName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.REQUEST)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()),
                                                            null);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   excludedAttributes: ["id"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "request"
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
    public void testReturnedRequestAttributeWithExcludedAttributesParamContainingTheName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.REQUEST)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            Collections.singletonList(schemaAttribute));
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * tests the following structure
     * 
     * <pre>
     *   excludedAttributes: ["another"]
     * </pre>
     * 
     * <pre>
     *    {
     *      "type": "*",
     *      "required": false,
     *      "returned": "request"
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
    public void testReturnedRequestAttributeWithExcludedAttributesParamNotContainingTheName(Type type)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(type)
                                                              .returned(Returned.REQUEST)
                                                              .build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()));
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }
  }
}
