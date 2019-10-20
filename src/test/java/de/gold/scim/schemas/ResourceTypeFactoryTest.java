package de.gold.scim.schemas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.endpoints.ResourceHandler;
import de.gold.scim.exceptions.DocumentValidationException;
import de.gold.scim.exceptions.InvalidResourceTypeException;
import de.gold.scim.exceptions.InvalidSchemaException;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 23:40 <br>
 * <br>
 */
public class ResourceTypeFactoryTest
{

  /**
   * a resource type factory instance for unit tests
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * just to verify if the content within the factory is stored correctly
   */
  private SchemaFactory schemaFactory;

  /**
   * the user resource type definition
   */
  private JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

  /**
   * the user resource schema
   */
  private JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

  /**
   * the user enterprise extension schema
   */
  private JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);

  /**
   * initializes the resource type factory
   */
  @BeforeEach
  public void initialize()
  {
    resourceTypeFactory = Assertions.assertDoesNotThrow(ResourceTypeFactory::getUnitTestInstance);
    schemaFactory = resourceTypeFactory.getSchemaFactory();
    userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
  }

  /**
   * this test will simply check if the {@link ResourceTypeFactory} is initialized correctly and the
   * resourceTypes are present
   */
  @Test
  public void testInitializeResourceTypeFactory()
  {
    resourceTypeFactory.registerResourceType(null, userResourceType, userResourceSchema, enterpriseUserExtension);
    ResourceType resourceType = Assertions.assertDoesNotThrow(() -> resourceTypeFactory.getResourceType("Users"));
    Assertions.assertNotNull(resourceType, "this resource type must be present!");
  }

  /**
   * this test will verify that a {@link de.gold.scim.exceptions.DocumentValidationException} is thrown if the
   * schema-validation of the resource type fails.
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.NAME, AttributeNames.RFC7643.ENDPOINT, AttributeNames.RFC7643.SCHEMA})
  public void testRegisterResourceTypeWithSchemaValidationFailure(String attributeName)
  {
    // remove the enterprise extension from the resource type document
    JsonHelper.removeAttribute(userResourceType, attributeName);
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> resourceTypeFactory.registerResourceType(null,
                                                                           userResourceType,
                                                                           userResourceSchema,
                                                                           enterpriseUserExtension));
    Assertions.assertEquals(0,
                            resourceTypeFactory.getResourceTypes().size(),
                            resourceTypeFactory.getResourceTypes().toString());
  }

  /**
   * this test will verify that a {@link de.gold.scim.exceptions.DocumentValidationException} is thrown if the
   * schema-validation of the resource definition fails
   */
  @Test
  public void testRegisterResourceTypeWithInvalidResourceSchema()
  {
    // remove the enterprise extension from the resource type document
    JsonHelper.removeAttribute(userResourceSchema, AttributeNames.RFC7643.ID);
    Assertions.assertThrows(InvalidSchemaException.class,
                            () -> resourceTypeFactory.registerResourceType(null,
                                                                           userResourceType,
                                                                           userResourceSchema,
                                                                           enterpriseUserExtension));
    Assertions.assertEquals(0,
                            resourceTypeFactory.getResourceTypes().size(),
                            resourceTypeFactory.getResourceTypes().toString());
  }

  /**
   * this test will verify that an exception is thrown if an extension was added to the registration method that
   * has not been marked in the resource type document under the attribute "schemaExtensions"
   */
  @Test
  public void testRegisterResourceTypeWithUnknownExtension()
  {
    JsonHelper.removeAttribute(userResourceType, AttributeNames.RFC7643.SCHEMA_EXTENSIONS);
    // now register and expect an exception
    Assertions.assertThrows(InvalidResourceTypeException.class,
                            () -> resourceTypeFactory.registerResourceType(null,
                                                                           userResourceType,
                                                                           userResourceSchema,
                                                                           enterpriseUserExtension));
  }

  /**
   * this test will verify that an exception is thrown if an extension was added to the registration method that
   * has not been marked in the resource type document under the attribute "schemaExtensions" but in this case
   * the array is present but declared empty
   */
  @Test
  public void testRegisterResourceTypeWithUnknownExtension2()
  {
    ArrayNode extensions = JsonHelper.getArrayAttribute(userResourceType, AttributeNames.RFC7643.SCHEMA_EXTENSIONS)
                                     .get();
    extensions.removeAll();
    // now register and expect an exception
    Assertions.assertThrows(InvalidResourceTypeException.class,
                            () -> resourceTypeFactory.registerResourceType(null,
                                                                           userResourceType,
                                                                           userResourceSchema,
                                                                           enterpriseUserExtension));
  }

  /**
   * this test will verify that the registration will not fail if the schema extensions are not added to the
   * method {@link ResourceTypeFactory#registerResourceType(ResourceHandler, JsonNode, JsonNode, JsonNode...)}
   * if they have already been registered
   */
  @Test
  public void testRegisterResourceTypeWithAlreadRegisteredExtension()
  {
    schemaFactory.registerResourceSchema(enterpriseUserExtension);
    Assertions.assertDoesNotThrow(() -> resourceTypeFactory.registerResourceType(null,
                                                                                 userResourceType,
                                                                                 userResourceSchema));
  }

  /**
   * in some cases someone might want to use the same resource for several endpoints with different extensions.
   * So if the resource is already registered and not given to the method
   * {@link ResourceTypeFactory#registerResourceType(ResourceHandler, JsonNode, JsonNode, JsonNode...)} the
   * registration must not fail
   */
  @Test
  public void testRegisterResourceTypeWithAlreadyRegisteredMainResource()
  {
    schemaFactory.registerResourceSchema(userResourceSchema);
    Assertions.assertDoesNotThrow(() -> resourceTypeFactory.registerResourceType(null,
                                                                                 userResourceType,
                                                                                 null,
                                                                                 enterpriseUserExtension));
  }

  /**
   * if the resource schema is already registered when registering a new resource type the resource schema
   * should be replaced with the new schema and a warning message should be printed
   */
  @Test
  public void testRegisterResourceTypeWithAlreadyRegisteredResource()
  {
    JsonNode oldResourceSchema = userResourceSchema.deepCopy();
    schemaFactory.registerResourceSchema(oldResourceSchema);
    JsonHelper.removeAttribute(userResourceSchema, AttributeNames.RFC7643.DESCRIPTION);
    Assertions.assertNotEquals(oldResourceSchema, userResourceSchema);

    ResourceType resourceType = Assertions.assertDoesNotThrow(() -> {
      return resourceTypeFactory.registerResourceType(null,
                                                      userResourceType,
                                                      userResourceSchema,
                                                      enterpriseUserExtension);
    });
    Schema schema = schemaFactory.getResourceSchema(resourceType.getSchema());
    Assertions.assertEquals(userResourceSchema, schema);
  }

  /**
   * this test will verify that an exception is thrown if the resource type declaration is missing the main
   * resource schema
   */
  @Test
  public void testForgotToAddResourceSchema()
  {
    Assertions.assertThrows(InvalidResourceTypeException.class,
                            () -> resourceTypeFactory.registerResourceType(null,
                                                                           userResourceType,
                                                                           null,
                                                                           enterpriseUserExtension));
  }

  /**
   * this test will verify that an exception is thrown if the extension referenced in the resource type was not
   * added to the method call and was also not registered yet
   */
  @Test
  public void testForgotToAddExtension()
  {
    Assertions.assertThrows(InvalidResourceTypeException.class,
                            () -> resourceTypeFactory.registerResourceType(null, userResourceType, userResourceType));
  }

  /**
   * this test will verify that the method
   * {@link ResourceTypeFactory#registerResourceType(ResourceHandler, String, String, String, String, String, JsonNode, JsonNode...)}
   * does also work correctly
   */
  @Test
  public void testRegisterResourceTypeAlternativeMethod()
  {
    Assertions.assertDoesNotThrow(() -> {
      resourceTypeFactory.registerResourceType(null,
                                               "User",
                                               "User",
                                               "user definition",
                                               SchemaUris.USER_URI,
                                               "/Users",
                                               userResourceSchema);
    });
  }
}
