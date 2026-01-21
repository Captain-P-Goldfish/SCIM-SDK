package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;


/**
 * author Pascal Knueppel <br>
 * created at: 30.11.2019 - 22:48 <br>
 * <br>
 */
public class CustomAttributeValidatorTest implements FileReferences
{


  /**
   * verifies that a string-type attribute validation works correctly
   */
  @TestFactory
  public List<DynamicTest> testValidateStringTypeAttribute()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(FileReferences.VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    SchemaAttribute stringAttribute = schema.getSchemaAttribute("minstring");

    List<DynamicTest> dynamicTests = new ArrayList<>();

    String expectedErrorMessage = "The 'STRING'-attribute 'minString' with value 'a' must have a minimum length of '5' "
                                  + "characters but is '1' characters long";
    dynamicTests.add(getAttributeValidationTest("minimum value not reached",
                                                () -> CustomAttributeValidator.validateTextNode(stringAttribute,
                                                                                                new TextNode("a")),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'STRING'-attribute 'minString' with value '01234567890' must not be longer than '10' "
                           + "characters but is '11' characters long";
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded",
                                                () -> CustomAttributeValidator.validateTextNode(stringAttribute,
                                                                                                new TextNode("01234567890")),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'STRING'-attribute 'minString' with value '$%&/!' must match the regular expression "
                           + "of '(?i)[a-z0-9]+'";
    dynamicTests.add(getAttributeValidationTest("pattern does not match",
                                                () -> CustomAttributeValidator.validateTextNode(stringAttribute,
                                                                                                new TextNode("$%&/!")),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("validation matches minimumLength exactly", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateTextNode(stringAttribute,
                                                                                    new TextNode("12345")));
    }));
    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("validation matches maximumLength exactly", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateTextNode(stringAttribute,
                                                                                    new TextNode("0123456789")));
    }));
    return dynamicTests;
  }

  /**
   * verifies that a number-type attribute validation works correctly
   */
  @TestFactory
  public List<DynamicTest> testValidateNumberTypeAttribute()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(FileReferences.VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    SchemaAttribute numberAttribute = schema.getSchemaAttribute("number");
    SchemaAttribute decimalAttribute = schema.getSchemaAttribute("decimal");

    List<DynamicTest> dynamicTests = new ArrayList<>();

    /* ****************************************************************************************************** */
    String expectedErrorMessage = "The 'INTEGER'-attribute 'number' with value '0' must have at least a value of '10'";
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for int",
                                                () -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                                  new IntNode(0)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for long",
                                                () -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                                  new LongNode(0)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for double",
                                                () -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                                  new DoubleNode(0)),
                                                expectedErrorMessage));

    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'DECIMAL'-attribute 'decimal' with value '0' must have at least a value of '10.5'";
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for int (decimalNode)",
                                                () -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                                  new IntNode(0)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for long (decimalNode)",
                                                () -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                                  new LongNode(0)),
                                                expectedErrorMessage));
    expectedErrorMessage = "The 'DECIMAL'-attribute 'decimal' with value '0.0' must have at least a value of '10.5'";
    dynamicTests.add(getAttributeValidationTest("minimum value not reached for double (decimalNode)",
                                                () -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                                  new DoubleNode(0)),
                                                expectedErrorMessage));

    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'INTEGER'-attribute 'number' with value '101' must not be greater than '100'";
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for int",
                                                () -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                                  new IntNode(101)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for long",
                                                () -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                                  new LongNode(101)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'INTEGER'-attribute 'number' with value '101' must not be greater than '100'";
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for double",
                                                () -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                                  new DoubleNode(101.5)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'DECIMAL'-attribute 'decimal' with value '101' must not be greater than '100.123456789'";
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for int (decimalNode)",
                                                () -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                                  new IntNode(101)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for long (decimalNode)",
                                                () -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                                  new LongNode(101)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'DECIMAL'-attribute 'decimal' with value '100.124' must not be greater than '100.123456789'";
    dynamicTests.add(getAttributeValidationTest("maximum value exceeded for double (decimalNode)",
                                                () -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                                  new DoubleNode(100.124)),
                                                expectedErrorMessage));

    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'INTEGER'-attribute 'number' with value '10' must be a multiple of '3.0'";
    dynamicTests.add(getAttributeValidationTest("not multipleOf for int",
                                                () -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                                  new IntNode(10)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'INTEGER'-attribute 'number' with value '31' must be a multiple of '3.0'";
    dynamicTests.add(getAttributeValidationTest("not multipleOf for long",
                                                () -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                                  new LongNode(31)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'INTEGER'-attribute 'number' with value '11' must be a multiple of '3.0'";
    dynamicTests.add(getAttributeValidationTest("not multipleOf for double",
                                                () -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                                  new DoubleNode(11.4)),
                                                expectedErrorMessage));

    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'DECIMAL'-attribute 'decimal' with value '12' must be a multiple of '3.1'";
    dynamicTests.add(getAttributeValidationTest("not multipleOf for int (decimalNode)",
                                                () -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                                  new IntNode(12)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'DECIMAL'-attribute 'decimal' with value '32' must be a multiple of '3.1'";
    dynamicTests.add(getAttributeValidationTest("not multipleOf for long (decimalNode)",
                                                () -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                                  new LongNode(32)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'DECIMAL'-attribute 'decimal' with value '31.1' must be a multiple of '3.1'";
    dynamicTests.add(getAttributeValidationTest("not multipleOf for double (decimalNode)",
                                                () -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                                  new DoubleNode(31.1)),
                                                expectedErrorMessage));
    /* ****************************************************************************************************** */

    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for int", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                      new IntNode(12)));
    }));
    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for long", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                      new LongNode(18)));
    }));
    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for double", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateNumberNode(numberAttribute,
                                                                                      new DoubleNode(15.0)));
    }));

    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for int (decimalNode)", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                      new IntNode(31)));
    }));
    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for long (decimalNode)", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                      new LongNode(62)));
    }));
    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("value matches requirements for double (decimalNode)", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateNumberNode(decimalAttribute,
                                                                                      new DoubleNode(12.4)));
    }));
    return dynamicTests;
  }

  /**
   * this test will verify that dateTime attributes are correctly validated
   */
  @TestFactory
  public List<DynamicTest> testValidateDateTimeAttributes()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(FileReferences.VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    SchemaAttribute dateAttribute = schema.getSchemaAttribute("date");
    final Instant notBefore = Instant.parse("2018-11-01T00:00:00Z");
    final Instant notAfter = Instant.parse("2020-12-01T00:00:00Z");

    List<DynamicTest> dynamicTests = new ArrayList<>();

    /* ****************************************************************************************************** */
    String expectedErrorMessage = "The 'DATE_TIME'-attribute 'date' with value '2018-10-31T00:00:00Z' must not "
                                  + "be before '2018-11-01T00:00:00Z'";
    dynamicTests.add(getAttributeValidationTest("dateTime is before", () -> {
      CustomAttributeValidator.validateTextNode(dateAttribute,
                                                new TextNode(notBefore.minus(1, ChronoUnit.DAYS).toString()));
    }, expectedErrorMessage));
    /* ****************************************************************************************************** */
    expectedErrorMessage = "The 'DATE_TIME'-attribute 'date' with value '2020-12-02T00:00:00Z' must not be after "
                           + "'2020-12-01T00:00:00Z'";
    dynamicTests.add(getAttributeValidationTest("dateTime is after", () -> {
      CustomAttributeValidator.validateTextNode(dateAttribute,
                                                new TextNode(notAfter.plus(1, ChronoUnit.DAYS).toString()));
    }, expectedErrorMessage));
    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("date time matches not before", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateTextNode(dateAttribute,
                                                                                    new TextNode(notBefore.toString())));
    }));
    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("date time matches not after", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateTextNode(dateAttribute,
                                                                                    new TextNode(notAfter.toString())));
    }));
    /* ****************************************************************************************************** */
    dynamicTests.add(DynamicTest.dynamicTest("date time between notBefore and notAfter", () -> {
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateTextNode(dateAttribute,
                                                                                    new TextNode("2019-02-17T00:00:00Z")));
    }));
    // @formatter:on
    return dynamicTests;
  }

  /**
   * builds a failing test for attribute validation
   */
  private DynamicTest getAttributeValidationTest(String testName, Runnable runValidation, String expectedErrorMessage)
  {
    return DynamicTest.dynamicTest(testName, () -> {
      try
      {
        runValidation.run();
        Assertions.fail("this point must not be reached. Error message should be: " + expectedErrorMessage);
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(expectedErrorMessage, ex.getMessage());
      }
    });
  }

  /**
   * this test will verify that the attribute validation on arrays works correctly
   */
  @TestFactory
  public List<DynamicTest> testValidateArray()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(FileReferences.VALIDATION_TEST_SCHEMA);
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
        CustomAttributeValidator.validateArrayNode(arrayAttribute, arrayNode);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        String expectedErrorMessage = String.format("The 'ARRAY'-attribute 'array' with value '%s' must have at "
                                                    + "least '3' items but only '2' items are present",
                                                    arrayNode);
        Assertions.assertEquals(expectedErrorMessage, ex.getMessage());
      }
    }));
    /* **************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("just enough items", () -> {
      ArrayNode arrayNode = getArray.apply(minItems);
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateArrayNode(arrayAttribute, arrayNode));
    }));
    /* **************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("too many items", () -> {
      ArrayNode arrayNode = getArray.apply(maxItems + 1);
      try
      {
        CustomAttributeValidator.validateArrayNode(arrayAttribute, arrayNode);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        String expectedErrorMessage = String.format("The 'ARRAY'-attribute 'array' with value '%s' must not have "
                                                    + "more than '10' items. Items found '11'",
                                                    arrayNode);
        Assertions.assertEquals(expectedErrorMessage, ex.getMessage());
      }
    }));
    /* **************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("just not too many items", () -> {
      ArrayNode arrayNode = getArray.apply(maxItems);
      Assertions.assertDoesNotThrow(() -> CustomAttributeValidator.validateArrayNode(arrayAttribute, arrayNode));
    }));
    /* **************************************************************************************************************/
    return dynamicTests;


  }
}
