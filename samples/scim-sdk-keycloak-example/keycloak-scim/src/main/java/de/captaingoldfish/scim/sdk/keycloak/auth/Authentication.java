package de.captaingoldfish.scim.sdk.keycloak.auth;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.AdminAuth;

import de.captaingoldfish.scim.sdk.keycloak.provider.RealmRoleInitializer;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@Slf4j
public class Authentication
{

  private static final String ERROR_MESSAGE_AUTHENTICATION_FAILED = "Authentication failed";

  /**
   * used to authenticate the user
   */
  private static final AppAuthManager APP_AUTH_MANAGER = new AppAuthManager();

  /**
   * Authenticates the calling user and client according to the Bearer Token in the HTTP header.
   *
   * @return authentication result object
   * @throws ClientErrorException on authentication errors
   */
  public static AdminAuth authenticate(KeycloakSession keycloakSession)
  {
    KeycloakContext context = keycloakSession.getContext();
    String accessToken = APP_AUTH_MANAGER.extractAuthorizationHeaderToken(context.getRequestHeaders());
    if (accessToken == null)
    {
      log.error(ERROR_MESSAGE_AUTHENTICATION_FAILED);
      throw new NotAuthorizedException(ERROR_MESSAGE_AUTHENTICATION_FAILED);
    }
    AuthenticationManager.AuthResult result = APP_AUTH_MANAGER.authenticateBearerToken(accessToken,
                                                                                       keycloakSession,
                                                                                       context.getRealm(),
                                                                                       context.getUri(),
                                                                                       context.getConnection(),
                                                                                       context.getRequestHeaders());

    if (result == null)
    {
      log.error(ERROR_MESSAGE_AUTHENTICATION_FAILED);
      throw new NotAuthorizedException(ERROR_MESSAGE_AUTHENTICATION_FAILED);
    }
    return createAdminAuth(keycloakSession, result);
  }

  /**
   * checks if the just logged in user was granted the {@link RealmRoleInitializer#SCIM_ADMIN_ROLE} to access
   * the SCIM administration
   * 
   * @param keycloakSession the current request context
   */
  public static void authenticateAsScimAdmin(KeycloakSession keycloakSession)
  {
    AdminAuth adminAuth = Authentication.authenticate(keycloakSession);
    RoleModel roleModel = keycloakSession.getContext()
                                         .getRealm()
                                         .getMasterAdminClient()
                                         .getRole(RealmRoleInitializer.SCIM_ADMIN_ROLE);
    boolean accessGranted = adminAuth.getUser().hasRole(roleModel);
    if (!accessGranted)
    {
      throw new NotAuthorizedException(ERROR_MESSAGE_AUTHENTICATION_FAILED);
    }
  }

  /**
   * creates a valid authentication object for the user
   *
   * @param result the result of the users authentication
   * @return the authentication object for the user
   */
  private static AdminAuth createAdminAuth(KeycloakSession keycloakSession, AuthenticationManager.AuthResult result)
  {
    KeycloakContext context = keycloakSession.getContext();
    RealmModel realm = context.getRealm();
    ClientModel client = realm.getClientByClientId(result.getToken().getIssuedFor());
    if (client == null)
    {
      log.error(ERROR_MESSAGE_AUTHENTICATION_FAILED);
      throw new NotFoundException(ERROR_MESSAGE_AUTHENTICATION_FAILED);
    }
    AdminAuth adminAuth = new AdminAuth(realm, result.getToken(), result.getUser(), client);
    log.debug("user '{}' was successfully authenticated", adminAuth.getUser().getUsername());
    return adminAuth;
  }
}
