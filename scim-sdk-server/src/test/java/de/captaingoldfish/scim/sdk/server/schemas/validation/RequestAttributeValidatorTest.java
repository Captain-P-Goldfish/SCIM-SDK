package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
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

  // -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------

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
        String errorMessage = String.format("'%s' attribute '%s' is required but is missing",
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
        String errorMessage = String.format("'%s' attribute '%s' is required but is missing",
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
        String errorMessage = String.format("'%s' required attribute '%s' must be set on object creation",
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
        String errorMessage = String.format("'%s' required attribute '%s' must be set on object creation",
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
  }
}
