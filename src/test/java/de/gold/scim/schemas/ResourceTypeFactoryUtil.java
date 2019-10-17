package de.gold.scim.schemas;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 22:50 <br>
 * <br>
 * a helper class for other unit tests to create instances of {@link ResourceTypeFactory} for unit tests
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceTypeFactoryUtil
{

  /**
   * @return a resource type factory that can be used by any unit test
   */
  public static ResourceTypeFactory getUnitTestResourceTypeFactory()
  {
    return ResourceTypeFactory.getUnitTestInstance();
  }

  /**
   * @return a new resource type for unit tests from the given json node
   */
  public static ResourceType getResourceType(ResourceTypeFactory resourceTypeFactory, JsonNode jsonNode)
  {
    return new ResourceType(resourceTypeFactory.getSchemaFactory(), jsonNode);
  }

}
