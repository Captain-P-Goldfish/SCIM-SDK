package de.captaingoldfish.scim.sdk.server.schemas;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;


/**
 * @author Pascal Knueppel
 * @since 18.04.2021
 */
public class DocumentDescriptionTest implements FileReferences
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
   * will test that the enterprise user extension is correctly added to the document description
   */
  @Test
  public void testBuildDocumentDescriptionWithExtension()
  {
    ResourceType userResourceType = new ResourceType(schemaFactory,
                                                     JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    DocumentDescription documentDescription = new DocumentDescription(userResourceType, chuckNorris);
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.USER_URI), documentDescription.getMetaSchema());
    Assertions.assertEquals(1, documentDescription.getExtensions().size());
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.ENTERPRISE_USER_URI),
                            documentDescription.getExtensions().get(0));
  }

  /**
   * will test that no extensions are added to the document description if extension is not present within the
   * document
   */
  @Test
  public void testBuildDocumentDescriptionWithoutExtension()
  {
    ResourceType userResourceType = new ResourceType(schemaFactory,
                                                     JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
    JsonNode chuckNorris = JsonHelper.loadJsonDocument(USER_RESOURCE);
    DocumentDescription documentDescription = new DocumentDescription(userResourceType, chuckNorris);
    Assertions.assertEquals(schemaFactory.getResourceSchema(SchemaUris.USER_URI), documentDescription.getMetaSchema());
    Assertions.assertEquals(0, documentDescription.getExtensions().size());
  }

  /**
   * verifies that unknown schema references are ignored and that the correct resource references are put into
   * the schemas-attribute and the document description
   */
  @Test
  public void testParseWithUnknownSchemaReference()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    ResourceType roleResourceType = new ResourceType(schemaFactory, JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE));
    JsonNode adminRole = JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    ArrayNode schemas = JsonHelper.getArrayAttribute(adminRole, AttributeNames.RFC7643.SCHEMAS).get();
    schemas.add(new TextNode("urn:unknown:reference"));
    DocumentDescription documentDescription = Assertions.assertDoesNotThrow(() -> new DocumentDescription(roleResourceType,
                                                                                                          adminRole));
    String roleUri = "urn:gold:params:scim:schemas:custom:2.0:Role";
    Assertions.assertEquals(roleUri, documentDescription.getMetaSchema().getNonNullId());
    List<String> schemasList = JsonHelper.getSimpleAttributeArray(adminRole, AttributeNames.RFC7643.SCHEMAS).get();
    Assertions.assertEquals(1, schemasList.size());
    MatcherAssert.assertThat(schemasList, Matchers.hasItem(roleUri));
  }

  /**
   * If the request document is missing the 'schemas'-attribute the implementation expects the resource to be
   * the correct one and adds the main schema manually to the resource
   */
  @Test
  public void testGetDocumentDescriptionWithMissingSchemasAttribute()
  {
    schemaFactory.registerResourceSchema(JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA));
    JsonNode roleResourceTypeNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE);
    ResourceType roleResourceType = new ResourceType(schemaFactory, roleResourceTypeNode);
    JsonNode adminRole = JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    JsonHelper.removeAttribute(adminRole, AttributeNames.RFC7643.SCHEMAS);
    Assertions.assertDoesNotThrow(() -> new DocumentDescription(roleResourceType, adminRole));
    Assertions.assertEquals(1, adminRole.get(AttributeNames.RFC7643.SCHEMAS).size());
    Assertions.assertEquals(roleResourceType.getMainSchema().getNonNullId(),
                            adminRole.get(AttributeNames.RFC7643.SCHEMAS).get(0).textValue());
  }

  /**
   * If the request document is missing the 'schemas'-attribute the implementation expects the resource to be
   * the correct one and adds the main schema manually to the resource
   */
  @Test
  public void testGetDocumentDescriptionWithExtensionsAndMissingSchemasAttribute()
  {
    ResourceType userResourceType = new ResourceType(schemaFactory,
                                                     JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
    JsonNode userResource = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    JsonHelper.removeAttribute(userResource, AttributeNames.RFC7643.SCHEMAS);
    Assertions.assertDoesNotThrow(() -> new DocumentDescription(userResourceType, userResource));
    Assertions.assertEquals(2, userResource.get(AttributeNames.RFC7643.SCHEMAS).size());
    Assertions.assertEquals(userResourceType.getMainSchema().getNonNullId(),
                            userResource.get(AttributeNames.RFC7643.SCHEMAS).get(0).textValue());
    Assertions.assertEquals(userResourceType.getSchemaExtensions().get(0).getSchema(),
                            userResource.get(AttributeNames.RFC7643.SCHEMAS).get(1).textValue());
  }

  /**
   * If the request document does have the 'schemas'-attribute but with as an empty array the implementation
   * expects the resource still to be the correct one and adds the main schema manually to the resource
   */
  @Test
  public void testGetDocumentDescriptionWithEmptySchemasAttribute()
  {
    ResourceType userResourceType = new ResourceType(schemaFactory,
                                                     JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON));
    JsonNode userResource = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    ArrayNode schemasNode = new ArrayNode(JsonNodeFactory.instance);
    JsonHelper.replaceNode(userResource, AttributeNames.RFC7643.SCHEMAS, schemasNode);
    Assertions.assertDoesNotThrow(() -> new DocumentDescription(userResourceType, userResource));
    Assertions.assertEquals(2, userResource.get(AttributeNames.RFC7643.SCHEMAS).size());
    Assertions.assertEquals(userResourceType.getMainSchema().getNonNullId(),
                            userResource.get(AttributeNames.RFC7643.SCHEMAS).get(0).textValue());
    Assertions.assertEquals(userResourceType.getSchemaExtensions().get(0).getSchema(),
                            userResource.get(AttributeNames.RFC7643.SCHEMAS).get(1).textValue());
  }

  /**
   * validates that the document description will parse the document successfully even if a required extension
   * is missing. The validation of required extensions is part of the schema validation not part of document
   * parsing
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
    Assertions.assertDoesNotThrow(() -> new DocumentDescription(userResourceType, chuckNorris));
  }
}
