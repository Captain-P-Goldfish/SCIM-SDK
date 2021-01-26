package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpointHandlerUtil;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactoryUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 19.10.2019 - 13:17 <br>
 * <br>
 */
@Slf4j
public class ResourceTypeHandlerTest
{

  /**
   * the resource type factory in which the resources will be registered
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource type for the resource types endpoint
   */
  private ResourceType resourceTypeResourceType;

  /**
   * the resource type handler implementation that was registered
   */
  private ResourceTypeHandler resourceTypeHandler;

  /**
   * initializes the resource endpoint implementation
   */
  @BeforeEach
  public void initialize()
  {
    resourceTypeFactory = new ResourceTypeFactory();
    ResourceEndpointHandlerUtil.registerAllEndpoints(resourceTypeFactory);
    this.resourceTypeResourceType = resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES);
    this.resourceTypeHandler = (ResourceTypeHandler)resourceTypeResourceType.getResourceHandlerImpl();
  }

  /**
   * verifies that all resource types can be extracted from the ResourceTypes endpoint
   *
   * @param name the name of the resource type
   */
  @ParameterizedTest
  @ValueSource(strings = {ResourceTypeNames.RESOURCE_TYPE, ResourceTypeNames.USER, ResourceTypeNames.GROUPS,
                          ResourceTypeNames.ME, ResourceTypeNames.SERVICE_PROVIDER_CONFIG, ResourceTypeNames.SCHEMA})
  public void testGetResourceTypeByName(String name)
  {
    ResourceType resourceType = resourceTypeHandler.getResource(name, null, null, null);
    Assertions.assertEquals(name, resourceType.getName());
    log.debug(resourceType.toPrettyString());
    Assertions.assertTrue(resourceType.getMeta().isPresent());
    Assertions.assertTrue(resourceType.getMeta().get().getLocation().isPresent());
    Assertions.assertTrue(resourceType.getMeta().get().getResourceType().isPresent());
    Assertions.assertTrue(resourceType.getMeta().get().getCreated().isPresent());
    Assertions.assertTrue(resourceType.getMeta().get().getLastModified().isPresent());
    Assertions.assertEquals(ResourceTypeNames.RESOURCE_TYPE, resourceType.getMeta().get().getResourceType().get());
    Assertions.assertEquals(getLocationUrl(name), resourceType.getMeta().get().getLocation().get());
  }

  /**
   * builds the expected location url of the resource types for the meta-attribute
   *
   * @param resourceTypeName the name of the requested resource
   * @return the fully qualified location url as it should be in the returned resource
   */
  private String getLocationUrl(String resourceTypeName)
  {
    return EndpointPaths.RESOURCE_TYPES + "/" + resourceTypeName;
  }

  /**
   * verifies that the resource types can be extracted from the list resource endpoint
   */
  @Test
  public void testListResourceTypes()
  {
    PartialListResponse<ResourceType> listResponse = resourceTypeHandler.listResources(1,
                                                                                       10,
                                                                                       null,
                                                                                       null,
                                                                                       null,
                                                                                       null,
                                                                                       null,
                                                                                       null);
    Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getResources().size());
  }

  /**
   * tries to get a resource with an id that does not exist
   */
  @Test
  public void testGetResourceWithInvalidId()
  {
    Assertions.assertThrows(ResourceNotFoundException.class,
                            () -> resourceTypeHandler.getResource("nonExistingResource", null, null, null));
  }

  /**
   * tries to create a resource on the endpoint
   */
  @Test
  public void testCreateResource()
  {
    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType userResourceType = ResourceTypeFactoryUtil.getResourceType(resourceTypeFactory, userResourceTypeNode);
    Assertions.assertThrows(NotImplementedException.class,
                            () -> resourceTypeHandler.createResource(userResourceType, null));
  }

  /**
   * tries to update a resource on the endpoint
   */
  @Test
  public void testUpdateResource()
  {
    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType userResourceType = ResourceTypeFactoryUtil.getResourceType(resourceTypeFactory, userResourceTypeNode);
    Assertions.assertThrows(NotImplementedException.class,
                            () -> resourceTypeHandler.updateResource(userResourceType, null));
  }

  /**
   * tries to delete a resource on the endpoint
   */
  @Test
  public void testDeleteResource()
  {
    Assertions.assertThrows(NotImplementedException.class, () -> resourceTypeHandler.deleteResource("blubb", null));
  }

  /**
   * verifies that no exception is thrown if no resource types are defined
   */
  @Test
  public void testNoResourceTypesDoExist()
  {
    ResourceTypeFactoryUtil.clearAllResourceTypes(resourceTypeFactory);
    Assertions.assertDoesNotThrow(() -> resourceTypeHandler.listResources(1, 0, null, null, null, null, null, null));
  }
}
