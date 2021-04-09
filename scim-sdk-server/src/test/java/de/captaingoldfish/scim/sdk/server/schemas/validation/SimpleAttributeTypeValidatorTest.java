package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimBooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimDoubleNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimIntNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimLongNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import de.captaingoldfish.scim.sdk.server.utils.SchemaAttributeBuilder;


/**
 * <p>
 * Copyright &copy; 2009-2020 Governikus GmbH &amp; Co. KG
 * </p>
 *
 * @author Pascal Knüppel
 * @since 09.04.2021
 */
public class SimpleAttributeTypeValidatorTest
{

  /**
   * test arguments for the test {@link ReferenceFailureTestBuilder#testAsNoneString(JsonNode, ReferenceTypes)}
   */
  public static Stream<Arguments> getReferenceFailureTestArguments()
  {

    return Stream.of(Arguments.arguments(new IntNode(5), ReferenceTypes.RESOURCE),
                     Arguments.arguments(new LongNode(Long.MAX_VALUE), ReferenceTypes.RESOURCE),
                     Arguments.arguments(BooleanNode.getTrue(), ReferenceTypes.RESOURCE),
                     Arguments.arguments(new DoubleNode(25.5), ReferenceTypes.RESOURCE),
                     Arguments.arguments(new IntNode(5), ReferenceTypes.URI),
                     Arguments.arguments(new LongNode(Long.MAX_VALUE), ReferenceTypes.URI),
                     Arguments.arguments(BooleanNode.getTrue(), ReferenceTypes.URI),
                     Arguments.arguments(new DoubleNode(25.5), ReferenceTypes.URI),
                     Arguments.arguments(new IntNode(5), ReferenceTypes.EXTERNAL),
                     Arguments.arguments(new LongNode(Long.MAX_VALUE), ReferenceTypes.EXTERNAL),
                     Arguments.arguments(BooleanNode.getTrue(), ReferenceTypes.EXTERNAL),
                     Arguments.arguments(new DoubleNode(25.5), ReferenceTypes.EXTERNAL));
  }

  /**
   * defines the arguments for test {@link #testIsSimpleNodeType(JsonNode)}
   */
  public static Stream<Arguments> getSimpleNodeTypes()
  {
    return Stream.of(Arguments.arguments(NullNode.getInstance()),
                     Arguments.arguments(new TextNode("text")),
                     Arguments.arguments(new IntNode(5)),
                     Arguments.arguments(new LongNode(5L)),
                     Arguments.arguments(new DoubleNode(5.5)),
                     Arguments.arguments(BooleanNode.getTrue()));
  }

  /**
   * defines the arguments for test {@link #testIsNotSimpleNodeType(JsonNode)}
   */
  public static Stream<Arguments> getNoneSimpleNodeTypes()
  {
    return Stream.of(Arguments.arguments(new ObjectNode(JsonNodeFactory.instance)),
                     Arguments.arguments(new ArrayNode(JsonNodeFactory.instance)));
  }

  /**
   * shows that all nodes used in this api are identified as simple node types
   */
  @ParameterizedTest
  @MethodSource("getSimpleNodeTypes")
  public void testIsSimpleNodeType(JsonNode jsonNode)
  {
    Assertions.assertTrue(SimpleAttributeValidator.isSimpleNode(jsonNode));
  }

  /**
   * checks that array and object nodes are not interpreted as simple node types
   */
  @ParameterizedTest
  @MethodSource("getNoneSimpleNodeTypes")
  public void testIsNotSimpleNodeType(JsonNode jsonNode)
  {
    Assertions.assertFalse(SimpleAttributeValidator.isSimpleNode(jsonNode));
  }

  /**
   * check for canonical values case insensitive
   */
  @Test
  public void testCanonicalStringValuesCaseInsensitive()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .canonicalValues("hello", "world")
                                                            .caseExact(false)
                                                            .build();

    {
      String content = "hello";
      JsonNode attribute = new TextNode(content);
      Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute));
    }

    {
      String content = "world";
      JsonNode attribute = new TextNode(content);
      Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute));
    }

    {
      String content = "hElLo";
      JsonNode attribute = new TextNode(content);
      Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute));
    }

    {
      String content = "wOrLd";
      JsonNode attribute = new TextNode(content);
      Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute));
    }

    {
      String content = "foo";
      JsonNode attribute = new TextNode(content);
      try
      {
        SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        final String errorMessage = String.format("Attribute with name '%s' does not match one of "
                                                  + "its canonicalValues '%s' actual value is '%s'",
                                                  schemaAttribute.getFullResourceName(),
                                                  schemaAttribute.getCanonicalValues(),
                                                  content);
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
    }
  }

  /**
   * check for canonical values case sensitive
   */
  @Test
  public void testCanonicalStringValuesCaseSensitive()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .canonicalValues("hello", "world")
                                                            .caseExact(true)
                                                            .build();

    {
      String content = "hello";
      JsonNode attribute = new TextNode(content);
      Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute));
    }

    {
      String content = "world";
      JsonNode attribute = new TextNode(content);
      Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute));
    }

    {
      String content = "hElLo";
      JsonNode attribute = new TextNode(content);
      try
      {
        SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        final String errorMessage = String.format("Attribute with name '%s' is caseExact and does not match "
                                                  + "its canonicalValues '%s' actual value is '%s'",
                                                  schemaAttribute.getFullResourceName(),
                                                  schemaAttribute.getCanonicalValues(),
                                                  content);
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
    }

    {
      String content = "wOrLd";
      JsonNode attribute = new TextNode(content);
      try
      {
        SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        final String errorMessage = String.format("Attribute with name '%s' is caseExact and does not match "
                                                  + "its canonicalValues '%s' actual value is '%s'",
                                                  schemaAttribute.getFullResourceName(),
                                                  schemaAttribute.getCanonicalValues(),
                                                  content);
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
    }

    {
      String content = "foo";
      JsonNode attribute = new TextNode(content);
      try
      {
        SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        final String errorMessage = String.format("Attribute with name '%s' does not match one of "
                                                  + "its canonicalValues '%s' actual value is '%s'",
                                                  schemaAttribute.getFullResourceName(),
                                                  schemaAttribute.getCanonicalValues(),
                                                  content);
        Assertions.assertEquals(errorMessage, ex.getMessage());
      }
    }
  }

  /**
   * check for canonical values case sensitive
   */
  @Test
  public void testCanonicalStringValuesWithoutAnyCanonicalValues()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.STRING).build();

    String content = "hello";
    JsonNode attribute = new TextNode(content);
    Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.checkCanonicalValues(schemaAttribute, attribute));
  }

  /**
   * verifies that string values are correctly parsed
   */
  @Test
  public void testStringType()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.STRING).build();

    final String content = UUID.randomUUID().toString();
    JsonNode attribute = new TextNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(content, parsedNode.textValue());
  }

  /**
   * verifies that integer values are correctly parsed
   */
  @Test
  public void testIntegerType()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.INTEGER).build();

    final int content = new Random().nextInt();
    JsonNode attribute = new IntNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimIntNode.class));
    Assertions.assertEquals(content, parsedNode.intValue());
  }

  /**
   * verifies that long values are correctly parsed to long nodes
   */
  @Test
  public void testIntegerTypeWithLong()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.INTEGER).build();

    final long content = Long.MAX_VALUE;
    JsonNode attribute = new LongNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimLongNode.class));
    Assertions.assertEquals(content, parsedNode.longValue());
  }

  /**
   * verifies that boolean values are correctly parsed to boolean nodes
   */
  @Test
  public void testBooleanType()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.BOOLEAN).build();

    JsonNode attribute = BooleanNode.getTrue();
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimBooleanNode.class));
    Assertions.assertEquals(attribute.booleanValue(), parsedNode.booleanValue());
  }

  /**
   * tests that decimal values are correctly parsed to double nodes
   */
  @Test
  public void testDecimalType()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DECIMAL).build();

    JsonNode attribute = new DoubleNode(25.5);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimDoubleNode.class));
    Assertions.assertEquals(attribute.doubleValue(), parsedNode.doubleValue());
  }

  /**
   * tests that integer values on decimal types are correctly parsed to double nodes
   */
  @Test
  public void testDecimalTypeWithInteger()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DECIMAL).build();

    JsonNode attribute = new IntNode(25);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimDoubleNode.class));
    Assertions.assertEquals(attribute.doubleValue(), parsedNode.doubleValue());
  }

  /**
   * shows that long values on decimal types are correctly parsed to double nodes
   */
  @Test
  public void testDecimalTypeWithLong()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DECIMAL).build();

    JsonNode attribute = new LongNode(Long.MAX_VALUE);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimDoubleNode.class));
    Assertions.assertEquals(attribute.doubleValue(), parsedNode.doubleValue());
  }

  /**
   * shows that a date time value is correctly parsed into a string node
   */
  @Test
  public void testDateTimeType()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DATE_TIME).build();

    DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
    String content = formatter.format(Instant.now());
    JsonNode attribute = new TextNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(attribute.textValue(), parsedNode.textValue());
  }

  /**
   * shows that a reference of type resource is correctly parsed to a string node
   */
  @Test
  public void testReferenceTypeResource()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.REFERENCE)
                                                            .referenceTypes(ReferenceTypes.RESOURCE)
                                                            .build();

    String content = "http://localhost:8080/auth/somewhere/myResource/123";
    JsonNode attribute = new TextNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(content, parsedNode.textValue());
  }

  /**
   * shows that a reference of type uri is correctly parsed to a string node
   */
  @Test
  public void testReferenceTypeUri()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.REFERENCE)
                                                            .referenceTypes(ReferenceTypes.URI)
                                                            .build();

    String content = "urn:captaingoldfish:scim:custom:identifier";
    JsonNode attribute = new TextNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(content, parsedNode.textValue());
  }

  /**
   * shows that a reference of type external is correctly parsed to a string node
   */
  @Test
  public void testReferenceTypeExternal()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.REFERENCE)
                                                            .referenceTypes(ReferenceTypes.EXTERNAL)
                                                            .build();

    String content = "id: 25";
    JsonNode attribute = new TextNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(content, parsedNode.textValue());
  }

  /**
   * shows that a reference is correctly turned into a string node no matter if the type is resource or uri
   */
  @ParameterizedTest
  @ValueSource(strings = {"http://localhost:8080/auth/somewhere/myResource/123",
                          "urn:captaingoldfish:scim:custom:identifier"})
  public void testReferenceTypeResourceOrUri(String content)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.REFERENCE)
                                                            .referenceTypes(ReferenceTypes.RESOURCE, ReferenceTypes.URI)
                                                            .build();

    JsonNode attribute = new TextNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(content, parsedNode.textValue());
  }

  /**
   * shows that a reference is correctly parsed into a string node no matter if the type is resource or external
   */
  @ParameterizedTest
  @ValueSource(strings = {"http://localhost:8080/auth/somewhere/myResource/123", "id$25"})
  public void testReferenceTypeResourceOrExternal(String content)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.REFERENCE)
                                                            .referenceTypes(ReferenceTypes.RESOURCE,
                                                                            ReferenceTypes.EXTERNAL)
                                                            .build();

    JsonNode attribute = new TextNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(content, parsedNode.textValue());
  }

  /**
   * shows that a reference is correctly parsed into a string node no matter if the type is uri or external
   */
  @ParameterizedTest
  @ValueSource(strings = {"urn:captaingoldfish:scim:custom:identifier", "id: 25"})
  public void testReferenceTypeUriOrExternal(String content)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.REFERENCE)
                                                            .referenceTypes(ReferenceTypes.URI, ReferenceTypes.EXTERNAL)
                                                            .build();

    JsonNode attribute = new TextNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(content, parsedNode.textValue());
  }

  /**
   * shows that a reference is correctly parsed into a string node no matter if the type is resource, uri or
   * external
   */
  @ParameterizedTest
  @ValueSource(strings = {"http://localhost:8080/auth/somewhere/myResource/123",
                          "urn:captaingoldfish:scim:custom:identifier", "id: 25"})
  public void testReferenceTypeResourceOrUriOrExternal(String content)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.REFERENCE)
                                                            .referenceTypes(ReferenceTypes.RESOURCE,
                                                                            ReferenceTypes.URI,
                                                                            ReferenceTypes.EXTERNAL)
                                                            .build();

    JsonNode attribute = new TextNode(content);
    JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                     attribute));
    Assertions.assertNotNull(parsedNode);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(content, parsedNode.textValue());
  }

  /**
   * tests that an any-type field is parsed to any type as long as the data type matches
   */
  @TestFactory
  public List<DynamicTest> testAnyTypeAsString()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.ANY).build();

    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(DynamicTest.dynamicTest("testAnyTypeAsString", () -> {
      String content = "hello-world";
      JsonNode attribute = new TextNode(content);
      JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                       attribute));
      Assertions.assertNotNull(parsedNode);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
      Assertions.assertEquals(content, parsedNode.textValue());
    }));
    dynamicTests.add(DynamicTest.dynamicTest("testAnyTypeAsInteger", () -> {
      int content = 5;
      JsonNode attribute = new IntNode(content);
      JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                       attribute));
      Assertions.assertNotNull(parsedNode);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimLongNode.class));
      Assertions.assertEquals(content, parsedNode.intValue());
    }));
    dynamicTests.add(DynamicTest.dynamicTest("testAnyTypeAsLong", () -> {
      long content = Long.MAX_VALUE;
      JsonNode attribute = new LongNode(content);
      JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                       attribute));
      Assertions.assertNotNull(parsedNode);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimLongNode.class));
      Assertions.assertEquals(content, parsedNode.longValue());
    }));
    dynamicTests.add(DynamicTest.dynamicTest("testAnyTypeAsDecimal", () -> {
      double content = 17.9;
      JsonNode attribute = new DoubleNode(content);
      JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                       attribute));
      Assertions.assertNotNull(parsedNode);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimDoubleNode.class));
      Assertions.assertEquals(content, parsedNode.doubleValue());
    }));
    dynamicTests.add(DynamicTest.dynamicTest("testAnyTypeAsBoolean", () -> {
      JsonNode attribute = BooleanNode.getTrue();
      JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                       attribute));
      Assertions.assertNotNull(parsedNode);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimBooleanNode.class));
      Assertions.assertTrue(parsedNode.booleanValue());
    }));
    dynamicTests.add(DynamicTest.dynamicTest("testAnyTypeAsNull", () -> {
      JsonNode attribute = NullNode.getInstance();
      JsonNode parsedNode = Assertions.assertDoesNotThrow(() -> SimpleAttributeValidator.parseNodeType(schemaAttribute,
                                                                                                       attribute));
      Assertions.assertNotNull(parsedNode);
      Assertions.assertTrue(parsedNode.isNull());
    }));
    return dynamicTests;
  }

  /**
   * failing tests for string types
   */
  @Nested
  public class StringFailureTestBuilder
  {

    /**
     * will verify that a string value will not be parsed into into a an integer value
     */
    @Test
    public void testWithInteger()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.STRING).build();

      JsonNode attribute = new IntNode(0);
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a string value will not be parsed into into a long value
     */
    @Test
    public void testWithLong()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.STRING).build();

      JsonNode attribute = new LongNode(1L);
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a string value will not be parsed into into a boolean value
     */
    @Test
    public void testWithBoolean()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.STRING).build();

      JsonNode attribute = BooleanNode.getTrue();
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.BOOLEAN.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }

    }

    /**
     * will verify that a string value will not be parsed into into a decimal value
     */
    @Test
    public void testWithDouble()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.STRING).build();

      JsonNode attribute = new DoubleNode(2.5);
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }
  }

  /**
   * failing tests for integer types
   */
  @Nested
  public class IntegerFailureTestBuilder
  {

    /**
     * will verify that an integer value will not be parsed into into a string value
     */
    @Test
    public void testWithString()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.INTEGER).build();

      JsonNode attribute = new TextNode("abc");
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.STRING.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that an integer value will not be parsed into into an integer if the value is a string value
     */
    @Test
    public void testWithStringNumber()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.INTEGER).build();

      JsonNode attribute = new TextNode("123456");
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.STRING.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that an integer value will not be parsed into into a boolean value
     */
    @Test
    public void testWithBoolean()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.INTEGER).build();

      JsonNode attribute = BooleanNode.getTrue();
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.BOOLEAN.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that an integer value will not be parsed into into a decimal value
     */
    @Test
    public void testWithDouble()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.INTEGER).build();

      JsonNode attribute = new DoubleNode(2.5);
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }
  }

  /**
   * failing tests for boolean attributes
   */
  @Nested
  public class BooleanFailureTestBuilder
  {

    /**
     * will verify that a boolean value will not be parsed into into a string value
     */
    @Test
    public void testWithString()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.BOOLEAN).build();

      JsonNode attribute = new TextNode("abc");
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.STRING.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a boolean value will not be parsed into into an integer value
     */
    @Test
    public void testWithInteger()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.BOOLEAN).build();

      JsonNode attribute = new IntNode(123456);
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a boolean value will not be parsed into into a long value
     */
    @Test
    public void testWithLong()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.BOOLEAN).build();

      JsonNode attribute = new LongNode(123456);
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a boolean value will not be parsed into into a decimal value
     */
    @Test
    public void testWithDouble()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.BOOLEAN).build();

      JsonNode attribute = new DoubleNode(123456.6);
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }
  }

  /**
   * failing tests for decimal value attributes
   */
  @Nested
  public class DecimalFailureTestBuilder
  {

    /**
     * will verify that a string value will not be parsed into into a decimal value
     */
    @Test
    public void testWithString()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DECIMAL).build();

      JsonNode attribute = new TextNode("abc");
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.STRING.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a number string value will not be parsed into into a decimal value. After all its to be
     * interpreted as a string value not a number
     */
    @Test
    public void testNumberString()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DECIMAL).build();

      JsonNode attribute = new TextNode("5.5");
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.STRING.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a boolean value will not be parsed into into a decimal value
     */
    @Test
    public void testWithBoolean()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DECIMAL).build();

      JsonNode attribute = BooleanNode.getTrue();
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.BOOLEAN.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }
  }

  /**
   * failing tests for date time attributes
   */
  @Nested
  public class DateTimeFailureTestBuilder
  {

    /**
     * will verify that a none date string value will not be parsed into into a date value
     */
    @Test
    public void testWithString()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DATE_TIME).build();

      try
      {
        JsonNode attribute = new TextNode("abc");
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = "Given value is not a valid dateTime 'abc'";
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a boolean value will not be parsed into into a date value
     */
    @Test
    public void testWithBoolean()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DATE_TIME).build();

      JsonNode attribute = BooleanNode.getTrue();
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.BOOLEAN.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that an integer value will not be parsed into into a date value
     */
    @Test
    public void testWithInteger()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DATE_TIME).build();

      JsonNode attribute = new IntNode(123456);
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a long value will not be parsed into into a date value
     */
    @Test
    public void testWithLong()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DATE_TIME).build();

      JsonNode attribute = new LongNode(Instant.now().getEpochSecond());
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that a decimal value will not be parsed into into a date value
     */
    @Test
    public void testWithDecimal()
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder().name("id").type(Type.DATE_TIME).build();

      JsonNode attribute = new DoubleNode(Instant.now().getEpochSecond() + 0.2);
      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(JsonNodeType.NUMBER.toString()),
                                               attribute);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }
  }

  /**
   * failing tests for reference type attributes
   */
  @Nested
  public class ReferenceFailureTestBuilder
  {

    /**
     * will verify that an error occurs if a resource locator is expected but not given
     */
    @ParameterizedTest
    @ValueSource(strings = {"RESOURCE", "URI"})
    public void testAsNoResourceLocator(ReferenceTypes referenceType)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.REFERENCE)
                                                              .referenceTypes(referenceType)
                                                              .build();

      String content = "$%&abc";
      try
      {
        JsonNode attribute = new TextNode(content);
        SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Given value is not a valid reference type. The value '%s' is "
                                               + "expected to be of one of the following values: %s",
                                               content,
                                               Collections.singletonList(referenceType));
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }

    /**
     * will verify that an error occurs if json node representation and schema attribute representation do not
     * match for reference type attributes
     */
    @ParameterizedTest
    @MethodSource("de.captaingoldfish.scim.sdk.server.schemas.validation.SimpleAttributeTypeValidatorTest#getReferenceFailureTestArguments")
    public void testAsNoneString(JsonNode jsonNode, ReferenceTypes referenceType)
    {
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("id")
                                                              .type(Type.REFERENCE)
                                                              .referenceTypes(referenceType)
                                                              .build();

      try
      {
        SimpleAttributeValidator.parseNodeType(schemaAttribute, jsonNode);
        Assertions.fail("this point must not be reached");
      }
      catch (AttributeValidationException ex)
      {
        Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
        String expectedMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(jsonNode.getNodeType().toString()),
                                               jsonNode);
        Assertions.assertEquals(expectedMessage, ex.getMessage());
      }
    }
  }

}
