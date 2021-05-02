package de.captaingoldfish.scim.sdk.translator.classbuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.translator.SchemaAttributeBuilder;


/**
 * @author Pascal Knueppel
 * @since 02.05.2021
 */
public class SetterMethodBuilderTest
{

  @Nested
  public class StringTests
  {

    @Test
    public void testRequiredStringAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(String helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredStringAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(String helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedStringAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<String> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedStringAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<String> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueStringAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<String> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueStringAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<String> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }
  }

  @Nested
  public class IntegerTests
  {

    @Test
    public void testRequiredIntegerAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(Long helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredIntegerAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(Long helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedIntegerAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Long> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedIntegerAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Long> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueIntegerAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Long> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueIntegerAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Long> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }
  }

  @Nested
  public class DecimalTests
  {

    @Test
    public void testRequiredDecimalAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(Double helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredDecimalAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(Double helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedDecimalAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Double> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedDecimalAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Double> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueDecimalAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Double> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueDecimalAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Double> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }
  }

  @Nested
  public class BooleanTests
  {

    @Test
    public void testRequiredBooleanAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(Boolean helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredBooleanAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(Boolean helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedBooleanAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Boolean> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedBooleanAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Boolean> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueBooleanAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Boolean> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueBooleanAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Boolean> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }
  }

  @Nested
  public class DateTimeTests
  {

    @Test
    public void testRequiredDateTimeAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(Instant helloWorld)\n" +
                              "  {\n" +
                              "     setDateTimeAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredDateTimeAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(Instant helloWorld)\n" +
                              "  {\n" +
                              "     setDateTimeAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedDateTimeAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Instant> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedDateTimeAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Instant> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueDateTimeAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Instant> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueDateTimeAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<Instant> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttributeList(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }
  }









  @Nested
  public class ComplexTests
  {

    @Test
    public void testRequiredComplexAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(HelloWorld helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredComplexAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(HelloWorld helloWorld)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedComplexAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<HelloWorld> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedComplexAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<HelloWorld> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueComplexAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<HelloWorld> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueComplexAttribute()
    {
      SetterMethodBuilder pojoTranslator = new SetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleSetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public void setHelloWorld(List<HelloWorld> helloWorldList)\n" +
                              "  {\n" +
                              "     setAttribute(FieldNames.HELLO_WORLD, helloWorldList);\n" +
                              "  }\n", method);
      // @formatter:on
    }
  }
}
