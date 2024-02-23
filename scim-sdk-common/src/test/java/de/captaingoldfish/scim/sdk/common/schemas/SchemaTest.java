package de.captaingoldfish.scim.sdk.common.schemas;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidSchemaException;
import de.captaingoldfish.scim.sdk.common.utils.FileReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 30.11.2019 - 00:11 <br>
 * <br>
 */
public class SchemaTest implements FileReferences
{

  /**
   * verifies that the validation-test-schema can successfully be parsed and that the validation attributes will
   * be correctly returned
   */
  @Test
  public void testValidationSchema()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    Assertions.assertEquals(5, schema.getSchemaAttribute("minstring").getMinLength().get());
    Assertions.assertEquals(10, schema.getSchemaAttribute("minstring").getMaxLength().get());
    Assertions.assertEquals("(?i)[a-z0-9]+", schema.getSchemaAttribute("minstring").getPattern().get().pattern());

    Assertions.assertEquals(3, schema.getSchemaAttribute("number").getMultipleOf().get());
    Assertions.assertEquals(10, schema.getSchemaAttribute("number").getMinimum().get());
    Assertions.assertEquals(100, schema.getSchemaAttribute("number").getMaximum().get());

    Assertions.assertEquals(3, schema.getSchemaAttribute("array").getMinItems().get());
    Assertions.assertEquals(10, schema.getSchemaAttribute("array").getMaxItems().get());

    Assertions.assertEquals(Instant.parse("2018-11-01T00:00:00Z"),
                            schema.getSchemaAttribute("date").getNotBefore().get());
    Assertions.assertEquals(Instant.parse("2020-12-01T00:00:00Z"),
                            schema.getSchemaAttribute("date").getNotAfter().get());
    // all attributes within this schema are readOnly
    Assertions.assertEquals(schema.getAttributes().size(), schema.getReadOnlyAttributeRegister().size());
    Assertions.assertEquals(0, schema.getImmutableAttributeRegister().size());
  }

  /**
   * verifies that the setter methods for the validation attributes cannot be set if the type is not applicable
   */
  @TestFactory
  public List<DynamicTest> testSetterForValidationAttributes()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    SchemaAttribute stringAttribute = schema.getSchemaAttribute("minstring");
    SchemaAttribute numberAttribute = schema.getSchemaAttribute("number");
    SchemaAttribute dateAttribute = schema.getSchemaAttribute("date");

    final String minimumErrorMessage = "The attribute 'minimum' is only applicable to 'integer' "
                                       + "and 'decimal' types";
    final String maximumErrorMessage = "The attribute 'maximum' is only applicable to 'integer' "
                                       + "and 'decimal' types";
    final String multipleOfErrorMessage = "The attribute 'multipleOf' is only applicable to 'integer' "
                                          + "and 'decimal' types";

    final String maxLengthErrorMessage = "The attribute 'maxLength' is only applicable to 'string' "
                                         + "and 'reference' types";
    final String minLengthErrorMessage = "The attribute 'minLength' is only applicable to 'string' "
                                         + "and 'reference' types";
    final String patternErrorMessage = "The attribute 'pattern' is only applicable to 'string' "
                                       + "and 'reference' types";
    final String minItemsErrorMessage = "The attribute 'minItems' is only applicable to 'multivalued' types";
    final String maxItemsErrorMessage = "The attribute 'maxItems' is only applicable to 'multivalued' types";
    final String notBeforeErrorMessage = "The attribute 'notBefore' is only applicable to 'dateTime' types";
    final String notAfterErrorMessage = "The attribute 'notAfter' is only applicable to 'dateTime' types";

    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getAttributeSetterTest("set multipleOf on string attribute",
                                            () -> stringAttribute.setMultipleOf(3),
                                            multipleOfErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set multipleOf on date attribute",
                                            () -> stringAttribute.setMultipleOf(5),
                                            multipleOfErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set minimum on string attribute",
                                            () -> stringAttribute.setMinimum(1L),
                                            minimumErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set minimum on date attribute",
                                            () -> stringAttribute.setMinimum(1L),
                                            minimumErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set maximum on string attribute",
                                            () -> stringAttribute.setMaximum(1L),
                                            maximumErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set maximum on date attribute",
                                            () -> stringAttribute.setMaximum(1L),
                                            maximumErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set maxLength on number attribute",
                                            () -> numberAttribute.setMaxLength(10L),
                                            maxLengthErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set maxLength on date attribute",
                                            () -> dateAttribute.setMaxLength(10L),
                                            maxLengthErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set minLength on number attribute",
                                            () -> numberAttribute.setMinLength(10L),
                                            minLengthErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set minLength on date attribute",
                                            () -> dateAttribute.setMinLength(10L),
                                            minLengthErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set pattern on number attribute",
                                            () -> numberAttribute.setPattern("klsj"),
                                            patternErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set pattern on date attribute",
                                            () -> dateAttribute.setPattern("klsj"),
                                            patternErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set unparsable pattern on string attribute",
                                            () -> stringAttribute.setPattern(".**"),
                                            "the given pattern is not a valid regular expression '.**'"));
    dynamicTests.add(getAttributeSetterTest("set minItems on non array attribute",
                                            () -> dateAttribute.setMinItems(1),
                                            minItemsErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set maxItems on non array attribute",
                                            () -> dateAttribute.setMaxItems(1),
                                            maxItemsErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set notBefore on string attribute",
                                            () -> stringAttribute.setNotBefore(Instant.now().toString()),
                                            notBeforeErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set notBefore on string attribute",
                                            () -> stringAttribute.setNotBefore(Instant.now()),
                                            notBeforeErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set notBefore on string attribute",
                                            () -> stringAttribute.setNotBefore(LocalDateTime.now()),
                                            notBeforeErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set notBefore on string attribute",
                                            () -> stringAttribute.setNotBefore(OffsetDateTime.now()),
                                            notBeforeErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set notAfter on string attribute",
                                            () -> stringAttribute.setNotAfter(Instant.now().toString()),
                                            notAfterErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set notAfter on string attribute",
                                            () -> stringAttribute.setNotAfter(Instant.now()),
                                            notAfterErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set notAfter on string attribute",
                                            () -> stringAttribute.setNotAfter(LocalDateTime.now()),
                                            notAfterErrorMessage));
    dynamicTests.add(getAttributeSetterTest("set notAfter on string attribute",
                                            () -> stringAttribute.setNotAfter(OffsetDateTime.now()),
                                            notAfterErrorMessage));

    return dynamicTests;
  }

  /**
   * verifies that the notBefore and notAfter attribute can be set successfully with a dateTime attribute
   */
  @TestFactory
  public List<DynamicTest> testSetDateTimeAttributes()
  {
    JsonNode validationSchema = JsonHelper.loadJsonDocument(VALIDATION_TEST_SCHEMA);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(validationSchema));
    SchemaAttribute dateAttribute = schema.getSchemaAttribute("date");

    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(DynamicTest.dynamicTest("Set notBefore with string value", () -> {
      Assertions.assertDoesNotThrow(() -> dateAttribute.setNotBefore(Instant.now().toString()));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("Set notBefore with Instant value", () -> {
      Assertions.assertDoesNotThrow(() -> dateAttribute.setNotBefore(Instant.now()));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("Set notBefore with LocalDateTime value", () -> {
      Assertions.assertDoesNotThrow(() -> dateAttribute.setNotBefore(LocalDateTime.now()));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("Set notBefore with OffsetDateTime value", () -> {
      Assertions.assertDoesNotThrow(() -> dateAttribute.setNotBefore(OffsetDateTime.now()));
    }));

    dynamicTests.add(DynamicTest.dynamicTest("Set notAfter with string value", () -> {
      Assertions.assertDoesNotThrow(() -> dateAttribute.setNotAfter(Instant.now().toString()));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("Set notAfter with Instant value", () -> {
      Assertions.assertDoesNotThrow(() -> dateAttribute.setNotAfter(Instant.now()));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("Set notAfter with LocalDateTime value", () -> {
      Assertions.assertDoesNotThrow(() -> dateAttribute.setNotAfter(LocalDateTime.now()));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("Set notAfter with OffsetDateTime value", () -> {
      Assertions.assertDoesNotThrow(() -> dateAttribute.setNotAfter(OffsetDateTime.now()));
    }));
    return dynamicTests;
  }

  /**
   * @return a test that calls a setter method and expects an {@link InvalidSchemaException} with an internal
   *         server error status and the given error message
   */
  private DynamicTest getAttributeSetterTest(String testName, Runnable setterCall, String errorMessage)
  {
    return DynamicTest.dynamicTest(testName, () -> {
      try
      {
        setterCall.run();
        Assertions.fail("this point must not be reached");
      }
      catch (InvalidSchemaException ex)
      {
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        Assertions.assertEquals(errorMessage, ex.getDetail());
      }
    });
  }
}
