package de.captaingoldfish.scim.sdk.translator.classbuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.translator.SchemaAttributeBuilder;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 02.05.2021
 */
@Slf4j
public class ComplexAttributeToInnerClassBuilderTest
{

  @Test
  public void testComplexAttributeBuilderTest()
  {
    SchemaAttribute aStringAttribute = SchemaAttributeBuilder.builder()
                                                             .name("aString")
                                                             .type(Type.STRING)
                                                             .required(true)
                                                             .build();
    SchemaAttribute anIntegerAttribute = SchemaAttributeBuilder.builder()
                                                               .name("anInteger")
                                                               .type(Type.INTEGER)
                                                               .required(true)
                                                               .build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("helloWorld")
                                                            .type(Type.COMPLEX)
                                                            .subAttributes(aStringAttribute, anIntegerAttribute)
                                                            .build();


    ComplexAttributeToInnerClassBuilder complexAttributeToInnerClassBuilder = new ComplexAttributeToInnerClassBuilder();
    String complexAttributeString = complexAttributeToInnerClassBuilder.generateComplexAttributeClass(schemaAttribute);
    // @formatter:off
    Assertions.assertEquals("    /** A description */\n" +
    "  public Optional<HelloWorld> getHelloWorld()\n" +
    "  {\n" +
    "     return getObjectAttribute(FieldNames.HELLO_WORLD, HelloWorld.class);\n" +
    "  }\n" +
    "\n" +
    "    /** A description */\n" +
    "  public void setHelloWorld(HelloWorld helloWorld)\n" +
    "  {\n" +
    "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public static class HelloWorld extends ScimObjectNode\n" +
    "  {\n" +
    "    public HelloWorld(String aString, Long anInteger) \n" +
    "    {\n" +
    "    setAString(aString);\n" +
    "    setAnInteger(anInteger);\n" +
    "    }\n" +
    "  /** A description */\n" +
    "  public String getAString()\n" +
    "  {\n" +
    "     return getStringAttribute(FieldNames.A_STRING).orElse(null);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public void setAString(String aString)\n" +
    "  {\n" +
    "     setAttribute(FieldNames.A_STRING, aString);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public Long getAnInteger()\n" +
    "  {\n" +
    "     return getLongAttribute(FieldNames.AN_INTEGER).orElse(null);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public void setAnInteger(Long anInteger)\n" +
    "  {\n" +
    "     setAttribute(FieldNames.AN_INTEGER, anInteger);\n" +
    "  }\n" +
    "\n" +
    "\n" +
    "  }", complexAttributeString);
    // @formatter:on
  }

  @Test
  public void testComplexRequiredAttributeBuilderTest()
  {
    SchemaAttribute aStringAttribute = SchemaAttributeBuilder.builder()
                                                             .name("aString")
                                                             .type(Type.STRING)
                                                             .required(true)
                                                             .build();
    SchemaAttribute anIntegerAttribute = SchemaAttributeBuilder.builder()
                                                               .name("anInteger")
                                                               .type(Type.INTEGER)
                                                               .required(true)
                                                               .build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("helloWorld")
                                                            .type(Type.COMPLEX)
                                                            .required(true)
                                                            .subAttributes(aStringAttribute, anIntegerAttribute)
                                                            .build();


    ComplexAttributeToInnerClassBuilder complexAttributeToInnerClassBuilder = new ComplexAttributeToInnerClassBuilder();
    String complexAttributeString = complexAttributeToInnerClassBuilder.generateComplexAttributeClass(schemaAttribute);
    // @formatter:off
    Assertions.assertEquals("    /** A description */\n" +
    "  public HelloWorld getHelloWorld()\n" +
    "  {\n" +
    "     return getObjectAttribute(FieldNames.HELLO_WORLD, HelloWorld.class).orElse(null);\n" +
    "  }\n" +
    "\n" +
    "    /** A description */\n" +
    "  public void setHelloWorld(HelloWorld helloWorld)\n" +
    "  {\n" +
    "     setAttribute(FieldNames.HELLO_WORLD, helloWorld);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public static class HelloWorld extends ScimObjectNode\n" +
    "  {\n" +
    "    public HelloWorld(String aString, Long anInteger) \n" +
    "    {\n" +
    "    setAString(aString);\n" +
    "    setAnInteger(anInteger);\n" +
    "    }\n" +
    "  /** A description */\n" +
    "  public String getAString()\n" +
    "  {\n" +
    "     return getStringAttribute(FieldNames.A_STRING).orElse(null);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public void setAString(String aString)\n" +
    "  {\n" +
    "     setAttribute(FieldNames.A_STRING, aString);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public Long getAnInteger()\n" +
    "  {\n" +
    "     return getLongAttribute(FieldNames.AN_INTEGER).orElse(null);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public void setAnInteger(Long anInteger)\n" +
    "  {\n" +
    "     setAttribute(FieldNames.AN_INTEGER, anInteger);\n" +
    "  }\n" +
    "\n" +
    "\n" +
    "  }", complexAttributeString);
    // @formatter:on
  }

  @Test
  public void testComplexMultivaluedAttributeBuilderTest()
  {
    SchemaAttribute aStringAttribute = SchemaAttributeBuilder.builder()
                                                             .name("aString")
                                                             .type(Type.STRING)
                                                             .required(true)
                                                             .build();
    SchemaAttribute anIntegerAttribute = SchemaAttributeBuilder.builder()
                                                               .name("anInteger")
                                                               .type(Type.INTEGER)
                                                               .required(true)
                                                               .build();
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("helloWorld")
                                                            .type(Type.COMPLEX)
                                                            .multivalued(true)
                                                            .subAttributes(aStringAttribute, anIntegerAttribute)
                                                            .build();


    ComplexAttributeToInnerClassBuilder complexAttributeToInnerClassBuilder = new ComplexAttributeToInnerClassBuilder();
    String complexAttributeString = complexAttributeToInnerClassBuilder.generateComplexAttributeClass(schemaAttribute);
    // @formatter:off
    Assertions.assertEquals("    /** A description */\n" +
    "  public List<HelloWorld> getHelloWorld()\n" +
    "  {\n" +
    "     return getArrayAttribute(FieldNames.HELLO_WORLD, HelloWorld.class);\n" +
    "  }\n" +
    "\n" +
    "    /** A description */\n" +
    "  public void setHelloWorld(List<HelloWorld> helloWorldList)\n" +
    "  {\n" +
    "     setAttribute(FieldNames.HELLO_WORLD, helloWorldList);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public static class HelloWorld extends ScimObjectNode\n" +
    "  {\n" +
    "    public HelloWorld(String aString, Long anInteger) \n" +
    "    {\n" +
    "    setAString(aString);\n" +
    "    setAnInteger(anInteger);\n" +
    "    }\n" +
    "  /** A description */\n" +
    "  public String getAString()\n" +
    "  {\n" +
    "     return getStringAttribute(FieldNames.A_STRING).orElse(null);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public void setAString(String aString)\n" +
    "  {\n" +
    "     setAttribute(FieldNames.A_STRING, aString);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public Long getAnInteger()\n" +
    "  {\n" +
    "     return getLongAttribute(FieldNames.AN_INTEGER).orElse(null);\n" +
    "  }\n" +
    "\n" +
    "  /** A description */\n" +
    "  public void setAnInteger(Long anInteger)\n" +
    "  {\n" +
    "     setAttribute(FieldNames.AN_INTEGER, anInteger);\n" +
    "  }\n" +
    "\n" +
    "\n" +
    "  }", complexAttributeString);
    // @formatter:on
  }
}
