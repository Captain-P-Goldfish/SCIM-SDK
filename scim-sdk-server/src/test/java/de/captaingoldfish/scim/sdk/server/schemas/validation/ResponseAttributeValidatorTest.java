package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
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
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import de.captaingoldfish.scim.sdk.server.utils.AttributeBuilder;
import de.captaingoldfish.scim.sdk.server.utils.SchemaAttributeBuilder;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 20.04.2021
 */
@Slf4j
public class ResponseAttributeValidatorTest
{

  /**
   * a url supplier that may be used in special cases during schema validation
   */
  private static final BiFunction<String, String, String> REFERENCE_URL_SUPPLIER = (resourceName, resourceId) -> {
    return String.format("http://localhost:8080/scim/v2/%s/%s", resourceName, resourceId);
  };

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
      return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                          attribute,
                                                          null,
                                                          null,
                                                          null,
                                                          REFERENCE_URL_SUPPLIER);
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
          return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                              null,
                                                              null,
                                                              null,
                                                              null,
                                                              REFERENCE_URL_SUPPLIER);
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
            return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                                attribute,
                                                                null,
                                                                null,
                                                                null,
                                                                REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
          return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                              null,
                                                              null,
                                                              null,
                                                              null,
                                                              REFERENCE_URL_SUPPLIER);
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
            return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                                attribute,
                                                                null,
                                                                null,
                                                                null,
                                                                REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        ResponseAttributeValidator.validateAttribute(schemaAttribute, null, null, null, null, REFERENCE_URL_SUPPLIER);
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
        ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                     NullNode.getInstance(),
                                                     null,
                                                     null,
                                                     null,
                                                     REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                     attribute,
                                                     null,
                                                     null,
                                                     null,
                                                     REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(schemaAttribute),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(schemaAttribute),
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(schemaAttribute),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(schemaAttribute),
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()),
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(schemaAttribute),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
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
                                                            null,
                                                            Collections.singletonList(schemaAttribute),
                                                            REFERENCE_URL_SUPPLIER);
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
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(type).build();
      JsonNode attribute = AttributeBuilder.build(type, "2");
      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            attribute,
                                                            null,
                                                            null,
                                                            Collections.singletonList(SchemaAttributeBuilder.builder()
                                                                                                            .name("another")
                                                                                                            .build()),
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      JsonNode jsonNode = validatedNode.get();
      Assertions.assertEquals(attribute, jsonNode);
    }
  }

  @Nested
  public class BaseUrlTests
  {

    /**
     * verifies that the API will set a resource url into the $ref attribute if the resource can be resolved
     */
    @Test
    public void testReferenceUrlIsSetByApi()
    {
      SchemaAttribute schemaAttribute = getComplexReferenceAttribute(false);

      final String id = UUID.randomUUID().toString();
      final String resourceName = "User";

      ObjectNode members = new ObjectNode(JsonNodeFactory.instance);
      members.set(AttributeNames.RFC7643.VALUE, new TextNode(id));
      members.set(AttributeNames.RFC7643.TYPE, new TextNode(resourceName));

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            members,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertEquals(3, validatedNode.get().size());

      final String expectedUrl = REFERENCE_URL_SUPPLIER.apply(resourceName, id);
      Assertions.assertEquals(expectedUrl, validatedNode.get().get(AttributeNames.RFC7643.REF).textValue());
    }

    /**
     * verifies that the API will NOT override a reference url that has been set by the implementation of the
     * developer
     */
    @Test
    public void testPresentReferenceUrlIsNotOverriddenByApi()
    {
      SchemaAttribute schemaAttribute = getComplexReferenceAttribute(false);

      final String id = UUID.randomUUID().toString();
      final String resourceName = "User";
      final String resourceUrl = "http://localhost:8889/my-resource/at/" + id;

      ObjectNode members = new ObjectNode(JsonNodeFactory.instance);
      members.set(AttributeNames.RFC7643.VALUE, new TextNode(id));
      members.set(AttributeNames.RFC7643.TYPE, new TextNode(resourceName));
      members.set(AttributeNames.RFC7643.REF, new TextNode(resourceUrl));

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            members,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertEquals(3, validatedNode.get().size());

      Assertions.assertEquals(resourceUrl, validatedNode.get().get(AttributeNames.RFC7643.REF).textValue());
    }

    /**
     * verifies that the API will NOT set the reference url if the resource type is missing
     */
    @TestFactory
    public List<DynamicTest> testReferenceUrlNotSetIfTypeIsMissing()
    {
      SchemaAttribute schemaAttribute = getComplexReferenceAttribute(false);

      Consumer<JsonNode> testWithTypeNode = typeNode -> {
        final String id = UUID.randomUUID().toString();

        ObjectNode members = new ObjectNode(JsonNodeFactory.instance);
        members.set(AttributeNames.RFC7643.VALUE, new TextNode(id));
        if (typeNode != null)
        {
          members.set(AttributeNames.RFC7643.TYPE, typeNode);
        }

        Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
          return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                              members,
                                                              null,
                                                              null,
                                                              null,
                                                              REFERENCE_URL_SUPPLIER);
        });
        Assertions.assertTrue(validatedNode.isPresent());
        Assertions.assertNull(validatedNode.get().get(AttributeNames.RFC7643.REF));
      };

      List<DynamicTest> dynamicTestList = new ArrayList<>();
      dynamicTestList.add(DynamicTest.dynamicTest("null", () -> testWithTypeNode.accept(null)));
      dynamicTestList.add(DynamicTest.dynamicTest("nullNode", () -> testWithTypeNode.accept(NullNode.getInstance())));
      dynamicTestList.add(DynamicTest.dynamicTest("empty", () -> testWithTypeNode.accept(new TextNode(""))));
      dynamicTestList.add(DynamicTest.dynamicTest("blank", () -> testWithTypeNode.accept(new TextNode("   "))));
      return dynamicTestList;
    }

    /**
     * verifies that the API will NOT set the reference url if the value is missing
     */
    @TestFactory
    public List<DynamicTest> testReferenceUrlNotSetIfValueIsMissing()
    {
      SchemaAttribute schemaAttribute = getComplexReferenceAttribute(false);

      Consumer<JsonNode> testWithTypeNode = valueNode -> {
        final String resourceName = "User";

        ObjectNode members = new ObjectNode(JsonNodeFactory.instance);
        members.set(AttributeNames.RFC7643.TYPE, new TextNode(resourceName));
        if (valueNode != null)
        {
          members.set(AttributeNames.RFC7643.VALUE, valueNode);
        }

        Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
          return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                              members,
                                                              null,
                                                              null,
                                                              null,
                                                              REFERENCE_URL_SUPPLIER);
        });
        Assertions.assertTrue(validatedNode.isPresent());
        Assertions.assertNull(validatedNode.get().get(AttributeNames.RFC7643.REF));
      };

      List<DynamicTest> dynamicTestList = new ArrayList<>();
      dynamicTestList.add(DynamicTest.dynamicTest("null", () -> testWithTypeNode.accept(null)));
      dynamicTestList.add(DynamicTest.dynamicTest("nullNode", () -> testWithTypeNode.accept(NullNode.getInstance())));
      dynamicTestList.add(DynamicTest.dynamicTest("empty", () -> testWithTypeNode.accept(new TextNode(""))));
      dynamicTestList.add(DynamicTest.dynamicTest("blank", () -> testWithTypeNode.accept(new TextNode("   "))));
      return dynamicTestList;
    }

    /**
     * verifies that the API will NOT set the reference url if either the type attribute, the $ref or the value
     * attribute is not defined as subAttribute within the complex type
     */
    @ParameterizedTest
    @ValueSource(strings = {AttributeNames.RFC7643.TYPE, AttributeNames.RFC7643.VALUE, AttributeNames.RFC7643.REF})
    public void testReferenceUrlNotSetIfRequiredSubAttributeIsNotDefined(String attributeName)
    {
      SchemaAttribute schemaAttribute = getComplexReferenceAttribute(false, attributeName);

      final String id = UUID.randomUUID().toString();
      final String resourceName = "User";

      ObjectNode members = new ObjectNode(JsonNodeFactory.instance);
      members.set(AttributeNames.RFC7643.VALUE, new TextNode(id));
      members.set(AttributeNames.RFC7643.TYPE, new TextNode(resourceName));

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            members,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertNull(validatedNode.get().get(AttributeNames.RFC7643.REF));
    }

    /**
     * verifies that no reference url is set if the referenceUrlSupplier returns null
     */
    @Test
    public void testReferenceUrlNotSetIfReferenceUrlSupplierReturnsNull()
    {
      SchemaAttribute schemaAttribute = getComplexReferenceAttribute(false);

      final String id = UUID.randomUUID().toString();
      final String resourceName = "User";

      ObjectNode members = new ObjectNode(JsonNodeFactory.instance);
      members.set(AttributeNames.RFC7643.VALUE, new TextNode(id));
      members.set(AttributeNames.RFC7643.TYPE, new TextNode(resourceName));

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            members,
                                                            null,
                                                            null,
                                                            null,
                                                            (s, s2) -> null);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertEquals(2, validatedNode.get().size());

      Assertions.assertNull(validatedNode.get().get(AttributeNames.RFC7643.REF));
    }

    /**
     * verifies that no reference url is set if the referenceUrlSupplier returns null
     */
    @Test
    public void testReferenceUrlIsSetInAllComplexTypesOfAMultiValuedComplexType()
    {
      SchemaAttribute schemaAttribute = getComplexReferenceAttribute(true);

      final String resourceName = "User";

      ArrayNode members = new ArrayNode(JsonNodeFactory.instance);
      for ( int i = 0 ; i < 3 ; i++ )
      {
        final String id = UUID.randomUUID().toString();
        ObjectNode member = new ObjectNode(JsonNodeFactory.instance);
        member.set(AttributeNames.RFC7643.VALUE, new TextNode(id));
        member.set(AttributeNames.RFC7643.TYPE, new TextNode(resourceName));
        members.add(member);
      }

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            members,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertTrue(validatedNode.get().isArray());
      Assertions.assertEquals(3, validatedNode.get().size());

      for ( int i = 0 ; i < members.size() ; i++ )
      {
        ObjectNode member = (ObjectNode)members.get(i);
        ObjectNode validatedMember = (ObjectNode)validatedNode.get().get(i);

        final String expectedUrl = REFERENCE_URL_SUPPLIER.apply(resourceName,
                                                                member.get(AttributeNames.RFC7643.VALUE).textValue());
        Assertions.assertEquals(expectedUrl, validatedMember.get(AttributeNames.RFC7643.REF).textValue());
      }
    }

    /**
     * verifies that the reference id is set even if a multivalued complex type is set as a simple complex node
     * and not as an array
     */
    @Test
    public void testReferenceUrlIsSetInIfMultiValuedIsASingleComplexNode()
    {
      SchemaAttribute schemaAttribute = getComplexReferenceAttribute(true);

      final String id = UUID.randomUUID().toString();
      final String resourceName = "User";

      ObjectNode member = new ObjectNode(JsonNodeFactory.instance);
      member.set(AttributeNames.RFC7643.VALUE, new TextNode(id));
      member.set(AttributeNames.RFC7643.TYPE, new TextNode(resourceName));

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                            member,
                                                            null,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertTrue(validatedNode.get().isArray());
      Assertions.assertEquals(1, validatedNode.get().size());

      final String expectedUrl = REFERENCE_URL_SUPPLIER.apply(resourceName,
                                                              member.get(AttributeNames.RFC7643.VALUE).textValue());
      Assertions.assertEquals(expectedUrl, validatedNode.get().get(0).get(AttributeNames.RFC7643.REF).textValue());
    }

    /**
     * builds a complex type after the groups "members"-attribute template that is either multivalued or a simple
     * complex type
     */
    private SchemaAttribute getComplexReferenceAttribute(boolean multivalued, String... subAttributesToIgnore)
    {

      SchemaAttribute valueAttribute = SchemaAttributeBuilder.builder()
                                                             .name(AttributeNames.RFC7643.VALUE)
                                                             .type(Type.STRING)
                                                             .build();
      SchemaAttribute typeAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.STRING)
                                                            .build();
      SchemaAttribute refAttribute = SchemaAttributeBuilder.builder()
                                                           .name(AttributeNames.RFC7643.REF)
                                                           .type(Type.REFERENCE)
                                                           .referenceTypes(ReferenceTypes.RESOURCE)
                                                           .build();

      SchemaAttributeBuilder attributeBuilder = SchemaAttributeBuilder.builder()
                                                                      .name(AttributeNames.RFC7643.MEMBERS)
                                                                      .multivalued(multivalued)
                                                                      .type(Type.COMPLEX);
      List<String> ignoreAttributes = Optional.ofNullable(subAttributesToIgnore)
                                              .map(Arrays::asList)
                                              .orElse(new ArrayList<>());
      List<SchemaAttribute> attributeToAdd = new ArrayList<>();
      if (!ignoreAttributes.contains(AttributeNames.RFC7643.VALUE))
      {
        attributeToAdd.add(valueAttribute);
      }
      if (!ignoreAttributes.contains(AttributeNames.RFC7643.TYPE))
      {
        attributeToAdd.add(typeAttribute);
      }
      if (!ignoreAttributes.contains(AttributeNames.RFC7643.REF))
      {
        attributeToAdd.add(refAttribute);
      }
      return attributeBuilder.subAttributes(attributeToAdd.toArray(new SchemaAttribute[0])).build();
    }
  }

  /**
   * checks that values with a returned-value of "request" and "default" are correctly returned
   */
  @Nested
  public class PresentInRequestTests
  {

    /**
     * shows that the attribute "lastname" of the following structure is correctly returned if the attribute was
     * present within the request
     *
     * <pre>
     *   "name": {
     *     "familyName": "goldfish"
     *   }
     * </pre>
     */
    @Test
    public void testRequestSubAttributeIsPresentInRequest()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      final String attributeName = String.format("%s.%s",
                                                 AttributeNames.RFC7643.NAME,
                                                 AttributeNames.RFC7643.FAMILY_NAME);
      TestHelper.modifyAttributeMetaData(userSchema,
                                         attributeName,
                                         null,
                                         null,
                                         Returned.REQUEST,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);
      SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NAME);

      Name name = Name.builder().familyName("goldfish").build();
      User user = User.builder().userName("captain").name(name).build();

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nameAttribute,
                                                            name,
                                                            user,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertEquals(name, validatedNode.get());
    }

    /**
     * shows that the attribute "lastname" of the following structure is correctly returned if the attribute was
     * present within the request but is not explicitly requested in the attributes-parameter
     *
     * <pre>
     *   "name": {
     *     "familyName": "goldfish"
     *   }
     * </pre>
     */
    @Test
    public void testRequestSubAttributeIsPresentInRequestButNotInAttributes()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      final String attributeName = String.format("%s.%s",
                                                 AttributeNames.RFC7643.NAME,
                                                 AttributeNames.RFC7643.FAMILY_NAME);
      TestHelper.modifyAttributeMetaData(userSchema,
                                         attributeName,
                                         null,
                                         null,
                                         Returned.REQUEST,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);
      SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NAME);

      Name name = Name.builder().familyName("goldfish").middlename("captain").build();
      User user = User.builder().userName("captain").name(name).build();

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nameAttribute,
                                                            name,
                                                            user,
                                                            Collections.singletonList(nameAttribute),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertEquals(name, validatedNode.get());
    }


    /**
     * shows that the attribute "lastname" of the following structure is removed from the node if the attribute
     * was not present within the request
     *
     * <pre>
     *   "name": {
     *     "familyName": "goldfish"
     *   }
     * </pre>
     */
    @Test
    public void testRequestSubAttributeIsNotPresentInRequestAndNotInAttributes()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      final String attributeName = String.format("%s.%s",
                                                 AttributeNames.RFC7643.NAME,
                                                 AttributeNames.RFC7643.FAMILY_NAME);
      TestHelper.modifyAttributeMetaData(userSchema,
                                         attributeName,
                                         null,
                                         null,
                                         Returned.REQUEST,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);
      SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NAME);

      Name name = Name.builder().familyName("goldfish").middlename("captain").build();
      User user = User.builder().userName("captain").name(Name.builder().givenName("goldfish").build()).build();

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nameAttribute,
                                                            name,
                                                            user,
                                                            null,
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertNotEquals(name, validatedNode.get());
      Assertions.assertEquals(1, validatedNode.get().size());
      Assertions.assertNotNull(validatedNode.get().get(AttributeNames.RFC7643.MIDDLE_NAME));
    }

    /**
     * shows that the attribute "lastname" of the following structure is removed from the node if the attribute
     * was not present within the request and also not present within the attributes parameter
     *
     * <pre>
     *   "name": {
     *     "familyName": "goldfish"
     *   }
     * </pre>
     */
    @Test
    public void testRequestSubAttributeIsNotPresentInRequest()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      final String attributeName = String.format("%s.%s",
                                                 AttributeNames.RFC7643.NAME,
                                                 AttributeNames.RFC7643.FAMILY_NAME);
      TestHelper.modifyAttributeMetaData(userSchema,
                                         attributeName,
                                         null,
                                         null,
                                         Returned.REQUEST,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);
      SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NAME);
      SchemaAttribute middleNameAttribute = userSchema.getSchemaAttribute(String.format("%s.%s",
                                                                                        AttributeNames.RFC7643.NAME,
                                                                                        AttributeNames.RFC7643.MIDDLE_NAME));

      Name name = Name.builder().familyName("goldfish").middlename("captain").build();
      User user = User.builder().userName("captain").name(Name.builder().givenName("goldfish").build()).build();

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nameAttribute,
                                                            name,
                                                            user,
                                                            Collections.singletonList(middleNameAttribute),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertNotEquals(name, validatedNode.get());
      Assertions.assertEquals(1, validatedNode.get().size());
      Assertions.assertNull(validatedNode.get().get(AttributeNames.RFC7643.FAMILY_NAME));
      Assertions.assertNotNull(validatedNode.get().get(AttributeNames.RFC7643.MIDDLE_NAME));
    }

  }

  @Nested
  public class AttributesParameterTests
  {

    /**
     * shows that the attribute "nickName" of the following structure is correctly returned if the attributes
     * parameter does reference this attribute
     *
     * <pre>
     *   attributes: ["nickName"]
     * </pre>
     *
     * <pre>
     *   "nickName": "captain"
     * </pre>
     */
    @Test
    public void testRequestAttributesParentIsInAttributesParameter()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      TestHelper.modifyAttributeMetaData(userSchema,
                                         AttributeNames.RFC7643.NICK_NAME,
                                         null,
                                         null,
                                         Returned.REQUEST,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);
      SchemaAttribute nickNameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NICK_NAME);

      JsonNode nickName = new TextNode("goldfish");

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nickNameAttribute,
                                                            nickName,
                                                            null,
                                                            Collections.singletonList(nickNameAttribute),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertEquals(nickName, validatedNode.get());
    }

    /**
     * shows that the attribute "nickName" of the following structure is not returned if the attributes parameter
     * does not reference it
     *
     * <pre>
     *   attributes: ["displayName"]
     * </pre>
     *
     * <pre>
     *   "nickName": "captain"
     * </pre>
     */
    @Test
    public void testRequestAttributeIsNotInAttributesParameter()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      TestHelper.modifyAttributeMetaData(userSchema,
                                         AttributeNames.RFC7643.NICK_NAME,
                                         null,
                                         null,
                                         Returned.REQUEST,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);
      SchemaAttribute nickNameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NICK_NAME);
      SchemaAttribute displayNameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.DISPLAY_NAME);

      JsonNode nickName = new TextNode("goldfish");

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nickNameAttribute,
                                                            nickName,
                                                            null,
                                                            Collections.singletonList(displayNameAttribute),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * shows that the attribute "lastname" of the following structure is correctly returned if the attributes
     * parameter has its parent "name" referenced
     *
     * <pre>
     *   attributes: ["name"]
     * </pre>
     *
     * <pre>
     *   "name": {
     *     "familyName": "goldfish",
     *     "middleName": "captain"
     *   }
     * </pre>
     */
    @Test
    public void testRequestSubAttributesParentIsInAttributesParameter()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      final String attributeName = String.format("%s.%s",
                                                 AttributeNames.RFC7643.NAME,
                                                 AttributeNames.RFC7643.FAMILY_NAME);
      TestHelper.modifyAttributeMetaData(userSchema,
                                         attributeName,
                                         null,
                                         null,
                                         Returned.REQUEST,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);
      SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NAME);

      Name name = Name.builder().familyName("goldfish").middlename("captain").build();
      User user = User.builder().userName("captain").build();

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nameAttribute,
                                                            name,
                                                            user,
                                                            Collections.singletonList(nameAttribute),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertEquals(name, validatedNode.get());
      Assertions.assertNotNull(validatedNode.get().get(AttributeNames.RFC7643.FAMILY_NAME));
      Assertions.assertNotNull(validatedNode.get().get(AttributeNames.RFC7643.MIDDLE_NAME));
    }

    /**
     * shows that the attribute "lastname" of the following structure is correctly returned if the attributes
     * parameter has its parent "name" referenced
     *
     * <pre>
     *   attributes: ["name.familyName"]
     * </pre>
     *
     * <pre>
     *   "name": {
     *     "familyName": "goldfish",
     *     "middleName": "captain"
     *   }
     * </pre>
     */
    @Test
    public void testRequestSubAttributeIsInAttributesParameter()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      final String familyAttributeName = String.format("%s.%s",
                                                       AttributeNames.RFC7643.NAME,
                                                       AttributeNames.RFC7643.FAMILY_NAME);
      TestHelper.modifyAttributeMetaData(userSchema,
                                         familyAttributeName,
                                         null,
                                         null,
                                         Returned.REQUEST,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);
      SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NAME);
      SchemaAttribute familyNameAttribute = userSchema.getSchemaAttribute(familyAttributeName);

      Name name = Name.builder().familyName("goldfish").middlename("captain").build();
      User user = User.builder().userName("captain").build();

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nameAttribute,
                                                            name,
                                                            user,
                                                            Collections.singletonList(familyNameAttribute),
                                                            null,
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertNotEquals(name, validatedNode.get());
      Assertions.assertNotNull(validatedNode.get().get(AttributeNames.RFC7643.FAMILY_NAME));
      Assertions.assertNull(validatedNode.get().get(AttributeNames.RFC7643.MIDDLE_NAME));
    }

  }

  @Nested
  public class ExcludedAttributesParameterTests
  {

    /**
     * shows that the attribute "nickName" of the following structure is correctly removed if it is referenced in
     * the excluded attributes parameter
     *
     * <pre>
     *   excludedAttributes: ["nickName"]
     * </pre>
     *
     * <pre>
     *   "nickName": "captain"
     * </pre>
     */
    @Test
    public void testRequestAttributesParentIsInExcludedAttributesParameter()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      SchemaAttribute nickNameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NICK_NAME);

      JsonNode nickName = new TextNode("goldfish");

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nickNameAttribute,
                                                            nickName,
                                                            null,
                                                            null,
                                                            Collections.singletonList(nickNameAttribute),
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * shows that the attribute "nickName" of the following structure is correctly removed if its returned-value
     * is request but not referenced in the excludedAttributes parameter the excluded attributes parameter
     *
     * <pre>
     *   excludedAttributes: ["displayName"]
     * </pre>
     *
     * <pre>
     *   "nickName": "captain"
     * </pre>
     */
    @Test
    public void testRequestAttributeIsNotInExcludedAttributesParameterButReturnedIsRequest()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      TestHelper.modifyAttributeMetaData(userSchema,
                                         AttributeNames.RFC7643.NICK_NAME,
                                         null,
                                         null,
                                         Returned.REQUEST,
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);
      SchemaAttribute nickNameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NICK_NAME);
      SchemaAttribute displayNameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.DISPLAY_NAME);

      JsonNode nickName = new TextNode("goldfish");

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nickNameAttribute,
                                                            nickName,
                                                            null,
                                                            null,
                                                            Collections.singletonList(displayNameAttribute),
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * shows that the attribute "nickName" of the following structure is not removed if it is not referenced in
     * the excludedAttributes parameter
     *
     * <pre>
     *   excludedAttributes: ["displayName"]
     * </pre>
     *
     * <pre>
     *   "nickName": "captain"
     * </pre>
     */
    @Test
    public void testAttributeIsNotExcludedIfNotInExcludedAttributesParameter()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);

      SchemaAttribute nickNameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NICK_NAME);
      SchemaAttribute displayNameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.DISPLAY_NAME);

      JsonNode nickName = new TextNode("goldfish");

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nickNameAttribute,
                                                            nickName,
                                                            null,
                                                            null,
                                                            Collections.singletonList(displayNameAttribute),
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertEquals(nickName, validatedNode.get());
    }

    /**
     * shows that the attribute "name" is completely excluded if referenced in the excludedAttributes parameter
     *
     * <pre>
     *   excludedAttributes: ["name"]
     * </pre>
     *
     * <pre>
     *   "name": {
     *     "familyName": "goldfish",
     *     "middleName": "captain"
     *   }
     * </pre>
     */
    @Test
    public void testComplexAttributeIsExcludedIfInExcludedAttributesParameter()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);

      SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NAME);

      Name name = Name.builder().familyName("goldfish").middlename("captain").build();

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nameAttribute,
                                                            name,
                                                            null,
                                                            null,
                                                            Collections.singletonList(nameAttribute),
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertFalse(validatedNode.isPresent());
    }

    /**
     * shows that the attribute "lastname" is excluded if it is referenced in the excludedAttributes parameter
     *
     * <pre>
     *   excludedAttributes: ["name.familyName"]
     * </pre>
     *
     * <pre>
     *   "name": {
     *     "familyName": "goldfish",
     *     "middleName": "captain"
     *   }
     * </pre>
     */
    @Test
    public void testSubAttributeIsExcludedIfInExcludedAttributesParameter()
    {
      JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
      Schema userSchema = new Schema(userSchemaNode);
      final String familyAttributeName = String.format("%s.%s",
                                                       AttributeNames.RFC7643.NAME,
                                                       AttributeNames.RFC7643.FAMILY_NAME);
      SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(AttributeNames.RFC7643.NAME);
      SchemaAttribute familyNameAttribute = userSchema.getSchemaAttribute(familyAttributeName);

      Name name = Name.builder().familyName("goldfish").middlename("captain").build();

      Optional<JsonNode> validatedNode = Assertions.assertDoesNotThrow(() -> {
        return ResponseAttributeValidator.validateAttribute(nameAttribute,
                                                            name,
                                                            null,
                                                            null,
                                                            Collections.singletonList(familyNameAttribute),
                                                            REFERENCE_URL_SUPPLIER);
      });
      Assertions.assertTrue(validatedNode.isPresent());
      Assertions.assertNotEquals(name, validatedNode.get());
      Assertions.assertNull(validatedNode.get().get(AttributeNames.RFC7643.FAMILY_NAME));
      Assertions.assertNotNull(validatedNode.get().get(AttributeNames.RFC7643.MIDDLE_NAME));
    }

  }
}
