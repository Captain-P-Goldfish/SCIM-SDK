package de.captaingoldfish.scim.sdk.common.resources.multicomplex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:55 <br>
 * <br>
 */
public class PersonRoleTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    PersonRole instance = Assertions.assertDoesNotThrow(() -> PersonRole.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new PersonRole().isEmpty());
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
    PersonRole personRole = PersonRole.builder()
                                      .value(value)
                                      .display(display)
                                      .primary(primary)
                                      .type(type)
                                      .ref(ref)
                                      .build();
    Assertions.assertEquals(value, personRole.getValue().get());
    Assertions.assertEquals(display, personRole.getDisplay().get());
    Assertions.assertEquals(primary, personRole.isPrimary());
    Assertions.assertEquals(type, personRole.getType().get());
    Assertions.assertEquals(ref, personRole.getRef().get());
  }
}
