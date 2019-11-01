package de.gold.scim.server.resources.multicomplex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:44 <br>
 * <br>
 */
public class EmailTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    Email instance = Assertions.assertDoesNotThrow(() -> Email.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new Email().isEmpty());
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
    Email email = Email.builder().value(value).display(display).primary(primary).type(type).ref(ref).build();
    Assertions.assertEquals(value, email.getValue().get());
    Assertions.assertEquals(display, email.getDisplay().get());
    Assertions.assertEquals(primary, email.isPrimary());
    Assertions.assertEquals(type, email.getType().get());
    Assertions.assertEquals(ref, email.getRef().get());
  }
}
