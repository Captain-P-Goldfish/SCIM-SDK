package de.captaingoldfish.scim.sdk.common.constants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 21:45 <br>
 * <br>
 */
public class EndpointPathsTest
{


  /**
   * this test verifies that the defined endpoint paths do have the correct values
   */
  @Test
  public void testEndpointPathValuesDefinedByScim()
  {
    Assertions.assertEquals("/ResourceTypes", EndpointPaths.RESOURCE_TYPES);
    Assertions.assertEquals("/Groups", EndpointPaths.GROUPS);
    Assertions.assertEquals("/Me", EndpointPaths.ME);
    Assertions.assertEquals("/Users", EndpointPaths.USERS);
    Assertions.assertEquals("/Schemas", EndpointPaths.SCHEMAS);
    Assertions.assertEquals("/ServiceProviderConfig", EndpointPaths.SERVICE_PROVIDER_CONFIG);
    Assertions.assertEquals("/Bulk", EndpointPaths.BULK);
  }
}
