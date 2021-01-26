package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpointHandlerUtil;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
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
    Schema schema = schemaHandler.getResource(name, null, null, null);
    Assertions.assertEquals(name, schema.getId().get());
  }

  /**
   * verifies that the schemas can be extracted from the list resource endpoint
   */
  @Test
  public void testListResourceTypes()
  {
    PartialListResponse<Schema> listResponse = schemaHandler.listResources(1,
                                                                           Integer.MAX_VALUE,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null);
    Assertions.assertEquals(allSchemas.size(), listResponse.getResources().size());
  }

  /**
   * tries to get a resource with an id that does not exist
   */
  @Test
  public void testGetResourceWithInvalidId()
  {
    Assertions.assertThrows(ResourceNotFoundException.class,
                            () -> schemaHandler.getResource("nonExistingResource", null, null, null));
  }

  /**
   * tries to create a resource on the endpoint
   */
  @Test
  public void testCreateResource()
  {
    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    Schema schema = new Schema(userResourceTypeNode);
    Assertions.assertThrows(NotImplementedException.class, () -> schemaHandler.createResource(schema, null));
  }

  /**
   * tries to update a resource on the endpoint
   */
  @Test
  public void testUpdateResource()
  {
    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    Schema schema = new Schema(userResourceTypeNode);
    Assertions.assertThrows(NotImplementedException.class, () -> schemaHandler.updateResource(schema, null));
  }

  /**
   * tries to delete a resource on the endpoint
   */
  @Test
  public void testDeleteResource()
  {
    Assertions.assertThrows(NotImplementedException.class, () -> schemaHandler.deleteResource("blubb", null));
  }
}
