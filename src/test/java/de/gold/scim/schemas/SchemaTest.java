package de.gold.scim.schemas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.exceptions.InvalidSchemaException;
import de.gold.scim.utils.JsonHelper;
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
    schemaFactory = Assertions.assertDoesNotThrow(() -> ResourceTypeFactory.getUnitTestInstance().getSchemaFactory());
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
  @ValueSource(strings = {AttributeNames.ID, AttributeNames.ATTRIBUTES})
  public void testParseSchemaWithMissingAttribute(String attributeName)
  {
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonHelper.removeAttribute(userResourceSchema, attributeName);
    Assertions.assertThrows(InvalidSchemaException.class,
                            () -> schemaFactory.registerResourceSchema(userResourceSchema));
  }
}
