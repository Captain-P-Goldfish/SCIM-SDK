package de.gold.scim.server.schemas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.common.constants.AttributeNames;
import de.gold.scim.common.constants.ClassPathReferences;
import de.gold.scim.common.constants.EndpointPaths;
import de.gold.scim.common.constants.ResourceTypeNames;
import de.gold.scim.common.exceptions.InvalidSchemaException;
import de.gold.scim.common.resources.complex.Meta;
import de.gold.scim.common.schemas.Schema;
import de.gold.scim.common.utils.JsonHelper;
import de.gold.scim.common.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 18:02 <br>
 * <br>
 */
@Slf4j
class SchemaTest
{

  /**
   * a unit test schema factory instance
   */
  private SchemaFactory schemaFactory;

  /**
   * initializes the schema factory instance for unit tests
   */
  @BeforeEach
  public void initialize()
  {
    schemaFactory = Assertions.assertDoesNotThrow(() -> new ResourceTypeFactory().getSchemaFactory());
  }

  /**
   * this test will assure that the default schemata will be read correctly from the classpath
   */
  @ParameterizedTest
  @ValueSource(strings = {"urn:ietf:params:scim:schemas:core:2.0:Schema",
                          "urn:ietf:params:scim:schemas:core:2.0:ResourceType",
                          "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"})
  public void testGetDefaultMetaSchemata(String schemaId)
  {
    Schema resourceSchema = schemaFactory.getMetaSchema(schemaId);
    Assertions.assertNotNull(resourceSchema);
    String json = resourceSchema.toString();
    Assertions.assertDoesNotThrow(() -> JsonHelper.readJsonDocument(json));
  }

  /**
   * this test will assure that the default schemata will be read correctly from the classpath
   */
  @ParameterizedTest
  @ValueSource(strings = {"urn:ietf:params:scim:schemas:core:2.0:User", "urn:ietf:params:scim:schemas:core:2.0:Group"})
  public void testGetDefaultResourceSchemata(String schemaId)
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON));
    Schema resourceSchema = schemaFactory.getResourceSchema(schemaId);
    Assertions.assertNotNull(resourceSchema);
    String json = resourceSchema.toString();
    Assertions.assertDoesNotThrow(() -> JsonHelper.readJsonDocument(json));
  }

  /**
   * verifies that an exception is thrown if the given schema does not have one of the given attributes
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.ID, AttributeNames.RFC7643.ATTRIBUTES})
  public void testParseSchemaWithMissingAttribute(String attributeName)
  {
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonHelper.removeAttribute(userResourceSchema, attributeName);
    Assertions.assertThrows(InvalidSchemaException.class,
                            () -> schemaFactory.registerResourceSchema(userResourceSchema));
  }

  /**
   * verifies that an exception is thrown if the id-attribute is missing
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.ID, AttributeNames.RFC7643.ATTRIBUTES})
  public void testIdAttributeIsMissing(String attributeName)
  {
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonHelper.removeAttribute(userResourceSchema, attributeName);
    Assertions.assertThrows(InvalidSchemaException.class, () -> new Schema(userResourceSchema));
  }

  /**
   * this test will verify that if a meta attribute was added to a json schema definition that the values from
   * the file are preserved and used in the representation
   */
  @Test
  public void testMetaAttributeIsPreservedFromJsonFile()
  {
    JsonNode serviceProviderSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_SERVICE_PROVIDER_JSON);
    Schema schema = new Schema(serviceProviderSchema, null);
    Meta meta = schema.getMeta().get();
    Assertions.assertEquals(ResourceTypeNames.SCHEMA, meta.getResourceType().get());
    Assertions.assertEquals(TimeUtils.parseDateTime("2019-10-18T14:51:11+02:00"), meta.getCreated().get());
    Assertions.assertEquals(TimeUtils.parseDateTime("2019-10-18T14:51:11+02:00"), meta.getLastModified().get());
    Assertions.assertEquals(EndpointPaths.SCHEMAS + "/" + ResourceTypeNames.SERVICE_PROVIDER_CONFIG,
                            meta.getLocation().get());
    Assertions.assertFalse(meta.getVersion().isPresent());
  }

  /**
   * this test will verify that if a meta attribute will be created for a new {@link Schema} instance if no meta
   * attribute is present within the json
   */
  @Test
  public void testMetaAttributeCreatedIfMissingInJsonFile()
  {
    JsonNode serviceProviderSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_SERVICE_PROVIDER_JSON);
    JsonHelper.removeAttribute(serviceProviderSchema, AttributeNames.RFC7643.META);
    Schema schema = new Schema(serviceProviderSchema, null);
    Meta meta = schema.getMeta().get();
    Assertions.assertEquals(ResourceTypeNames.SCHEMA, meta.getResourceType().get());
    Assertions.assertTrue(meta.getCreated().isPresent());
    Assertions.assertTrue(meta.getLastModified().isPresent());
    Assertions.assertFalse(meta.getLocation().isPresent());
    Assertions.assertFalse(meta.getVersion().isPresent());
  }
}
