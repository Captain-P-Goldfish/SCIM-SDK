package de.gold.scim.resources.multicomplex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:56 <br>
 * <br>
 */
public class ScimX509CertificateTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    ScimX509Certificate instance = Assertions.assertDoesNotThrow(() -> ScimX509Certificate.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new ScimX509Certificate().isEmpty());
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
    ScimX509Certificate x509Certificate = ScimX509Certificate.builder()
                                                             .value(value)
                                                             .display(display)
                                                             .primary(primary)
                                                             .type(type)
                                                             .ref(ref)
                                                             .build();
    Assertions.assertEquals(value, x509Certificate.getValue().get());
    Assertions.assertEquals(display, x509Certificate.getDisplay().get());
    Assertions.assertEquals(primary, x509Certificate.isPrimary());
    Assertions.assertEquals(type, x509Certificate.getScimType().get());
    Assertions.assertEquals(ref, x509Certificate.getRef().get());
  }
}
