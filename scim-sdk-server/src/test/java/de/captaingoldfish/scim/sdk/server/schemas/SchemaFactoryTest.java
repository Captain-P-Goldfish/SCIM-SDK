package de.captaingoldfish.scim.sdk.server.schemas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidSchemaException;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 17:06 <br>
 * <br>
 */
class SchemaFactoryTest implements FileReferences
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
   * this test will assure that the default meta-schemata will be read correctly from the classpath
   */
  @ParameterizedTest
  @ValueSource(strings = {"urn:ietf:params:scim:schemas:core:2.0:Schema",
                          "urn:ietf:params:scim:schemas:core:2.0:ResourceType",
                          "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"})
  public void testGetDefaultMetaSchemata(String schemaId)
  {
    Assertions.assertNotNull(schemaFactory.getMetaSchema(schemaId));
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
    Assertions.assertNotNull(schemaFactory.getResourceSchema(schemaId));
  }

  // @formatter:off
  /**
   * this test will make sure that no senseless meta declarations can be used.
   * a senseless declaration might be something like this:
   * {
   *     "name": "id",
   *     "type": "string",
   *     "required": true,
   *     "mutability": "readOnly",
   *     "returned": "never",
   * }
   * the value is required but a readOnly attribute and it is never returned from the server. This declaration
   * simply makes no sense. The client cannot write to this attribute and will never see it
   */
  // @formatter:on
  @ParameterizedTest
  @ValueSource(strings = {DUPLICATE_NAME_SCHEMA, DUPLICATE_SUB_NAME_SCHEMA, READ_ONLY_NEVER_SCHEMA,
                          WRITE_ONLY_ALWAYS_SCHEMA})
  public void testSenselessAttributeCombinationsInMetaSchemata(String badSchemaLocation)
  {
    Assertions.assertThrows(InvalidSchemaException.class,
                            () -> schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(badSchemaLocation)));
  }

  /**
   * will verify that schemas cannot be registered if the schemas-attribute is missing
   */
  @Test
  public void testRegisterSchemasWithMissingSchemasAttribute()
  {
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonHelper.removeAttribute(userResourceSchema, AttributeNames.RFC7643.SCHEMAS);
    Assertions.assertThrows(InvalidSchemaException.class,
                            () -> schemaFactory.registerResourceSchema(userResourceSchema));
  }
}
