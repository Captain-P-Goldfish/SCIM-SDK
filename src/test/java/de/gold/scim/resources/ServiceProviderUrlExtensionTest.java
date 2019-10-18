package de.gold.scim.resources;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.exceptions.InvalidConfigException;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 13:37 <br>
 * <br>
 */
public class ServiceProviderUrlExtensionTest
{

  private static final String BASE_URL = "https://localhost/scim/v2";

  /**
   * verifies that a new created instance is not empty
   */
  @Test
  public void testNewCreatedInstance()
  {
    Assertions.assertThrows(InvalidConfigException.class, () -> ServiceProviderUrlExtension.builder().build());
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    ServiceProviderUrlExtension urlExtension = ServiceProviderUrlExtension.builder().baseUrl(BASE_URL).build();

    urlExtension.remove(AttributeNames.Custom.BASE_URL);

    Assertions.assertThrows(InvalidConfigException.class, urlExtension::getBaseUrl);
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testSetterMethods()
  {
    ServiceProviderUrlExtension urlExtension = ServiceProviderUrlExtension.builder().baseUrl(BASE_URL).build();
    Assertions.assertThrows(InvalidConfigException.class, () -> urlExtension.setBaseUrl(null));
    Assertions.assertEquals(BASE_URL, urlExtension.getBaseUrl());
    String newUrl = "https://www.example.com/scim/v2";
    urlExtension.setBaseUrl(newUrl);
    Assertions.assertEquals(newUrl, urlExtension.getBaseUrl());
  }
}
