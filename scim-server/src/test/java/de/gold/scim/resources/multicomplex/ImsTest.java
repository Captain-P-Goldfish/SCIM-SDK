package de.gold.scim.resources.multicomplex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:53 <br>
 * <br>
 */
public class ImsTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    Ims instance = Assertions.assertDoesNotThrow(() -> Ims.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new Ims().isEmpty());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributes()
  {
    final String value = UUID.randomUUID().toString();
    final String display = UUID.randomUUID().toString();
    final boolean primary = true;
    final String type = UUID.randomUUID().toString();
    final String ref = UUID.randomUUID().toString();
    Ims ims = Ims.builder().value(value).display(display).primary(primary).type(type).ref(ref).build();
    Assertions.assertEquals(value, ims.getValue().get());
    Assertions.assertEquals(display, ims.getDisplay().get());
    Assertions.assertEquals(primary, ims.isPrimary());
    Assertions.assertEquals(type, ims.getType().get());
    Assertions.assertEquals(ref, ims.getRef().get());
  }
}
