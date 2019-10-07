package de.gold.scim.schemas;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.InvalidResourceTypeException;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 22:29 <br>
 * <br>
 */
@Slf4j
public class ResourceTypeTest implements FileReferences
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
    schemaFactory = Assertions.assertDoesNotThrow(SchemaFactory::getUnitTestInstance);
    ResourceType.setSchemaFactory(schemaFactory);
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON));
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON));
  }

  /**
   * this test will simply test if a resource type object will be built successfully and if the values are set
   * correctly
   */
  @Test
  public void testCreateResourceType()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType resourceType = Assertions.assertDoesNotThrow(() -> new ResourceType(userResourceType));
    Assertions.assertEquals(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                            resourceType.getSchemas());
    Assertions.assertEquals("User", resourceType.getId());
    Assertions.assertEquals("User", resourceType.getName());
    Assertions.assertEquals("User Account", resourceType.getDescription());
    Assertions.assertEquals(SchemaUris.USER_URI, resourceType.getSchema());
    Assertions.assertEquals("/Users", resourceType.getEndpoint());

    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE);
    ResourceType.ResourceSchema resourceSchema = resourceType.getResourceSchema(chuckNorris);
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.USER_URI), resourceSchema.getMetaSchema());
    Assertions.assertEquals(1, resourceType.getSchemas().size());
    Assertions.assertEquals(schemaFactory.getMetaSchema(SchemaUris.RESOURCE_TYPE_URI),
                            schemaFactory.getMetaSchema(resourceType.getSchemas().get(0)));

    List<Schema> schemaExtensions = resourceType.getNotRequiredResourceSchemaExtensions();
    Assertions.assertEquals(1, schemaExtensions.size());
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.ENTERPRISE_USER_URI), schemaExtensions.get(0));

    Assertions.assertEquals(0, resourceType.getRequiredResourceSchemaExtensions().size());
  }

  /**
   * will test that the method {@link ResourceType#toJsonNode()} produces the same document as the document at
   * {@link ClassPathReferences#USER_RESOURCE_TYPE_JSON}
   */
  @Test
  public void testResourceTypeToJsonNode()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType resourceType = Assertions.assertDoesNotThrow(() -> new ResourceType(userResourceType));
    JsonNode resourceTypeJsonNode = resourceType.toJsonNode();
    Assertions.assertEquals(userResourceType, resourceTypeJsonNode);
  }

  /**
   * this test will simply test if a resource type object will be built successfully
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.ID, AttributeNames.NAME, AttributeNames.SCHEMA, AttributeNames.ENDPOINT})
  public void testCreateResourceTypeWithMissingAttribute(String attributeName)
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonHelper.removeAttribute(userResourceType, attributeName);
    Assertions.assertThrows(InvalidResourceTypeException.class, () -> new ResourceType(userResourceType));
  }

  /**
   * will test that the enterprise user extension is correctly parsed from the json document in the resource
   * type object
   */
  @Test
  public void testGetBuildResourceSchemaWithExtension()
  {
    ResourceType userResourceType = new ResourceType(JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    ResourceType.ResourceSchema resourceSchema = userResourceType.getResourceSchema(chuckNorris);
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.USER_URI), resourceSchema.getMetaSchema());
    Assertions.assertEquals(1, resourceSchema.getExtensions().size());
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.ENTERPRISE_USER_URI),
                            resourceSchema.getExtensions().get(0));
  }

  /**
   * will test that no extensions are read if the extension has not been used within the resource
   */
  @Test
  public void testGetBuildResourceSchemaWithoutExtension()
  {
    ResourceType userResourceType = new ResourceType(JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE);
    ResourceType.ResourceSchema resourceSchema = userResourceType.getResourceSchema(chuckNorris);
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.USER_URI), resourceSchema.getMetaSchema());
    Assertions.assertEquals(0, resourceSchema.getExtensions().size());
  }

  /**
   * if the resource of the resource type has not been registered yet an exception must be thrown
   */
  @Test
  public void testLoadResourceTypeWithUnregisteredResourceSchema()
  {
    Assertions.assertThrows(InvalidResourceTypeException.class,
                            () -> new ResourceType(JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE)));
  }

  /**
   * if the document put into the {@link ResourceType#getResourceSchema(String)} method references an unknown
   * schema an {@link InvalidResourceTypeException} is expected
   */
  @Test
  public void testGetUnregisteredSchema()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    ResourceType roleResourceType = new ResourceType(JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE));
    JsonNode adminRole = JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    ArrayNode schemas = JsonHelper.getArrayAttribute(adminRole, AttributeNames.SCHEMAS).get();
    schemas.add(new TextNode("urn:unknown:reference"));
    Assertions.assertThrows(InvalidResourceTypeException.class, () -> roleResourceType.getResourceSchema(adminRole));
  }

  /**
   * if the document put into the constructor of {@link ResourceType} has an empty 'schemas'-attribute an
   * {@link InvalidResourceTypeException} is expected
   */
  @Test
  public void testGetResourceSchemaWithEmptySchemasAttributeOnResourceType()
  {
    JsonNode roleResourceTypeNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE);
    JsonHelper.removeAttribute(roleResourceTypeNode, AttributeNames.SCHEMAS);
    Assertions.assertThrows(InvalidResourceTypeException.class, () -> new ResourceType(roleResourceTypeNode));
  }

  /**
   * if the document put into the {@link ResourceType#getResourceSchema(String)} method is missing the
   * 'schemas'-attribute an {@link InvalidResourceTypeException} is expected
   */
  @Test
  public void testGetResourceSchemaWithMissingSchemasAttribute()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    JsonNode roleResourceTypeNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE);
    ResourceType roleResourceType = new ResourceType(roleResourceTypeNode);
    JsonNode adminRole = JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    JsonHelper.removeAttribute(adminRole, AttributeNames.SCHEMAS);
    Assertions.assertThrows(BadRequestException.class, () -> roleResourceType.getResourceSchema(adminRole));
  }

  /**
   * if the document put into the {@link ResourceType#getResourceSchema(String)} method has an empty
   * 'schemas'-attribute an {@link InvalidResourceTypeException} is expected
   */
  @Test
  public void testGetResourceSchemaWithEmptySchemasAttribute()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    JsonNode roleResourceTypeNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE);
    ResourceType roleResourceType = new ResourceType(roleResourceTypeNode);
    JsonNode adminRole = JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    ArrayNode schemasNode = new ArrayNode(JsonNodeFactory.instance);
    JsonHelper.replaceNode(adminRole, AttributeNames.SCHEMAS, schemasNode);
    Assertions.assertThrows(BadRequestException.class, () -> roleResourceType.getResourceSchema(adminRole));
  }

  /**
   * if the document put into the {@link ResourceType#getResourceSchema(String)} method is missing a required
   * extension a {@link BadRequestException} is expected
   */
  @Test
  public void testMissingRequiredExtension()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    JsonNode customUserResourceType = JsonHelper.loadJsonDocument(USER_CUSTOM_RESOURCE_TYPE);
    ResourceType userResourceType = new ResourceType(customUserResourceType);
    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE);
    try
    {
      userResourceType.getResourceSchema(chuckNorris);
      Assertions.fail("the call must throw an exception!");
    }
    catch (BadRequestException ex)
    {
      Assertions.assertEquals(ScimType.MISSING_EXTENSION, ex.getScimType());
    }
  }

  /**
   * if the document put into the {@link ResourceType#getResourceSchema(String)} method is NOT missing the
   * required extensions the execution should be successful
   */
  @Test
  public void testRequiredExtensionIsPresent()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    JsonNode customUserResourceType = JsonHelper.loadJsonDocument(USER_CUSTOM_RESOURCE_TYPE);
    ResourceType userResourceType = new ResourceType(customUserResourceType);
    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE);
    ArrayNode schemas = JsonHelper.getArrayAttribute(chuckNorris, AttributeNames.SCHEMAS).get();
    final String roleUri = "urn:gold:params:scim:schemas:custom:2.0:Role";
    JsonHelper.addAttributeToArray(schemas, new TextNode(roleUri));
    JsonNode adminRole = JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    JsonHelper.addAttribute(chuckNorris, roleUri, adminRole);
    Assertions.assertDoesNotThrow(() -> userResourceType.getResourceSchema(chuckNorris));
  }

  /**
   * this test shall verify that the {@link SchemaValidator} works correctly if used with a {@link ResourceType}
   * object
   */
  @Test
  public void testSchemaValidationWithResourceTypeWithExtensionForResponse()
  {
    JsonNode userResourceTypeJson = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType resourceType = new ResourceType(userResourceTypeJson);

    JsonNode enterpriseUserDocument = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    JsonNode validatedDocument = SchemaValidator.validateDocumentForResponse(resourceType, enterpriseUserDocument);

    SchemaValidatorTest.validateJsonNodeIsScimNode(validatedDocument);
    Assertions.assertTrue(JsonHelper.getObjectAttribute(validatedDocument,
                                                        resourceType.getSchemaExtensions().get(0).getSchema())
                                    .isPresent());
  }

  /**
   * this test shall verify that the {@link SchemaValidator} works correctly if used with a {@link ResourceType}
   * object
   */
  @Test
  public void testSchemaValidationWithResourceTypeWithExtensionForRequest()
  {
    JsonNode userResourceTypeJson = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType resourceType = new ResourceType(userResourceTypeJson);

    JsonNode enterpriseUserDocument = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    JsonNode validatedDocument = SchemaValidator.validateDocumentForRequest(resourceType,
                                                                            enterpriseUserDocument,
                                                                            SchemaValidator.HttpMethod.POST);

    SchemaValidatorTest.validateJsonNodeIsScimNode(validatedDocument);
    Assertions.assertTrue(JsonHelper.getObjectAttribute(validatedDocument,
                                                        resourceType.getSchemaExtensions().get(0).getSchema())
                                    .isPresent());
  }
}
