package de.gold.scim.server.resources.complex;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.server.constants.AttributeNames;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 11:57 <br>
 * <br>
 */
public class FilterConfigTest
{


  /**
   * verifies that a new created instance is not empty
   */
  @Test
  public void testNewCreatedInstanceIsNotEmpty()
  {
    FilterConfig filterConfig = FilterConfig.builder().build();
    MatcherAssert.assertThat(filterConfig, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertEquals(2, filterConfig.size());
    Assertions.assertFalse(filterConfig.isSupported());
    Assertions.assertEquals(FilterConfig.DEFAULT_MAX_RESULTS, filterConfig.getMaxResults());
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    FilterConfig eTagConfig = FilterConfig.builder().build();

    eTagConfig.remove(AttributeNames.RFC7643.SUPPORTED);
    eTagConfig.remove(AttributeNames.RFC7643.MAX_RESULTS);

    Assertions.assertFalse(eTagConfig.isSupported());
    Assertions.assertEquals(FilterConfig.DEFAULT_MAX_RESULTS, eTagConfig.getMaxResults());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testSetterMethods()
  {
    final int maxResults = 50;
    FilterConfig eTagConfig = FilterConfig.builder().build();
    eTagConfig.setSupported(true);
    eTagConfig.setMaxResults(maxResults);
    Assertions.assertTrue(eTagConfig.isSupported());
    Assertions.assertEquals(maxResults, eTagConfig.getMaxResults());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testBuilderParameterSet()
  {
    final int maxResults = 50;
    FilterConfig eTagConfig = FilterConfig.builder().supported(true).maxResults(maxResults).build();
    Assertions.assertTrue(eTagConfig.isSupported());
    Assertions.assertEquals(maxResults, eTagConfig.getMaxResults());
  }
}
