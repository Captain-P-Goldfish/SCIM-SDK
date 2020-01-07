package de.captaingoldfish.scim.sdk.common.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimDoubleNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimIntNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimLongNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * author Pascal Knueppel <br>
 * created at: 30.11.2019 - 22:48 <br>
 * <br>
 */
public class AttributeValidatorTest implements FileReferences
{


  /**
   * verifies that a string-type attribute validation works correctly
   */
  @TestFactory
  public List<DynamicTest> testValidateStringTypeAttribute()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    SchemaAttribute stringAttribute = schema.getSchemaAttribute("minstring");
    final int minLength = 5;
    final int maxLength = 10;
    final String pattern = "(?i)[a-z0-9]+";

    List<DynamicTest> dynamicTests = new ArrayList<>();
    // @formatter:off
    dynamicTests.add(getAttributeValidationTest("minimum value not reached",
      () -> new ScimTextNode(stringAttribute, "a"),
      "the attribute '" + stringAttribute.getName() + "' has a "
        + "minimum length of " + minLength + " characters but value is 'a'"));
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded",
      () -> new ScimTextNode(stringAttribute, "01234567890"),
      "the attribute '" + stringAttribute.getName() + "' has a "
        + "maximum length of " + maxLength + " characters but value "
        + "is '01234567890'"));
    dynamicTests.add(getAttributeValidationTest("pattern does not match",
      () -> new ScimTextNode(stringAttribute, "$%&/!"),
      "the attribute '" + stringAttribute.getName()
        + "' must match the regular expression '" + pattern
        + "' but value is '$%&/!'"));
    dynamicTests.add(DynamicTest.dynamicTest("validation matches minimumLength exactly", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimTextNode(stringAttribute, "12345"));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("validation matches maximumLength exactly", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimTextNode(stringAttribute, "0123456789"));
    }));
    // @formatter:on
    return dynamicTests;
  }

  /**
   * verifies that a number-type attribute validation works correctly
   */
  @TestFactory
  public List<DynamicTest> testValidateNumberTypeAttribute()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    SchemaAttribute numberAttribute = schema.getSchemaAttribute("number");
    SchemaAttribute decimalAttribute = schema.getSchemaAttribute("decimal");
    final double minimum = 10;
    final double minimumDecimal = 10.5;
    final double maximum = 100;
    final double maximumDecimal = 100.123456789;
    final double multipleOf = 3;
    final double multipleOfDecimal = 3.1;

    List<DynamicTest> dynamicTests = new ArrayList<>();
    // @formatter:off
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for int",
                                                () -> new ScimIntNode(numberAttribute, 0),
                                                "the attribute '" + numberAttribute.getName() + "' has a "
                                                  + "minimum value of " + minimum + " but value is '0.0'"));
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for long",
                                                () -> new ScimLongNode(numberAttribute, 0),
                                                "the attribute '" + numberAttribute.getName() + "' has a "
                                                  + "minimum value of " + minimum + " but value is '0.0'"));
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for double",
                                                () -> new ScimDoubleNode(numberAttribute, 0),
                                                "the attribute '" + numberAttribute.getName() + "' has a "
                                                  + "minimum value of " + minimum + " but value is '0.0'"));

    dynamicTests.add(getAttributeValidationTest("minimum value not reached for int (decimalNode)",
                                                () -> new ScimIntNode(decimalAttribute, 0),
                                                "the attribute '" + decimalAttribute.getName() + "' has a "
                                                  + "minimum value of " + minimumDecimal + " but value is '0.0'"));
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for long (decimalNode)",
                                                () -> new ScimLongNode(decimalAttribute, 0),
                                                "the attribute '" + decimalAttribute.getName() + "' has a "
                                                  + "minimum value of " + minimumDecimal + " but value is '0.0'"));
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for double (decimalNode)",
                                                () -> new ScimDoubleNode(decimalAttribute, 0),
                                                "the attribute '" + decimalAttribute.getName() + "' has a "
                                                  + "minimum value of " + minimumDecimal + " but value is '0.0'"));

    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for int",
                                                () -> new ScimIntNode(numberAttribute, 101),
                                                "the attribute '" + numberAttribute.getName() + "' has a "
                                                  + "maximum value of " + maximum + " but value is '101.0'"));
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for long",
                                                () -> new ScimLongNode(numberAttribute, 101),
                                                "the attribute '" + numberAttribute.getName() + "' has a "
                                                  + "maximum value of " + maximum + " but value is '101.0'"));
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for double",
                                                () -> new ScimDoubleNode(numberAttribute, 101.5),
                                                "the attribute '" + numberAttribute.getName() + "' has a "
                                                  + "maximum value of " + maximum + " but value is '101.5'"));

    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for int (decimalNode)",
                                                () -> new ScimIntNode(decimalAttribute, 101),
                                                "the attribute '" + decimalAttribute.getName() + "' has a "
                                                  + "maximum value of " + maximumDecimal + " but value is '101.0'"));
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for long (decimalNode)",
                                                () -> new ScimLongNode(decimalAttribute, 101),
                                                "the attribute '" + decimalAttribute.getName() + "' has a "
                                                  + "maximum value of " + maximumDecimal + " but value is '101.0'"));
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for double (decimalNode)",
                                                () -> new ScimDoubleNode(decimalAttribute, 100.124),
                                                "the attribute '" + decimalAttribute.getName() + "' has a "
                                                  + "maximum value of " + maximumDecimal + " but value is '100.124'"));

    dynamicTests.add(getAttributeValidationTest("not multipleOf for int",
                                                () -> new ScimIntNode(numberAttribute, 10),
                                                "the attribute '" + numberAttribute.getName() + "' must "
                                                  + "be multiple of " + multipleOf + " but value is '10.0'"));
    dynamicTests.add(getAttributeValidationTest("not multipleOf for long",
                                                () -> new ScimLongNode(numberAttribute, 31),
                                                "the attribute '" + numberAttribute.getName() + "' must "
                                                  + "be multiple of " + multipleOf + " but value is '31.0'"));
    dynamicTests.add(getAttributeValidationTest("not multipleOf for double",
                                                () -> new ScimDoubleNode(numberAttribute, 11.4),
                                                "the attribute '" + numberAttribute.getName() + "' must "
                                                  + "be multiple of " + multipleOf + " but value is '11.4'"));

    dynamicTests.add(getAttributeValidationTest("not multipleOf for int (decimalNode)",
                                                () -> new ScimIntNode(decimalAttribute, 12),
                                                "the attribute '" + decimalAttribute.getName() + "' must "
                                                  + "be multiple of " + multipleOfDecimal + " but value is '12.0'"));
    dynamicTests.add(getAttributeValidationTest("not multipleOf for long (decimalNode)",
                                                () -> new ScimLongNode(decimalAttribute, 32),
                                                "the attribute '" + decimalAttribute.getName() + "' must "
                                                  + "be multiple of " + multipleOfDecimal + " but value is '32.0'"));
    dynamicTests.add(getAttributeValidationTest("not multipleOf for double (decimalNode)",
                                                () -> new ScimDoubleNode(decimalAttribute, 31.1),
                                                "the attribute '" + decimalAttribute.getName() + "' must "
                                                  + "be multiple of " + multipleOfDecimal + " but value is '31.1'"));

    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for int", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimIntNode(numberAttribute, 12));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for long", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimLongNode(numberAttribute, 18));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for double", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimDoubleNode(numberAttribute, 15.0));
    }));

    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for int (decimalNode)", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimIntNode(decimalAttribute, 31));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for long (decimalNode)", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimLongNode(decimalAttribute, 62));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for double (decimalNode)", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimDoubleNode(decimalAttribute, 12.4));
    }));
    // @formatter:on
    return dynamicTests;
  }

  /**
   * this test will verify that dateTime attributes are correctly validated
   */
  @TestFactory
  public List<DynamicTest> testValidateDateTimeAttributes()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    SchemaAttribute dateAttribute = schema.getSchemaAttribute("date");
    final Instant notBefore = Instant.parse("2018-11-01T00:00:00Z");
    final Instant notAfter = Instant.parse("2020-12-01T00:00:00Z");

    List<DynamicTest> dynamicTests = new ArrayList<>();
    // @formatter:off
    dynamicTests.add(getAttributeValidationTest("dateTime is before",
                                                () -> new ScimTextNode(dateAttribute,
                                                                       notBefore.minus(1, ChronoUnit.DAYS).toString()),
                                                "the attribute '" + dateAttribute.getName()
                                                  + "' must not be before '" + notBefore.toString()
                                                  + "' but was '2018-10-31T00:00:00Z'"));
    dynamicTests.add(getAttributeValidationTest("dateTime is after",
                                                () -> new ScimTextNode(dateAttribute,
                                                                       notAfter.plus(1, ChronoUnit.DAYS).toString()),
                                                "the attribute '" + dateAttribute.getName()
                                                  + "' must not be after '" + notAfter.toString()
                                                  + "' but was '2020-12-02T00:00:00Z'"));
    dynamicTests.add(DynamicTest.dynamicTest("date time matches not before", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimTextNode(dateAttribute, notBefore.toString()));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("date time matches not after", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimTextNode(dateAttribute, notAfter.toString()));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("date time between notBefore and notAfter", () -> {
      Assertions.assertDoesNotThrow(() -> new ScimTextNode(dateAttribute, "2019-02-17T00:00:00Z"));
    }));
    // @formatter:on
    return dynamicTests;
  }

  /**
   * builds a failing test for attribute validation
   */
  private DynamicTest getAttributeValidationTest(String testName, Supplier<JsonNode> createNode, String errorMessage)
  {
    return DynamicTest.dynamicTest(testName, () -> {
      try
      {
        createNode.get();
        Assertions.fail("this point must not be reached. Error message should be: " + errorMessage);
      }
      catch (DocumentValidationException ex)
      {
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        Assertions.assertEquals(errorMessage, ex.getDetail());
      }
    });
  }

  /**
   * this test will verify that the attribute validation on arrays works correctly
   */
  @TestFactory
  public List<DynamicTest> testValidateArray()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    SchemaAttribute arrayAttribute = schema.getSchemaAttribute("array");
    final int minItems = 3;
    final int maxItems = 10;

    List<DynamicTest> dynamicTests = new ArrayList<>();

    Function<Integer, ArrayNode> getArray = integer -> {
      ScimArrayNode arrayNode = new ScimArrayNode(arrayAttribute);
      for ( int i = 0 ; i < integer ; i++ )
      {
        arrayNode.add(new TextNode(UUID.randomUUID().toString()));
      }
      return arrayNode;
    };

    /* **************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("not enough items", () -> {
      ArrayNode arrayNode = getArray.apply(minItems - 1);
      try
      {
        AttributeValidator.validateArrayNode(arrayAttribute, arrayNode);
        Assertions.fail("this point must not be reached");
      }
      catch (DocumentValidationException ex)
      {
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        Assertions.assertEquals("the multivalued attribute '" + arrayAttribute.getScimNodeName() + "' "
                                + "must have at least " + minItems + " items but array has " + arrayNode.size()
                                + " items and is: " + arrayNode.toString(),
                                ex.getDetail());
      }
    }));
    /* **************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("just enough items", () -> {
      ArrayNode arrayNode = getArray.apply(minItems);
      Assertions.assertDoesNotThrow(() -> AttributeValidator.validateArrayNode(arrayAttribute, arrayNode));
    }));
    /* **************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("too many items", () -> {
      ArrayNode arrayNode = getArray.apply(maxItems + 1);
      try
      {
        AttributeValidator.validateArrayNode(arrayAttribute, arrayNode);
        Assertions.fail("this point must not be reached");
      }
      catch (DocumentValidationException ex)
      {
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        Assertions.assertEquals("the multivalued attribute '" + arrayAttribute.getScimNodeName() + "' "
                                + "must not have more than " + maxItems + " items but array has " + arrayNode.size()
                                + " items and is: " + arrayNode.toString(),
                                ex.getDetail());
      }
    }));
    /* **************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("just not too many items", () -> {
      ArrayNode arrayNode = getArray.apply(maxItems);
      Assertions.assertDoesNotThrow(() -> AttributeValidator.validateArrayNode(arrayAttribute, arrayNode));
    }));
    /* **************************************************************************************************************/
    return dynamicTests;


  }
}
