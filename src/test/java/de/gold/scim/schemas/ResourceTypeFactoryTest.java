package de.gold.scim.schemas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.SchemaUris;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 23:40 <br>
 * <br>
 */
public class ResourceTypeFactoryTest
{

  /**
   * a resource type factory instance for unit tests
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * initializes the resource type factory
   */
  @BeforeEach
  public void initialize()
  {
    resourceTypeFactory = Assertions.assertDoesNotThrow(ResourceTypeFactory::getUnitTestInstance);
  }

  /**
   * this test will simply check if the {@link ResourceTypeFactory} is initialized correctly and the
   * resourceTypes are present
   */
  @Test
  public void testInitializeResourceTypeFactory()
  {
    ResourceType resourceType = Assertions.assertDoesNotThrow(() -> resourceTypeFactory.getResourceType(SchemaUris.USER_URI));
    Assertions.assertNotNull(resourceType);
  }
}
