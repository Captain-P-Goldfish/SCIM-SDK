package de.gold.scim.server.resources.multicomplex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:54 <br>
 * <br>
 */
public class PhoneNumberTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    PhoneNumber instance = Assertions.assertDoesNotThrow(() -> PhoneNumber.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new PhoneNumber().isEmpty());
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
    PhoneNumber phoneNumber = PhoneNumber.builder()
                                         .value(value)
                                         .display(display)
                                         .primary(primary)
                                         .type(type)
                                         .ref(ref)
                                         .build();
    Assertions.assertEquals(value, phoneNumber.getValue().get());
    Assertions.assertEquals(display, phoneNumber.getDisplay().get());
    Assertions.assertEquals(primary, phoneNumber.isPrimary());
    Assertions.assertEquals(type, phoneNumber.getType().get());
    Assertions.assertEquals(ref, phoneNumber.getRef().get());
  }
}
