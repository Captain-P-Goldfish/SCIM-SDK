package de.captaingoldfish.scim.sdk.common.constants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 21:51 <br>
 * <br>
 */
public class ResourceTypeNamesTest
{


  /**
   * will verify that the constants do have the correct values
   */
  @Test
  public void testResourceTypeNamesDefinedByScim()
  {
    Assertions.assertEquals("User", ResourceTypeNames.USER);
    Assertions.assertEquals("Group", ResourceTypeNames.GROUPS);
    Assertions.assertEquals("Me", ResourceTypeNames.ME);
    Assertions.assertEquals("ResourceType", ResourceTypeNames.RESOURCE_TYPE);
    Assertions.assertEquals("Schema", ResourceTypeNames.SCHEMA);
    Assertions.assertEquals("ServiceProviderConfig", ResourceTypeNames.SERVICE_PROVIDER_CONFIG);
  }
}
