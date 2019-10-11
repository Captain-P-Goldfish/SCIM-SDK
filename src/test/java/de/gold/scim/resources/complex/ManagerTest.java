package de.gold.scim.resources.complex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:19 <br>
 * <br>
 */
public class ManagerTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    Manager instance = Assertions.assertDoesNotThrow(() -> Manager.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new Manager().isEmpty());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributes()
  {
    final String value = UUID.randomUUID().toString();
    final String displayName = "Chuck Norris";
    final String ref = "1";
    Manager manager = Manager.builder().value(value).displayName(displayName).ref(ref).build();
    Assertions.assertEquals(value, manager.getValue().get());
    Assertions.assertEquals(displayName, manager.getDisplayName().get());
    Assertions.assertEquals(ref, manager.getRef().get());
  }
}
