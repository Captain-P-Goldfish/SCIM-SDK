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

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidResourceTypeException;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import de.captaingoldfish.scim.sdk.server.schemas.validation.RequestResourceValidator;
import de.captaingoldfish.scim.sdk.server.schemas.validation.ResponseResourceValidator;
import de.captaingoldfish.scim.sdk.server.schemas.validation.SchemaValidatorTest;
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
   * a basic service provider configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * initializes the schema factory instance for unit tests
   */
  @BeforeEach
  public void initialize()
  {
    serviceProvider = new ServiceProvider();
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
   * this test shall verify that the {@link SchemaValidator} works correctly if used with a {@link ResourceType}
   * object
   */
  @Test
  public void testSchemaValidationWithResourceTypeWithExtensionForResponse()
  {
    JsonNode userResourceTypeJson = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType resourceType = new ResourceType(schemaFactory, userResourceTypeJson);
    resourceType.setResourceHandlerImpl(new UserHandlerImpl(false));

    JsonNode enterpriseUserDocument = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    TestHelper.addMetaToDocument(enterpriseUserDocument);
    JsonNode validatedDocument = new ResponseResourceValidator(serviceProvider, resourceType, null, null, null,
                                                               (s, s2) -> "http://localhost")
                                                                                             .validateDocument(enterpriseUserDocument);

    SchemaValidatorTest.validateJsonNodeIsScimNode(null, validatedDocument);
    Assertions.assertTrue(JsonHelper.getObjectAttribute(validatedDocument,
                                                        resourceType.getSchemaExtensions().get(0).getSchema())
                                    .isPresent());
  }

  /**
   * this test will simply test if a resource type object will be built successfully and whether the values are
   * set correctly or not
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
    DocumentDescription documentDescription = new DocumentDescription(resourceType, chuckNorris);
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.USER_URI), documentDescription.getMetaSchema());
    Assertions.assertEquals(1, resourceType.getSchemas().size());
    Assertions.assertEquals(schemaFactory.getMetaSchema(SchemaUris.RESOURCE_TYPE_URI),
                            schemaFactory.getMetaSchema(resourceType.getSchemas().iterator().next()));

    List<Schema> schemaExtensions = resourceType.getNotRequiredResourceSchemaExtensions();
    Assertions.assertEquals(1, schemaExtensions.size());
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.ENTERPRISE_USER_URI), schemaExtensions.get(0));

    Assertions.assertEquals(0, resourceType.getRequiredResourceSchemaExtensions().size());
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
    resourceType.setResourceHandlerImpl(new UserHandlerImpl(false));

    JsonNode enterpriseUserDocument = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    JsonNode validatedDocument = new RequestResourceValidator(serviceProvider, resourceType,
                                                              HttpMethod.POST).validateDocument(enterpriseUserDocument);

    SchemaValidatorTest.validateJsonNodeIsScimNode(null, validatedDocument);
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
    JsonNode resourceTypeFilterExt = JsonHelper.loadJsonDocument(ClassPathReferences.RESOURCE_TYPES_FEATURE_EXT_JSON);
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

  /**
   * verifies that the endpoint control-feature is correctly get and set from the resource type
   */
  @Test
  public void testSetEndpointControlFeature()
  {
    ResourceType resourceType = new ResourceType(schemaFactory,
                                                 JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
    Assertions.assertNotNull(resourceType.getFeatures());

    EndpointControlFeature endpointControlFeature = resourceType.getFeatures().getEndpointControlFeature();
    Assertions.assertNotNull(endpointControlFeature);
    Assertions.assertFalse(endpointControlFeature.isCreateDisabled());
    Assertions.assertFalse(endpointControlFeature.isGetDisabled());
    Assertions.assertFalse(endpointControlFeature.isListDisabled());
    Assertions.assertFalse(endpointControlFeature.isUpdateDisabled());
    Assertions.assertFalse(endpointControlFeature.isDeleteDisabled());
    Assertions.assertFalse(endpointControlFeature.isResourceTypeDisabled());

    endpointControlFeature.setCreateDisabled(true);
    endpointControlFeature.setGetDisabled(true);
    endpointControlFeature.setListDisabled(true);
    endpointControlFeature.setUpdateDisabled(true);
    endpointControlFeature.setDeleteDisabled(true);

    Assertions.assertTrue(resourceType.getFeatures().getEndpointControlFeature().isCreateDisabled());
    Assertions.assertTrue(resourceType.getFeatures().getEndpointControlFeature().isGetDisabled());
    Assertions.assertTrue(resourceType.getFeatures().getEndpointControlFeature().isListDisabled());
    Assertions.assertTrue(resourceType.getFeatures().getEndpointControlFeature().isUpdateDisabled());
    Assertions.assertTrue(resourceType.getFeatures().getEndpointControlFeature().isDeleteDisabled());
    Assertions.assertTrue(resourceType.getFeatures().getEndpointControlFeature().isResourceTypeDisabled());
    Assertions.assertTrue(resourceType.getFeatures().isResourceTypeDisabled());

    endpointControlFeature.setDeleteDisabled(false);
    Assertions.assertFalse(resourceType.getFeatures().getEndpointControlFeature().isResourceTypeDisabled());
    Assertions.assertFalse(resourceType.getFeatures().isResourceTypeDisabled());
  }

  /**
   * verifies that a resource type can be disabled by setting the specified attribute
   */
  @Test
  public void testDisableResourceType()
  {
    ResourceType resourceType = new ResourceType(schemaFactory,
                                                 JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
    Assertions.assertNotNull(resourceType.getFeatures());

    resourceType.setDisabled(true);
    Assertions.assertTrue(resourceType.isDisabled());
    Assertions.assertTrue(resourceType.getFeatures().isResourceTypeDisabled());
    Assertions.assertFalse(resourceType.getFeatures().getEndpointControlFeature().isResourceTypeDisabled());

    resourceType.setDisabled(false);
    Assertions.assertFalse(resourceType.isDisabled());
    Assertions.assertFalse(resourceType.getFeatures().isResourceTypeDisabled());
    Assertions.assertFalse(resourceType.getFeatures().getEndpointControlFeature().isResourceTypeDisabled());
  }
}
