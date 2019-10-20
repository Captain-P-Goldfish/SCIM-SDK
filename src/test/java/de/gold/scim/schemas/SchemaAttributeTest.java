package de.gold.scim.schemas;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.enums.Mutability;
import de.gold.scim.constants.enums.ReferenceTypes;
import de.gold.scim.constants.enums.Returned;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.constants.enums.Uniqueness;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 14:18 <br>
 * <br>
 */
class SchemaAttributeTest
{

  /**
   * verifies that the data of a multivalued complex type is correctly read
   */
  @Test
  public void testReadSchemaWithComplexAttributes()
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(metaSchema));
    List<SchemaAttribute> attributes = schema.getAttributes();
    SchemaAttribute schemaExtensions = attributes.stream()
                                                 .filter(attr -> attr.getName().equals("schemaExtensions"))
                                                 .findAny()
                                                 .get();
    Assertions.assertNotNull(schema.getSchemaAttribute(schemaExtensions.getScimNodeName()));

    Assertions.assertEquals("schemaExtensions", schemaExtensions.getName());
    Assertions.assertEquals(Type.COMPLEX, schemaExtensions.getType());
    Assertions.assertTrue(schemaExtensions.isMultiValued());
    Assertions.assertEquals("A list of URIs of the resource type's schema extensions.",
                            schemaExtensions.getDescription());
    Assertions.assertFalse(schemaExtensions.isRequired());
    Assertions.assertFalse(schemaExtensions.isCaseExact());
    Assertions.assertEquals(Mutability.READ_ONLY, schemaExtensions.getMutability());
    Assertions.assertEquals(Returned.DEFAULT, schemaExtensions.getReturned());
    Assertions.assertEquals(Uniqueness.NONE, schemaExtensions.getUniqueness());
    Assertions.assertEquals(Collections.emptyList(), schemaExtensions.getCanonicalValues());
    Assertions.assertEquals(Collections.emptyList(), schemaExtensions.getReferenceTypes());
    Assertions.assertFalse(schemaExtensions.getSubAttributes().isEmpty());
  }

  /**
   * verifies that referenceTypes are correctly read from the json schema
   */
  @Test
  public void testReadSchemaAttributeWithReferenceTypes()
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(metaSchema));
    List<SchemaAttribute> attributes = schema.getAttributes();
    SchemaAttribute schemaAttribute = attributes.stream()
                                                .filter(attr -> attr.getName().equals("schema"))
                                                .findAny()
                                                .get();
    Assertions.assertEquals("schema", schemaAttribute.getName());
    Assertions.assertEquals(Type.REFERENCE, schemaAttribute.getType());
    Assertions.assertFalse(schemaAttribute.isMultiValued());
    Assertions.assertEquals("The resource type's primary/base schema URI.", schemaAttribute.getDescription());
    Assertions.assertTrue(schemaAttribute.isRequired());
    Assertions.assertTrue(schemaAttribute.isCaseExact());
    Assertions.assertEquals(Mutability.READ_ONLY, schemaAttribute.getMutability());
    Assertions.assertEquals(Returned.DEFAULT, schemaAttribute.getReturned());
    Assertions.assertEquals(Uniqueness.NONE, schemaAttribute.getUniqueness());
    Assertions.assertTrue(schemaAttribute.getSubAttributes().isEmpty());
    Assertions.assertTrue(schemaAttribute.getCanonicalValues().isEmpty());
    Assertions.assertEquals(Collections.singletonList(ReferenceTypes.URI), schemaAttribute.getReferenceTypes());
  }

  /**
   * verifies that referenceTypes are correctly read from the json schema
   */
  @Test
  public void testReadSchemaAttributeWithCanonicalValues()
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON);
    Schema schema = Assertions.assertDoesNotThrow(() -> new Schema(metaSchema));
    SchemaAttribute attributes = schema.getAttributes()
                                       .stream()
                                       .filter(attr -> attr.getName().equals("attributes"))
                                       .findAny()
                                       .get();
    SchemaAttribute type = attributes.getSubAttributes()
                                     .stream()
                                     .filter(attr -> attr.getName().equals("type"))
                                     .findAny()
                                     .get();
    Assertions.assertEquals("type", type.getName());
    Assertions.assertEquals(Type.STRING, type.getType());
    Assertions.assertFalse(type.isMultiValued());
    Assertions.assertEquals("The attribute's data type. Valid values include 'string',"
                            + " 'complex', 'boolean', 'decimal', 'integer', 'dateTime', 'reference'.",
                            type.getDescription());
    Assertions.assertTrue(type.isRequired());
    Assertions.assertTrue(type.isCaseExact());
    Assertions.assertEquals(Mutability.READ_ONLY, type.getMutability());
    Assertions.assertEquals(Returned.DEFAULT, type.getReturned());
    Assertions.assertEquals(Uniqueness.NONE, type.getUniqueness());
    Assertions.assertTrue(type.getSubAttributes().isEmpty());
    Assertions.assertTrue(type.getReferenceTypes().isEmpty());
    Assertions.assertEquals(Arrays.asList(Type.STRING.getValue(),
                                          Type.COMPLEX.getValue(),
                                          Type.BOOLEAN.getValue(),
                                          Type.DECIMAL.getValue(),
                                          Type.INTEGER.getValue(),
                                          Type.DATE_TIME.getValue(),
                                          Type.REFERENCE.getValue()),
                            type.getCanonicalValues());
  }

}
