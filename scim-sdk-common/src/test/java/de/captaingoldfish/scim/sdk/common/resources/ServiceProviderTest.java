package de.captaingoldfish.scim.sdk.common.resources;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
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
    MatcherAssert.assertThat(serviceProvider, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertEquals(8, serviceProvider.size());
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.RFC7643.SCHEMAS));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.RFC7643.PATCH));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.RFC7643.BULK));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.RFC7643.FILTER));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.RFC7643.CHANGE_PASSWORD));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.RFC7643.SORT));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.RFC7643.ETAG));
    Assertions.assertNotNull(serviceProvider.get(AttributeNames.RFC7643.META));
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder().documentationUri("test").build();

    serviceProvider.remove(AttributeNames.RFC7643.DOCUMENTATION_URI);
    serviceProvider.remove(AttributeNames.RFC7643.PATCH);
    serviceProvider.remove(AttributeNames.RFC7643.BULK);
    serviceProvider.remove(AttributeNames.RFC7643.FILTER);
    serviceProvider.remove(AttributeNames.RFC7643.CHANGE_PASSWORD);
    serviceProvider.remove(AttributeNames.RFC7643.SORT);
    serviceProvider.remove(AttributeNames.RFC7643.ETAG);

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
}
