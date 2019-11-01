package de.gold.scim.common.resources.multicomplex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:52 <br>
 * <br>
 */
public class EntitlementTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    Entitlement instance = Assertions.assertDoesNotThrow(() -> Entitlement.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new Entitlement().isEmpty());
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
    Entitlement entitlement = Entitlement.builder()
                                         .value(value)
                                         .display(display)
                                         .primary(primary)
                                         .type(type)
                                         .ref(ref)
                                         .build();
    Assertions.assertEquals(value, entitlement.getValue().get());
    Assertions.assertEquals(display, entitlement.getDisplay().get());
    Assertions.assertEquals(primary, entitlement.isPrimary());
    Assertions.assertEquals(type, entitlement.getType().get());
    Assertions.assertEquals(ref, entitlement.getRef().get());
  }
}
