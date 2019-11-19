package de.gold.scim.server.schemas;

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
   * @return a new resource type for unit tests from the given json node
   */
  public static ResourceType getResourceType(ResourceTypeFactory resourceTypeFactory, JsonNode jsonNode)
  {
    return new ResourceType(resourceTypeFactory.getSchemaFactory(), jsonNode);
  }

  /**
   * removes all existing resource types from the given factory
   */
  public static void clearAllResourceTypes(ResourceTypeFactory resourceTypeFactory)
  {
    resourceTypeFactory.getResourceTypes().clear();
  }

  /**
   * retrieves the {@link SchemaFactory} for unit tests
   */
  public static SchemaFactory getSchemaFactory(ResourceTypeFactory resourceTypeFactory)
  {
    return resourceTypeFactory.getSchemaFactory();
  }

}
