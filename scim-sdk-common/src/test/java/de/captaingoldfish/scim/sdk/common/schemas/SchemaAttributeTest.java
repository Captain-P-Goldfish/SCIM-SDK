package de.captaingoldfish.scim.sdk.common.schemas;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.utils.FileReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.common.utils.SchemaAttributeBuilder;


/**
 * @author Pascal Knueppel
 * @since 11.11.2023
 */
public class SchemaAttributeTest implements FileReferences
{

  /**
   * verifies that boolean values are correctly assigned
   */
  @DisplayName("Illegal boolean-default-value is ignored")
  @ParameterizedTest
  @ValueSource(strings = {"true", "TRUE", "false", "FALSE"})
  public void testValidBooleanValuesAreAccepted(String booleanString)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.BOOLEAN)
                                                            .build();
    schemaAttribute.setDefaultValue(booleanString);
    Assertions.assertEquals(booleanString, schemaAttribute.getDefaultValue());
    Assertions.assertEquals(Boolean.parseBoolean(booleanString), StringUtils.equalsIgnoreCase(booleanString, "true"));
  }

  /**
   * verifies that illegal boolean values are ignored
   */
  @DisplayName("Illegal boolean-default-value is ignored")
  @Test
  public void testIllegalBooleanDefaultValueIsIgnored()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.BOOLEAN)
                                                            .build();
    schemaAttribute.setDefaultValue("this is not a boolean");
    Assertions.assertNull(schemaAttribute.getDefaultValue());
  }

  /**
   * verifies that illegal decimal values are ignored
   */
  @DisplayName("Valid integer-default-value is correctly assigned")
  @Test
  public void testIntegerDefaultValueIsAssigned()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.INTEGER)
                                                            .build();
    schemaAttribute.setDefaultValue("55");
    Assertions.assertEquals("55", schemaAttribute.getDefaultValue());
  }

  /**
   * verifies that illegal integer values are ignored
   */
  @DisplayName("Illegal integer-default-value is ignored")
  @Test
  public void testIllegalIntegerDefaultValueIsIgnored()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.INTEGER)
                                                            .build();
    schemaAttribute.setDefaultValue("55.5");
    Assertions.assertNull(schemaAttribute.getDefaultValue());
  }

  /**
   * verifies that illegal decimal values are ignored
   */
  @DisplayName("Valid decimal-default-value is correctly assigned")
  @Test
  public void testDecimalDefaultValueIsAssigned()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.DECIMAL)
                                                            .build();
    schemaAttribute.setDefaultValue("55.5");
    Assertions.assertEquals("55.5", schemaAttribute.getDefaultValue());
  }

  /**
   * verifies that illegal decimal values are ignored
   */
  @DisplayName("Illegal decimal-default-value is ignored")
  @Test
  public void testIllegalDecimalDefaultValueIsIgnored()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.DECIMAL)
                                                            .build();
    schemaAttribute.setDefaultValue("x55.5");
    Assertions.assertNull(schemaAttribute.getDefaultValue());
  }

  /**
   * verifies that default values are ignored on complex types
   */
  @DisplayName("Illegal default-value on complex-type is ignored")
  @Test
  public void testIllegalComplexDefaultValueIsIgnored()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.COMPLEX)
                                                            .build();
    schemaAttribute.setDefaultValue("x55.5");
    Assertions.assertNull(schemaAttribute.getDefaultValue());
  }

  /**
   * verifies that default values are ignored on binary types
   */
  @DisplayName("Illegal default-value on binary-type is ignored")
  @Test
  public void testIllegalBinaryDefaultValueIsIgnored()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.BINARY)
                                                            .build();
    schemaAttribute.setDefaultValue("x55.5");
    Assertions.assertNull(schemaAttribute.getDefaultValue());
  }

  /**
   * verifies that default values are ignored on any types
   */
  @DisplayName("Illegal default-value on any-type is ignored")
  @Test
  public void testIllegalAnyDefaultValueIsIgnored()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name(AttributeNames.RFC7643.TYPE)
                                                            .type(Type.ANY)
                                                            .build();
    schemaAttribute.setDefaultValue("x55.5");
    Assertions.assertNull(schemaAttribute.getDefaultValue());
  }

  /**
   * makes sure that the method {@link SchemaAttribute#getSubAttribute(String)} works as expected
   */
  @DisplayName("getSubAttribute works as expected")
  @Test
  public void testGetSubAttribute()
  {
    Schema schema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    SchemaAttribute nameAttribute = schema.getSchemaAttribute(AttributeNames.RFC7643.NAME);
    SchemaAttribute givenNameAttribute = schema.getSchemaAttribute(String.format("%s.%s",
                                                                                 AttributeNames.RFC7643.NAME,
                                                                                 AttributeNames.RFC7643.GIVEN_NAME));

    // this attribute does not exist in name
    Assertions.assertNull(nameAttribute.getSubAttribute(AttributeNames.RFC7643.VALUE));
    Assertions.assertEquals(givenNameAttribute, nameAttribute.getSubAttribute(AttributeNames.RFC7643.GIVEN_NAME));
    Assertions.assertEquals(givenNameAttribute,
                            nameAttribute.getSubAttribute(givenNameAttribute.getFullResourceName()));

  }
}
