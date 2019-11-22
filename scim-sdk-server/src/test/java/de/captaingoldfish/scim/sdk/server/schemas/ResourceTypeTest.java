package de.captaingoldfish.scim.sdk.server.schemas;

import java.util.ArrayList;
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

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidResourceTypeException;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
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
    ResourceTypeFactory resourceTypeFactory = new ResourceTypeFactory();
    schemaFactory = Assertions.assertDoesNotThrow(resourceTypeFactory::getSchemaFactory);
    resourceTypeFactory.registerResourceType(null,
                                             JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON),
                                             JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON),
                                             JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON));
    resourceTypeFactory.registerResourceType(null,
                                             JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_RESOURCE_TYPE_JSON),
                                             JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON));
  }

  /**
   * this test will simply test if a resource type object will be built successfully and if the values are set
   * correctly
   */
  @Test
  public void testCreateResourceType()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType resourceType = Assertions.assertDoesNotThrow(() -> new ResourceType(schemaFactory, userResourceType));
    Assertions.assertEquals(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                            new ArrayList<>(resourceType.getSchemas()));
    Assertions.assertEquals("User", resourceType.getId().get());
    Assertions.assertEquals("User", resourceType.getName());
    Assertions.assertEquals("User Account", resourceType.getDescription().get());
    Assertions.assertEquals(SchemaUris.USER_URI, resourceType.getSchema());
    Assertions.assertEquals("/Users", resourceType.getEndpoint());

    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE);
    ResourceType.ResourceSchema resourceSchema = resourceType.getResourceSchema(chuckNorris);
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.USER_URI), resourceSchema.getMetaSchema());
    Assertions.assertEquals(1, resourceType.getSchemas().size());
    Assertions.assertEquals(schemaFactory.getMetaSchema(SchemaUris.RESOURCE_TYPE_URI),
                            schemaFactory.getMetaSchema(resourceType.getSchemas().iterator().next()));

    List<Schema> schemaExtensions = resourceType.getNotRequiredResourceSchemaExtensions();
    Assertions.assertEquals(1, schemaExtensions.size());
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.ENTERPRISE_USER_URI), schemaExtensions.get(0));

    Assertions.assertEquals(0, resourceType.getRequiredResourceSchemaExtensions().size());
  }

  /**
   * this test will simply test if a resource type object will be built successfully
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.ID, AttributeNames.RFC7643.NAME, AttributeNames.RFC7643.SCHEMA,
                          AttributeNames.RFC7643.ENDPOINT})
  public void testCreateResourceTypeWithMissingAttribute(String attributeName)
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonHelper.removeAttribute(userResourceType, attributeName);
    Assertions.assertThrows(InvalidResourceTypeException.class,
                            () -> new ResourceType(schemaFactory, userResourceType));
  }

  /**
   * will test that the enterprise user extension is correctly parsed from the json document in the resource
   * type object
   */
  @Test
  public void testGetBuildResourceSchemaWithExtension()
  {
    ResourceType userResourceType = new ResourceType(schemaFactory,
                                                     JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
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
    ResourceType userResourceType = new ResourceType(schemaFactory,
                                                     JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE);
    ResourceType.ResourceSchema resourceSchema = userResourceType.getResourceSchema(chuckNorris);
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.USER_URI), resourceSchema.getMetaSchema());
    Assertions.assertEquals(0, resourceSchema.getExtensions().size());
  }

  /**
   * if the document put into the {@link ResourceType#getResourceSchema(JsonNode)} method references an unknown
   * schema an {@link InvalidResourceTypeException} is expected
   */
  @Test
  public void testGetUnregisteredSchema()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    ResourceType roleResourceType = new ResourceType(schemaFactory, JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE));
    JsonNode adminRole = JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    ArrayNode schemas = JsonHelper.getArrayAttribute(adminRole, AttributeNames.RFC7643.SCHEMAS).get();
    schemas.add(new TextNode("urn:unknown:reference"));
    Assertions.assertThrows(InvalidResourceTypeException.class, () -> roleResourceType.getResourceSchema(adminRole));
  }

  /**
   * if the document put into the constructor of {@link ResourceType} has an empty 'schemas'-attribute the
   * default value of {@link SchemaUris#RESOURCE_TYPE_URI} is used
   */
  @Test
  public void testSetDefaultSchemasAttribute()
  {
    JsonNode roleResourceTypeNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE);
    JsonHelper.removeAttribute(roleResourceTypeNode, AttributeNames.RFC7643.SCHEMAS);
    ResourceType resourceType = Assertions.assertDoesNotThrow(() -> new ResourceType(schemaFactory,

                                                                                     roleResourceTypeNode));
    Assertions.assertEquals(1, resourceType.getSchemas().size());
    Assertions.assertEquals(SchemaUris.RESOURCE_TYPE_URI, resourceType.getSchemas().iterator().next());
  }

  /**
   * if the document put into the {@link ResourceType#getResourceSchema(JsonNode)} method is missing the
   * 'schemas'-attribute an {@link InvalidResourceTypeException} is expected
   */
  @Test
  public void testGetResourceSchemaWithMissingSchemasAttribute()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    JsonNode roleResourceTypeNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE);
    ResourceType roleResourceType = new ResourceType(schemaFactory, roleResourceTypeNode);
    JsonNode adminRole = JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    JsonHelper.removeAttribute(adminRole, AttributeNames.RFC7643.SCHEMAS);
    Assertions.assertThrows(BadRequestException.class, () -> roleResourceType.getResourceSchema(adminRole));
  }

  /**
   * if the document put into the {@link ResourceType#getResourceSchema(JsonNode)} method has an empty
   * 'schemas'-attribute an {@link InvalidResourceTypeException} is expected
   */
  @Test
  public void testGetResourceSchemaWithEmptySchemasAttribute()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    JsonNode roleResourceTypeNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE);
    ResourceType roleResourceType = new ResourceType(schemaFactory, roleResourceTypeNode);
    JsonNode adminRole = JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    ArrayNode schemasNode = new ArrayNode(JsonNodeFactory.instance);
    JsonHelper.replaceNode(adminRole, AttributeNames.RFC7643.SCHEMAS, schemasNode);
    Assertions.assertThrows(BadRequestException.class, () -> roleResourceType.getResourceSchema(adminRole));
  }

  /**
   * if the document put into the {@link ResourceType#getResourceSchema(JsonNode)} method is NOT missing the
   * required extensions the execution should be successful
   */
  @Test
  public void testRequiredExtensionIsPresent()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    JsonNode customUserResourceType = JsonHelper.loadJsonDocument(USER_CUSTOM_RESOURCE_TYPE);
    ResourceType userResourceType = new ResourceType(schemaFactory, customUserResourceType);
    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE);
    ArrayNode schemas = JsonHelper.getArrayAttribute(chuckNorris, AttributeNames.RFC7643.SCHEMAS).get();
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
    ResourceType resourceType = new ResourceType(schemaFactory, userResourceTypeJson);

    JsonNode enterpriseUserDocument = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    TestHelper.addMetaToDocument(enterpriseUserDocument);
    JsonNode validatedDocument = SchemaValidator.validateDocumentForResponse(schemaFactory.getResourceTypeFactory(),
                                                                             resourceType,
                                                                             enterpriseUserDocument,
                                                                             null,
                                                                             null,
                                                                             null);

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
    ResourceType resourceType = new ResourceType(schemaFactory, userResourceTypeJson);

    JsonNode enterpriseUserDocument = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    JsonNode validatedDocument = SchemaValidator.validateDocumentForRequest(resourceType,
                                                                            enterpriseUserDocument,
                                                                            HttpMethod.POST);

    SchemaValidatorTest.validateJsonNodeIsScimNode(validatedDocument);
    Assertions.assertTrue(JsonHelper.getObjectAttribute(validatedDocument,
                                                        resourceType.getSchemaExtensions().get(0).getSchema())
                                    .isPresent());
  }

  /**
   * will simply test setting, getting and changing the attribute in the {@link ResourceTypeFeatures} of a
   * {@link ResourceType}
   */
  @Test
  public void testGetAndFilterFilterExtension()
  {
    JsonNode resourceTypeResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.RESOURCE_TYPE_RESOURCE_TYPE_JSON);
    JsonNode resourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    JsonNode resourceTypeFilterExt = JsonHelper.loadJsonDocument(ClassPathReferences.RESOURCE_TYPES_FILTER_EXT_JSON);
    schemaFactory.registerResourceSchema(resourceTypeSchema);
    schemaFactory.registerResourceSchema(resourceTypeFilterExt);
    ResourceType resourceType = new ResourceType(schemaFactory, resourceTypeResourceType);
    Assertions.assertNotNull(resourceType.getFeatures());
    resourceType.setFeatures(ResourceTypeFeatures.builder()
                                                 .autoFiltering(true)
                                                 .autoSorting(true)
                                                 .singletonEndpoint(true)
                                                 .build());
    Assertions.assertNotNull(resourceType.getFeatures());
    Assertions.assertTrue(resourceType.getFeatures().isAutoFiltering());
    Assertions.assertTrue(resourceType.getFeatures().isAutoSorting());
    Assertions.assertTrue(resourceType.getFeatures().isSingletonEndpoint());
    resourceType.getFeatures().setAutoFiltering(false);
    resourceType.getFeatures().setAutoSorting(false);
    resourceType.getFeatures().setSingletonEndpoint(false);
    Assertions.assertFalse(resourceType.getFeatures().isAutoFiltering());
    Assertions.assertFalse(resourceType.getFeatures().isAutoSorting());
    Assertions.assertFalse(resourceType.getFeatures().isSingletonEndpoint());
    resourceType.setFeatures(null);
    Assertions.assertNotNull(resourceType.getFeatures());
    Assertions.assertFalse(resourceType.getFeatures().isAutoFiltering());
    Assertions.assertFalse(resourceType.getFeatures().isAutoSorting());
    Assertions.assertFalse(resourceType.getFeatures().isSingletonEndpoint());
  }

}
