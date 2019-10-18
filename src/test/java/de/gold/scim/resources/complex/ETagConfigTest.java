package de.gold.scim.resources.complex;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.AttributeNames;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 11:59 <br>
 * <br>
 */
public class ETagConfigTest
{


  /**
   * verifies that a new created instance is not empty
   */
  @Test
  public void testNewCreatedInstanceIsNotEmpty()
  {
    ETagConfig eTagConfig = ETagConfig.builder().build();
    MatcherAssert.assertThat(eTagConfig, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertEquals(1, eTagConfig.size());
    Assertions.assertFalse(eTagConfig.isSupported());
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    ETagConfig eTagConfig = ETagConfig.builder().build();

    eTagConfig.remove(AttributeNames.SUPPORTED);

    Assertions.assertFalse(eTagConfig.isSupported());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testSetterMethods()
  {
    ETagConfig eTagConfig = ETagConfig.builder().build();
    eTagConfig.setSupported(true);
    Assertions.assertTrue(eTagConfig.isSupported());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testBuilderParameterSet()
  {
    ETagConfig eTagConfig = ETagConfig.builder().supported(true).build();
    Assertions.assertTrue(eTagConfig.isSupported());
  }
}
