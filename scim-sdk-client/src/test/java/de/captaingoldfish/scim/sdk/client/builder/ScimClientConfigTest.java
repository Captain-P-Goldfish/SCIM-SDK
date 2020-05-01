package de.captaingoldfish.scim.sdk.client.builder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 11:52 <br>
 * <br>
 */
public class ScimClientConfigTest
{

  /**
   * verifies that the default values for the timeout values are set
   */
  @Test
  public void testDefaultValuesAreGettingSet()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder().build();
    Assertions.assertEquals(ScimClientConfig.DEFAULT_TIMEOUT, scimClientConfig.getRequestTimeout());
    Assertions.assertEquals(ScimClientConfig.DEFAULT_TIMEOUT, scimClientConfig.getConnectTimeout());
    Assertions.assertEquals(ScimClientConfig.DEFAULT_TIMEOUT, scimClientConfig.getSocketTimeout());
  }

  /**
   * verifies that values from the builder are used and not the default values
   */
  @Test
  public void testBuilderValuesAreGettingSetCorrectly()
  {
    final int timeout = 1;
    Assertions.assertNotEquals(ScimClientConfig.DEFAULT_TIMEOUT, timeout);
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .connectTimeout(timeout)
                                                        .requestTimeout(timeout)
                                                        .socketTimeout(timeout)
                                                        .build();
    Assertions.assertEquals(timeout, scimClientConfig.getRequestTimeout());
    Assertions.assertEquals(timeout, scimClientConfig.getConnectTimeout());
    Assertions.assertEquals(timeout, scimClientConfig.getSocketTimeout());
  }
}
