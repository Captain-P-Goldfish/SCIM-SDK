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
public final class TestConstants
{

  /**
   * the basepath for reading resources
   */
  public static final String BASE_PATH = "/de/captaingoldfish/scim/sdk/client/tests";

  /**
   * the super admin role for testing with the scim server
   */
  public static final String ADMIN_ROLE = "ADMIN";

  /**
   * the role for a simple user
   */
  public static final String USER_ROLE = "USER";

  /**
   * username of the user that is granted access to the scim endpoints
   */
  public static final String AUTHORIZED_USERNAME = "admin";

  /**
   * username of the user that is NOT granted access to the scim endpoints
   */
  public static final String UNAUTHORIZED_USERNAME = "unauthorized";

  /**
   * standard password for basic auth
   */
  public static final String PASSWORD = "1\" äöü3$5&";
}
