package de.captaingoldfish.scim.sdk.client.springboot;

/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 13:09 <br>
 * <br>
 */
public final class SecurityConstants
{

  /**
   * the super admin role for testing with the scim server
   */
  public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

  /**
   * profile for basic authentication
   */
  public static final String BASIC_PROFILE = "basic";

  /**
   * profile for x509 client authentication
   */
  public static final String X509_PROFILE = "x509";

  /**
   * username of the user that is granted access to the scim endpoints
   */
  public static final String AUTHORIZED_USERNAME = "test";

  /**
   * username of the user that is NOT granted access to the scim endpoints
   */
  public static final String UNAUTHORIZED_USERNAME = "test-unauthorized";

  /**
   * standard password for basic auth
   */
  public static final String PASSWORD = "1\" äöü3$5&";
}
