package de.gold.scim.resources.complex;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.AttributeNames;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 12:00 <br>
 * <br>
 */
public class PatchConfigTest
{


  /**
   * verifies that a new created instance is not empty
   */
  @Test
  public void testNewCreatedInstanceIsNotEmpty()
  {
    PatchConfig patchConfig = PatchConfig.builder().build();
    MatcherAssert.assertThat(patchConfig, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertEquals(1, patchConfig.size());
    Assertions.assertFalse(patchConfig.isSupported());
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    PatchConfig patchConfig = PatchConfig.builder().build();

    patchConfig.remove(AttributeNames.RFC7643.SUPPORTED);

    Assertions.assertFalse(patchConfig.isSupported());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testSetterMethods()
  {
    PatchConfig patchConfig = PatchConfig.builder().build();
    patchConfig.setSupported(true);
    Assertions.assertTrue(patchConfig.isSupported());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testBuilderParameterSet()
  {
    PatchConfig patchConfig = PatchConfig.builder().supported(true).build();
    Assertions.assertTrue(patchConfig.isSupported());
  }
}
