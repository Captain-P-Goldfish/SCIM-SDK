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
public class SortConfigTest
{

  /**
   * verifies that a new created instance is not empty
   */
  @Test
  public void testNewCreatedInstanceIsNotEmpty()
  {
    SortConfig sortConfig = SortConfig.builder().build();
    MatcherAssert.assertThat(sortConfig, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertEquals(1, sortConfig.size());
    Assertions.assertFalse(sortConfig.isSupported());
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    SortConfig sortConfig = SortConfig.builder().build();

    sortConfig.remove(AttributeNames.SUPPORTED);

    Assertions.assertFalse(sortConfig.isSupported());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testSetterMethods()
  {
    SortConfig sortConfig = SortConfig.builder().build();
    sortConfig.setSupported(true);
    Assertions.assertTrue(sortConfig.isSupported());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testBuilderParameterSet()
  {
    SortConfig sortConfig = SortConfig.builder().supported(true).build();
    Assertions.assertTrue(sortConfig.isSupported());
  }
}
