package de.gold.scim.resources.complex;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.AttributeNames;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 12:01 <br>
 * <br>
 */
public class ChangePasswordConfigTest
{


  /**
   * verifies that a new created instance is not empty
   */
  @Test
  public void testNewCreatedInstanceIsNotEmpty()
  {
    ChangePasswordConfig changePasswordConfig = ChangePasswordConfig.builder().build();
    MatcherAssert.assertThat(changePasswordConfig, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertEquals(1, changePasswordConfig.size());
    Assertions.assertFalse(changePasswordConfig.isSupported());
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    ChangePasswordConfig changePasswordConfig = ChangePasswordConfig.builder().build();

    changePasswordConfig.remove(AttributeNames.SUPPORTED);

    Assertions.assertFalse(changePasswordConfig.isSupported());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testSetterMethods()
  {
    ChangePasswordConfig changePasswordConfig = ChangePasswordConfig.builder().build();
    changePasswordConfig.setSupported(true);
    Assertions.assertTrue(changePasswordConfig.isSupported());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testBuilderParameterSet()
  {
    ChangePasswordConfig changePasswordConfig = ChangePasswordConfig.builder().supported(true).build();
    Assertions.assertTrue(changePasswordConfig.isSupported());
  }
}
