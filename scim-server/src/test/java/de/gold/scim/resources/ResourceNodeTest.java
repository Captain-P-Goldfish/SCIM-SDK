package de.gold.scim.resources;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.resources.complex.Meta;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 00:00 <br>
 * <br>
 */
public class ResourceNodeTest
{

  /**
   * will verify that the attributes of a {@link ResourceNode} are correctly set and read
   */
  @Test
  public void testSetAndGetValues()
  {
    final String id = UUID.randomUUID().toString();
    final String externalId = UUID.randomUUID().toString();
    final Meta meta = buildMetaObject();
    TestResource testResource = new TestResource();
    testResource.setId(id);
    testResource.setExternalId(externalId);
    testResource.setMeta(meta);

    Assertions.assertEquals(id, testResource.getId().get());
    Assertions.assertEquals(externalId, testResource.getExternalId().get());
    Assertions.assertEquals(meta, testResource.getMeta().get());
  }

  /**
   * @return a simple meta object
   */
  private Meta buildMetaObject()
  {
    final LocalDateTime created = LocalDateTime.now().withNano(0);
    final LocalDateTime lastModified = LocalDateTime.now().withNano(0);
    final String resourceType = "User";
    final String location = "/Users/" + UUID.randomUUID().toString();
    final String version = "1";
    return Meta.builder()
               .created(created)
               .lastModified(lastModified)
               .resourceType(resourceType)
               .location(location)
               .version(version)
               .build();
  }

  /**
   * a test implementation of {@link ResourceNode} to have an object to test the methods in the abstract class
   */
  public static class TestResource extends ResourceNode
  {

  }
}
