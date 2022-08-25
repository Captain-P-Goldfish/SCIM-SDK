package de.captaingoldfish.scim.sdk.common.resources.complex;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 12:01 <br>
 * <br>
 */
public class BulkConfigTest
{


  /**
   * verifies that a new created instance is not empty
   */
  @Test
  public void testNewCreatedInstanceIsNotEmpty()
  {
    BulkConfig bulkConfig = BulkConfig.builder().build();
    MatcherAssert.assertThat(bulkConfig, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertEquals(6, bulkConfig.size());
    Assertions.assertFalse(bulkConfig.isSupported());
    Assertions.assertEquals(BulkConfig.DEFAULT_MAX_OPERATIONS, bulkConfig.getMaxOperations());
    Assertions.assertEquals(BulkConfig.DEFAULT_MAX_PAYLOAD_SIZE, bulkConfig.getMaxPayloadSize());
    Assertions.assertFalse(bulkConfig.isReturnResourcesEnabled());
    Assertions.assertFalse(bulkConfig.isReturnResourcesByDefault());
    Assertions.assertFalse(bulkConfig.isSupportBulkGet());
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    BulkConfig bulkConfig = BulkConfig.builder().build();

    bulkConfig.remove(AttributeNames.RFC7643.SUPPORTED);
    bulkConfig.remove(AttributeNames.RFC7643.MAX_OPERATIONS);
    bulkConfig.remove(AttributeNames.RFC7643.MAX_PAYLOAD_SIZE);

    Assertions.assertFalse(bulkConfig.isSupported());
    Assertions.assertEquals(BulkConfig.DEFAULT_MAX_OPERATIONS, bulkConfig.getMaxOperations());
    Assertions.assertEquals(BulkConfig.DEFAULT_MAX_PAYLOAD_SIZE, bulkConfig.getMaxPayloadSize());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testSetterMethods()
  {
    final int maxOperations = 50;
    final long maxPayLoadSize = 100_000;
    BulkConfig bulkConfig = BulkConfig.builder().build();
    bulkConfig.setSupported(true);
    bulkConfig.setMaxOperations(maxOperations);
    bulkConfig.setMaxPayloadSize(maxPayLoadSize);
    Assertions.assertTrue(bulkConfig.isSupported());
    Assertions.assertEquals(maxOperations, bulkConfig.getMaxOperations());
    Assertions.assertEquals(maxPayLoadSize, bulkConfig.getMaxPayloadSize());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testBuilderParameterSet()
  {
    final int maxOperations = 50;
    final long maxPayLoadSize = 100_000;
    BulkConfig bulkConfig = BulkConfig.builder()
                                      .supported(true)
                                      .maxOperations(maxOperations)
                                      .maxPayloadSize(maxPayLoadSize)
                                      .build();
    Assertions.assertTrue(bulkConfig.isSupported());
    Assertions.assertEquals(maxOperations, bulkConfig.getMaxOperations());
    Assertions.assertEquals(maxPayLoadSize, bulkConfig.getMaxPayloadSize());
  }

}
