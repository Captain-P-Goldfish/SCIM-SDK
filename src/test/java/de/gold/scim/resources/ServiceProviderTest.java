package de.gold.scim.resources;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.AttributeNames;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 11:49 <br>
 * <br>
 */
@Slf4j
public class ServiceProviderTest
{

  /**
   * this test will show that the service provider config is not empty even no configurations have been given to
   * the builder
   */
  @Test
  public void testBuildServiceProviderConfigNoParameters()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder().build();
    log.warn(serviceProvider.toPrettyString());
    MatcherAssert.assertThat(serviceProvider, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertEquals(7, serviceProvider.size());
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.SCHEMAS));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.PATCH));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.BULK));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.FILTER));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.CHANGE_PASSWORD));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.SORT));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.ETAG));
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder().documentationUri("test").build();

    serviceProvider.remove(AttributeNames.DOCUMENTATION_URI);
    serviceProvider.remove(AttributeNames.PATCH);
    serviceProvider.remove(AttributeNames.BULK);
    serviceProvider.remove(AttributeNames.FILTER);
    serviceProvider.remove(AttributeNames.CHANGE_PASSWORD);
    serviceProvider.remove(AttributeNames.SORT);
    serviceProvider.remove(AttributeNames.ETAG);

    Assertions.assertNotNull(serviceProvider.getDocumentationUri());
    Assertions.assertNotNull(serviceProvider.getPatchConfig());
    Assertions.assertNotNull(serviceProvider.getBulkConfig());
    Assertions.assertNotNull(serviceProvider.getFilterConfig());
    Assertions.assertNotNull(serviceProvider.getChangePasswordConfig());
    Assertions.assertNotNull(serviceProvider.getSortConfig());
    Assertions.assertNotNull(serviceProvider.getETagConfig());
    Assertions.assertNotNull(serviceProvider.getAuthenticationSchemes());
    MatcherAssert.assertThat(serviceProvider.getAuthenticationSchemes(), Matchers.empty());
  }

  /**
   * verifies that the url extension can successfully be set and removed
   */
  @Test
  public void testGetAndSetUrlExtension()
  {
    final String baseUrl = "https://localhost:7scim/v2";
    ServiceProviderUrlExtension urlExtension = ServiceProviderUrlExtension.builder().baseUrl(baseUrl).build();
    ServiceProvider serviceProvider = ServiceProvider.builder().serviceProviderUrlExtension(urlExtension).build();
    Assertions.assertTrue(serviceProvider.getServiceProviderUrlExtension().isPresent());
    Assertions.assertEquals(urlExtension, serviceProvider.getServiceProviderUrlExtension().get());
    Assertions.assertEquals(baseUrl, serviceProvider.getServiceProviderUrlExtension().get().getBaseUrl());

    serviceProvider.setServiceProviderUrlExtension(null);
    Assertions.assertFalse(serviceProvider.getServiceProviderUrlExtension().isPresent());
  }
}
