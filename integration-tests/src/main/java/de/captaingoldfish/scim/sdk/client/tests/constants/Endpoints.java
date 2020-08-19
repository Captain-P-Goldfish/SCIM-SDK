package de.captaingoldfish.scim.sdk.client.tests.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;



/**
 * <br>
 * <br>
 * created at: 17.05.2020
 *
 * @author Pascal Knüppel
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Endpoints
{

  /**
   * the context path to the test-controller endpoint with the scim endpoint
   */
  public static final String SCIM_ENDPOINT_PATH = "/scim/v2";

  /**
   * the context path to the test-controller endpoint
   */
  public static final String GET_ENDPOINT_PATH = "get-endpoint";

  /**
   * this endpoint is supposed to provoke an error due to a thread.sleep
   */
  public static final String TIMEOUT_ENDPOINT_PATH = "bad-test-endpoint";

  /**
   * this context path points to a method that will accept post requests
   */
  public static final String POST_ENDPOINT_PATH = "post-test-endpoint";

}
