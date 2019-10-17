package de.gold.scim.endpoints.base;

import static de.gold.scim.endpoints.ResourceEndpointHandlerUtil.getUnitTestResourceEndpointHandler;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.gold.scim.endpoints.ResourceEndpointHandler;
import de.gold.scim.endpoints.handler.GroupHandlerImpl;
import de.gold.scim.endpoints.handler.UserHandlerImpl;
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
   * the endpoint path for resource types
   */
  private static final String ENDPOINT = "ResourceTypes";

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
    ResourceTypeEndpointDefinition resourceTypeEndpoint = new ResourceTypeEndpointDefinition(resourceTypeFactory);
    resourceEndpointHandler = getUnitTestResourceEndpointHandler(resourceTypeFactory,
                                                                 userEndpoint,
                                                                 groupEndpoint,
                                                                 meEndpoint,
                                                                 resourceTypeEndpoint);
  }

  /**
   * verifies that all resource types can be extracted from the ResourceTypes endpoint
   *
   * @param name the name of the resource type
   */
  @ParameterizedTest
  @ValueSource(strings = {"ResourceType", "User", "Group", "Me"})
  public void testGetResourceTypeByName(String name)
  {
    ScimResponse scimResponse = resourceEndpointHandler.getResource(ENDPOINT, name);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    ResourceType resourceType = ResourceTypeFactoryUtil.getResourceType(resourceTypeFactory,
                                                                        getResponse.getExistingResource());
    Assertions.assertEquals(name, resourceType.getName());
    log.debug(resourceType.toPrettyString());
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
