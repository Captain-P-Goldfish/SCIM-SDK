package de.gold.scim.endpoints.handler;

import static de.gold.scim.endpoints.ResourceEndpointHandlerUtil.getUnitTestResourceEndpointHandler;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.constants.ResourceTypeNames;
import de.gold.scim.endpoints.base.GroupEndpointDefinition;
import de.gold.scim.endpoints.base.MeEndpointDefinition;
import de.gold.scim.endpoints.base.UserEndpointDefinition;
import de.gold.scim.exceptions.NotImplementedException;
import de.gold.scim.exceptions.ResourceNotFoundException;
import de.gold.scim.response.PartialListResponse;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.schemas.ResourceTypeFactoryUtil;
import de.gold.scim.utils.JsonHelper;
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
   * the resource type handler implementation that was registered
   */
  private ResourceTypeHandler resourceTypeHandler;

  /**
   * initializes the resource endpoint implementation
   */
  @BeforeEach
  public void initialize()
  {
    resourceTypeFactory = ResourceTypeFactoryUtil.getUnitTestResourceTypeFactory();
    UserEndpointDefinition userEndpoint = new UserEndpointDefinition(new UserHandlerImpl());
    GroupEndpointDefinition groupEndpoint = new GroupEndpointDefinition(new GroupHandlerImpl());
    MeEndpointDefinition meEndpoint = new MeEndpointDefinition(new UserHandlerImpl());

    // this line is simply used to register the endpoints on the resourceTypeFactory
    getUnitTestResourceEndpointHandler(resourceTypeFactory, userEndpoint, groupEndpoint, meEndpoint);
    this.resourceTypeHandler = (ResourceTypeHandler)resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES)
                                                                       .getResourceHandlerImpl();
  }

  /**
   * verifies that all resource types can be extracted from the ResourceTypes endpoint
   *
   * @param name the name of the resource type
   */
  @ParameterizedTest
  @ValueSource(strings = {ResourceTypeNames.RESOURCE_TYPE, ResourceTypeNames.USER, ResourceTypeNames.GROUPS,
                          ResourceTypeNames.ME, ResourceTypeNames.SERVICE_PROVIDER_CONFIG})
  public void testGetResourceTypeByName(String name)
  {
    ResourceType resourceType = resourceTypeHandler.getResource(name);
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
    PartialListResponse<ResourceType> listResponse = resourceTypeHandler.listResources(1, 10, null, null, null);
    Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getResources().size());
  }

  /**
   * verifies that the listResources method will never return more entries than stated in count with count has a
   * value that enforces less than count entries in the last request
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testListResourceTypesWithStartIndexAndCount(int count)
  {
    for ( int startIndex = 0 ; startIndex < resourceTypeFactory.getAllResourceTypes().size() ; startIndex += count )
    {
      PartialListResponse<ResourceType> listResponse = resourceTypeHandler.listResources(startIndex + 1,
                                                                                         count,
                                                                                         null,
                                                                                         null,
                                                                                         null);
      MatcherAssert.assertThat(listResponse.getResources().size(), Matchers.lessThanOrEqualTo(count));
      Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getTotalResults());
      log.debug("returned entries: {}", listResponse.getResources().size());
    }
  }

  /**
   * tries to get a resource with an id that does not exist
   */
  @Test
  public void testGetResourceWithInvalidId()
  {
    Assertions.assertThrows(ResourceNotFoundException.class,
                            () -> resourceTypeHandler.getResource("nonExistingResource"));
  }

  /**
   * tries to create a resource on the endpoint
   */
  @Test
  public void testCreateResource()
  {
    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType userResourceType = ResourceTypeFactoryUtil.getResourceType(resourceTypeFactory, userResourceTypeNode);
    Assertions.assertThrows(NotImplementedException.class, () -> resourceTypeHandler.createResource(userResourceType));
  }

  /**
   * tries to update a resource on the endpoint
   */
  @Test
  public void testUpdateResource()
  {
    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    ResourceType userResourceType = ResourceTypeFactoryUtil.getResourceType(resourceTypeFactory, userResourceTypeNode);
    Assertions.assertThrows(NotImplementedException.class, () -> resourceTypeHandler.updateResource(userResourceType));
  }

  /**
   * tries to delete a resource on the endpoint
   */
  @Test
  public void testDeleteResource()
  {
    Assertions.assertThrows(NotImplementedException.class, () -> resourceTypeHandler.deleteResource("blubb"));
  }
}
