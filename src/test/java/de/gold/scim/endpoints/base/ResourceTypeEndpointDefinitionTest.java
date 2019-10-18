package de.gold.scim.endpoints.base;

import static de.gold.scim.endpoints.ResourceEndpointHandlerUtil.getUnitTestResourceEndpointHandler;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.constants.ResourceTypeNames;
import de.gold.scim.endpoints.ResourceEndpointHandler;
import de.gold.scim.endpoints.handler.GroupHandlerImpl;
import de.gold.scim.endpoints.handler.UserHandlerImpl;
import de.gold.scim.resources.ServiceProvider;
import de.gold.scim.resources.ServiceProviderUrlExtension;
import de.gold.scim.response.GetResponse;
import de.gold.scim.response.ScimResponse;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.schemas.ResourceTypeFactoryUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 17.10.2019 - 23:57 <br>
 * <br>
 */
@Slf4j
public class ResourceTypeEndpointDefinitionTest
{

  /**
   * defines the base url for the meta-attribute
   */
  private static final String BASE_URL = "https://localhost/scim/v2";

  /**
   * the resource type factory in which the resources will be registered
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the endpoint definition that handles all requests
   */
  private ResourceEndpointHandler resourceEndpointHandler;

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

    ServiceProviderUrlExtension urlExtension = ServiceProviderUrlExtension.builder().baseUrl(BASE_URL).build();
    ServiceProvider serviceProvider = ServiceProvider.builder().serviceProviderUrlExtension(urlExtension).build();
    resourceEndpointHandler = getUnitTestResourceEndpointHandler(resourceTypeFactory,
                                                                 serviceProvider,
                                                                 userEndpoint,
                                                                 groupEndpoint,
                                                                 meEndpoint);
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
    ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.RESOURCE_TYPES, name);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    ResourceType resourceType = ResourceTypeFactoryUtil.getResourceType(resourceTypeFactory,
                                                                        getResponse.getExistingResource());
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
    return BASE_URL + EndpointPaths.RESOURCE_TYPES + "/" + resourceTypeName;
  }

  /**
   * verifies that the resource types can be extracted from the list resource endpoint
   */
  @Test
  public void testListResourceTypes()
  {
    Assertions.fail("implement service provider endpoint first");
  }
}
