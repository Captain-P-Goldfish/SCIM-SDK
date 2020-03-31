package de.captaingoldfish.scim.sdk.keycloak.auth;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.JsonWebToken;

import java.util.List;
import java.util.Map;


public class ScopeCheck implements TokenVerifier.Predicate<JsonWebToken>
{

  private static final ScopeCheck NULL_INSTANCE = new ScopeCheck();

  private final String[] scopes;

  private final String[] clientIds;

  private static final String SCOPE = "http://smartfacts.com/claims/scope";

  public ScopeCheck()
  {
    this.scopes = System.getProperty("mid.scopes", "mid:scim").split(";");
    this.clientIds = System.getProperty("mid.clientIds", "scim").split(";");
  }

  public boolean test(JsonWebToken token) throws VerificationException
  {
    if (this.scopes == null)
    {
      throw new VerificationException("Scope not set");
    }
    boolean found = false;
    for ( String clientId : this.clientIds )
    {
      if (token.issuedFor.equals(clientId))
      {
        found = true;
        break;
      }
    }
    if (!found)
    {
      throw new VerificationException("Token not issued for one of: " + String.join(", ", this.clientIds));
    }
    Map<String, Object> claims = token.getOtherClaims();
    Object scopesObject = claims.get(ScopeCheck.SCOPE);
    if (scopesObject == null)
    {
      scopesObject = ((org.keycloak.representations.AccessToken)token).getScope();
    }
    if (scopesObject == null)
    {
      throw new VerificationException("No Scopes in token found");
    }
    else if (scopesObject instanceof String)
    {
      String scopeString = (String)scopesObject;
      for ( String sendScope : scopeString.split((" ")) )
      {
        for ( String scope : this.scopes )
        {
          if (scope.equals(sendScope))
          {
            return true;
          }
        }
      }

      throw new VerificationException("Invalid scope. Expected one of'" + String.join(", ", this.scopes)
                                      + "', but was '" + scopeString + "'");
    }
    else if (scopesObject instanceof List)
    {
      List<String> scopesList = (List<String>)scopesObject;

      for ( String scope : this.scopes )
      {
        if (scopesList.indexOf(scope) > -1)
        {
          return true;
        }
      }
      throw new VerificationException("Invalid scope. Expected one of'" + String.join(", ", this.scopes)
                                      + "', but was '" + String.join(",", scopesList) + "'");
    }
    throw new VerificationException("No Scopes in token found");
  }
}
