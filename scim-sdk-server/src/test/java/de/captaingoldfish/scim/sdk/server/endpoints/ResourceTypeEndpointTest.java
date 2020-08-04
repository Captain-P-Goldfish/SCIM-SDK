package de.captaingoldfish.scim.sdk.server.endpoints;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.response.GetResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactoryUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 04.08.2020
 */
@Slf4j
public class ResourceTypeEndpointTest extends AbstractEndpointTest
{

  @ParameterizedTest
  @ValueSource(strings = {ResourceTypeNames.RESOURCE_TYPE, ResourceTypeNames.USER, ResourceTypeNames.GROUPS,
                          ResourceTypeNames.SERVICE_PROVIDER_CONFIG, ResourceTypeNames.SCHEMA})
  public void getResourceTypesByName(String name)
  {
    final String url = getUrl(EndpointPaths.RESOURCE_TYPES + "/" + name);
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
    Assertions.assertEquals(HttpStatus.OK, scimResponse.getHttpStatus(), scimResponse.toPrettyString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    log.debug(scimResponse.toPrettyString());

    ResourceType resourceType = ResourceTypeFactoryUtil.getResourceType(resourceEndpoint.getResourceTypeFactory(),
                                                                        scimResponse);
    Assertions.assertEquals(name, resourceType.getName());
    Assertions.assertTrue(resourceType.getMeta().isPresent());
    Assertions.assertTrue(resourceType.getMeta().get().getLocation().isPresent());
    Assertions.assertTrue(resourceType.getMeta().get().getResourceType().isPresent());
    Assertions.assertTrue(resourceType.getMeta().get().getCreated().isPresent());
    Assertions.assertTrue(resourceType.getMeta().get().getLastModified().isPresent());
    Assertions.assertEquals(ResourceTypeNames.RESOURCE_TYPE, resourceType.getMeta().get().getResourceType().get());
    Assertions.assertEquals(url, resourceType.getMeta().get().getLocation().get());
  }
}
