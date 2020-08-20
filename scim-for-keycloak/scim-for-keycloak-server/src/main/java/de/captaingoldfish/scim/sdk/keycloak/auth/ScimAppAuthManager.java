package de.captaingoldfish.scim.sdk.keycloak.auth;

import org.keycloak.TokenVerifier;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;


public class ScimAppAuthManager extends AppAuthManager
{

  private TokenVerifier.Predicate<? super AccessToken> additionalCheck = new ScopeCheck();

  public ScimAppAuthManager()
  {

  }

  public AuthResult authenticateBearerToken(String tokenString,
                                            KeycloakSession session,
                                            RealmModel realm,
                                            UriInfo uriInfo,
                                            ClientConnection connection,
                                            HttpHeaders headers)
  {
    if (tokenString == null)
    {
      return null;
    }
    else
    {
      AuthResult authResult = verifyIdentityToken(session,
                                                  realm,
                                                  uriInfo,
                                                  connection,
                                                  true,
                                                  true,
                                                  false,
                                                  tokenString,
                                                  headers,
                                                  additionalCheck);
      return authResult;
    }
  }
}
