package de.gold.scim.server.endpoints.handler;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.server.constants.ClassPathReferences;
import de.gold.scim.server.constants.EndpointPaths;
import de.gold.scim.server.constants.SchemaUris;
import de.gold.scim.server.endpoints.ResourceEndpointHandlerUtil;
import de.gold.scim.server.exceptions.NotImplementedException;
import de.gold.scim.server.exceptions.ResourceNotFoundException;
import de.gold.scim.server.response.PartialListResponse;
import de.gold.scim.server.schemas.ResourceType;
import de.gold.scim.server.schemas.ResourceTypeFactory;
import de.gold.scim.server.schemas.Schema;
import de.gold.scim.server.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 20.10.2019 - 17:28 <br>
 * <br>
 */
@Slf4j
public class SchemaHandlerTest
{

  /**
   * the resource type factory in which the resources will be registered
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the schema handler implementation that was registered
   */
  private SchemaHandler schemaHandler;

  /**
   * this list of all resource type schemas
   */
  private List<Schema> allSchemas;

  /**
   * initializes the resource endpoint implementation
   */
  @BeforeEach
  public void initialize()
  {
    resourceTypeFactory = new ResourceTypeFactory();
    ResourceEndpointHandlerUtil.registerAllEndpoints(resourceTypeFactory);
    this.schemaHandler = (SchemaHandler)resourceTypeFactory.getResourceType(EndpointPaths.SCHEMAS)
                                                           .getResourceHandlerImpl();
    allSchemas = resourceTypeFactory.getAllResourceTypes()
                                    .stream()
                                    .map(ResourceType::getAllSchemas)
                                    .flatMap(Collection::stream)
                                    .distinct()
                                    .collect(Collectors.toList());
  }

  /**
   * verifies that all resource types can be extracted from the Schemas endpoint
   *
   * @param name the uri of the schema
   */
  @ParameterizedTest
  @ValueSource(strings = {SchemaUris.SCHEMA_URI, SchemaUris.SERVICE_PROVIDER_CONFIG_URI, SchemaUris.RESOURCE_TYPE_URI,
                          SchemaUris.USER_URI, SchemaUris.ENTERPRISE_USER_URI, SchemaUris.GROUP_URI})
  public void testGetResourceTypeByName(String name)
  {
    Schema schema = schemaHandler.getResource(name);
    Assertions.assertEquals(name, schema.getId().get());
  }

  /**
   * verifies that the schemas can be extracted from the list resource endpoint
   */
  @Test
  public void testListResourceTypes()
  {
    PartialListResponse<Schema> listResponse = schemaHandler.listResources(1, Integer.MAX_VALUE, null, null, null);
    Assertions.assertEquals(allSchemas.size(), listResponse.getResources().size());
  }

  /**
   * tries to get a resource with an id that does not exist
   */
  @Test
  public void testGetResourceWithInvalidId()
  {
    Assertions.assertThrows(ResourceNotFoundException.class, () -> schemaHandler.getResource("nonExistingResource"));
  }

  /**
   * tries to create a resource on the endpoint
   */
  @Test
  public void testCreateResource()
  {
    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    Schema schema = new Schema(userResourceTypeNode);
    Assertions.assertThrows(NotImplementedException.class, () -> schemaHandler.createResource(schema));
  }

  /**
   * tries to update a resource on the endpoint
   */
  @Test
  public void testUpdateResource()
  {
    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    Schema schema = new Schema(userResourceTypeNode);
    Assertions.assertThrows(NotImplementedException.class, () -> schemaHandler.updateResource(schema));
  }

  /**
   * tries to delete a resource on the endpoint
   */
  @Test
  public void testDeleteResource()
  {
    Assertions.assertThrows(NotImplementedException.class, () -> schemaHandler.deleteResource("blubb"));
  }
}
