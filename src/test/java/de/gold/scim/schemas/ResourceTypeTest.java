package de.gold.scim.schemas;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.exceptions.InvalidResourceTypeException;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 22:29 <br>
 * <br>
 */
public class ResourceTypeTest
{

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

    Assertions.assertEquals(SchemaFactory.getResourceSchema(SchemaUris.USER_URI), resourceType.getResourceSchema());
    List<Schema> metaSchemata = resourceType.getMetaSchemata();
    Assertions.assertEquals(1, metaSchemata.size());
    Assertions.assertEquals(SchemaFactory.getResourceSchema(SchemaUris.RESOURCE_TYPE_URI), metaSchemata.get(0));

    List<Schema> schemaExtensions = resourceType.getNotRequiredResourceSchemaExtensions();
    Assertions.assertEquals(1, schemaExtensions.size());
    Assertions.assertEquals(SchemaFactory.getResourceSchema(SchemaUris.ENTERPRISE_USER_URI), schemaExtensions.get(0));

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
}
