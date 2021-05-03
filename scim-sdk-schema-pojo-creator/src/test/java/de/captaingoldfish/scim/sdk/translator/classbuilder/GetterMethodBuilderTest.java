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
 * @since 29.04.2021
 */
public class GetterMethodBuilderTest
{

  @Nested
  public class StringTests
  {

    @Test
    public void testRequiredStringAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public String getHelloWorld()\n" +
                              "  {\n" +
                              "     return getStringAttribute(FieldNames.HELLO_WORLD).orElse(null);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredStringAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Optional<String> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getStringAttribute(FieldNames.HELLO_WORLD);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedStringAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<String> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedStringAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<String> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueStringAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<String> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueStringAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.STRING)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<String> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD);\n" +
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
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Long getHelloWorld()\n" +
                              "  {\n" +
                              "     return getLongAttribute(FieldNames.HELLO_WORLD).orElse(null);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredIntegerAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Optional<Long> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getLongAttribute(FieldNames.HELLO_WORLD);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedIntegerAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .description("hello world")
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** hello world */\n" +
                              "  public List<Long> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD, Long.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedIntegerAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<Long> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD, Long.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueIntegerAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<Long> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD, Long.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueIntegerAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.INTEGER)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<Long> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD, Long.class);\n" +
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
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Double getHelloWorld()\n" +
                              "  {\n" +
                              "     return getDoubleAttribute(FieldNames.HELLO_WORLD).orElse(null);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredDecimalAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Optional<Double> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getDoubleAttribute(FieldNames.HELLO_WORLD);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedDecimalAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<Double> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD, Double.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedDecimalAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<Double> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD, Double.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueDecimalAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<Double> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD, Double.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueDecimalAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DECIMAL)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<Double> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD, Double.class);\n" +
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
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Boolean getHelloWorld()\n" +
                              "  {\n" +
                              "     return getBooleanAttribute(FieldNames.HELLO_WORLD).orElse(false);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredBooleanAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Optional<Boolean> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getBooleanAttribute(FieldNames.HELLO_WORLD);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedBooleanAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<Boolean> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD, Boolean.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedBooleanAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<Boolean> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD, Boolean.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueBooleanAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<Boolean> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD, Boolean.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueBooleanAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.BOOLEAN)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<Boolean> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD, Boolean.class);\n" +
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
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Instant getHelloWorld()\n" +
                              "  {\n" +
                              "     return getDateTimeAttribute(FieldNames.HELLO_WORLD).orElse(null);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredDateTimeAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Optional<Instant> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getDateTimeAttribute(FieldNames.HELLO_WORLD);\n" +
                              "  }\n", method);
      // @formatter:on
    }



    @Test
    public void testRequiredMultivaluedDateTimeAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<Instant> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD, Instant.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedDateTimeAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<Instant> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttribute(FieldNames.HELLO_WORLD, Instant.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueDateTimeAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<Instant> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD, Instant.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueDateTimeAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.DATE_TIME)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Set<Instant> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getSimpleArrayAttributeSet(FieldNames.HELLO_WORLD, Instant.class);\n" +
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
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public HelloWorld getHelloWorld()\n" +
                              "  {\n" +
                              "     return getObjectAttribute(FieldNames.HELLO_WORLD, HelloWorld.class).orElse(null);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredComplexAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(false)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public Optional<HelloWorld> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getObjectAttribute(FieldNames.HELLO_WORLD, HelloWorld.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }



    @Test
    public void testRequiredMultivaluedComplexAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<HelloWorld> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getArrayAttribute(FieldNames.HELLO_WORLD, HelloWorld.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedComplexAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<HelloWorld> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getArrayAttribute(FieldNames.HELLO_WORLD, HelloWorld.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testRequiredMultivaluedUniqueComplexAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(true)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<HelloWorld> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getArrayAttribute(FieldNames.HELLO_WORLD, HelloWorld.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }

    @Test
    public void testNoneRequiredMultivaluedUniqueComplexAttribute()
    {
      GetterMethodBuilder pojoTranslator = new GetterMethodBuilder();
      SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                              .name("helloWorld")
                                                              .type(Type.COMPLEX)
                                                              .required(false)
                                                              .multivalued(true)
                                                              .uniqueness(Uniqueness.SERVER)
                                                              .build();
      String method = pojoTranslator.generateSimpleGetterMethod(schemaAttribute);
      // @formatter:off
      Assertions.assertEquals("  /** A description */\n" +
                              "  public List<HelloWorld> getHelloWorld()\n" +
                              "  {\n" +
                              "     return getArrayAttribute(FieldNames.HELLO_WORLD, HelloWorld.class);\n" +
                              "  }\n", method);
      // @formatter:on
    }
  }
}
