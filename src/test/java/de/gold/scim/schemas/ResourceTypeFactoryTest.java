package de.gold.scim.schemas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.utils.JsonHelper;


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

    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    resourceTypeFactory.registerResourceType(null, userResourceType, userResourceSchema, enterpriseUserExtension);
  }

  /**
   * this test will simply check if the {@link ResourceTypeFactory} is initialized correctly and the
   * resourceTypes are present
   */
  @Test
  public void testInitializeResourceTypeFactory()
  {
    ResourceType resourceType = Assertions.assertDoesNotThrow(() -> resourceTypeFactory.getResourceType("Users"));
    Assertions.assertNotNull(resourceType, "this resource type must be present!");
  }
}
