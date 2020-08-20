package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 13.07.2020 - 10:50 <br>
 * <br>
 */
@Slf4j
public class AuthenticationTest extends AbstractEndpointTest
{

  /**
   * the http header map that is validated on a request and contains authentication details
   */
  private Map<String, String> authorizedHttpHeaders = new HashMap<>();

  /**
   * hte http header map that is validated on a request and does not contain authentication details
   */
  private Map<String, String> unauthorizedHttpHeaders = new HashMap<>();

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    super.initialize();
    {
      unauthorizedHttpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
    }

    {
      authorizedHttpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
      authorizedHttpHeaders.put(HttpHeader.AUTHORIZATION, "Basic MTox");
    }
  }

  /**
   * verifies that resources that do not require authentication can be accessed directly
   */
  @ParameterizedTest
  @ValueSource(strings = {EndpointPaths.SERVICE_PROVIDER_CONFIG, EndpointPaths.RESOURCE_TYPES, EndpointPaths.SCHEMAS})
  public void testAccessUnauthorizedResourcesWithoutAuthentication(String endpoint)
  {
    final String realm = "master";
    ScimAuthorization scimAuthorization = Mockito.spy(new ScimAuthorization(realm));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(endpoint),
                                                               HttpMethod.GET,
                                                               null,
                                                               unauthorizedHttpHeaders,
                                                               scimAuthorization);
    Assertions.assertEquals(HttpStatus.OK, scimResponse.getHttpStatus());
    Mockito.verify(scimAuthorization, Mockito.times(0)).authenticate(Mockito.anyMap(), Mockito.anyMap());
  }

  /**
   * verifies that resources that do require authentication cannot be accessed directly
   */
  @ParameterizedTest
  @ValueSource(strings = {EndpointPaths.USERS, EndpointPaths.GROUPS})
  public void testAccessAuthorizedResourcesWithoutAuthentication(String endpoint)
  {
    final String realm = "master";
    ScimAuthorization scimAuthorization = Mockito.spy(new ScimAuthorization(realm));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(endpoint),
                                                               HttpMethod.GET,
                                                               null,
                                                               unauthorizedHttpHeaders,
                                                               scimAuthorization);
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, scimResponse.getHttpStatus());
    Mockito.verify(scimAuthorization, Mockito.times(1)).authenticate(Mockito.anyMap(), Mockito.anyMap());

    String authenticateHeader = scimResponse.getHttpHeaders().get(HttpHeader.WWW_AUTHENTICATE);
    Assertions.assertNotNull(authenticateHeader);
    Assertions.assertEquals(authenticateHeader, "Basic realm=\"" + realm + "\", Bearer realm=\"" + realm + "\"");
  }

  /**
   * verifies that resources that do require authentication are accessible if the authentication details are
   * provided
   */
  @ParameterizedTest
  @ValueSource(strings = {EndpointPaths.USERS, EndpointPaths.GROUPS})
  public void testAccessAuthorizedResourcesWithAuthentication(String endpoint)
  {
    final String realm = "master";
    ScimAuthorization scimAuthorization = Mockito.spy(new ScimAuthorization(realm));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(endpoint),
                                                               HttpMethod.GET,
                                                               null,
                                                               authorizedHttpHeaders,
                                                               scimAuthorization);
    Assertions.assertEquals(HttpStatus.OK, scimResponse.getHttpStatus());
    Mockito.verify(scimAuthorization, Mockito.times(1)).authenticate(Mockito.anyMap(), Mockito.anyMap());

    String authenticateHeader = scimResponse.getHttpHeaders().get(HttpHeader.WWW_AUTHENTICATE);
    Assertions.assertNull(authenticateHeader);
  }

  /**
   * verifies negative case of executed authentication in bulk requests
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  public void testUnauthenticatedWithBulk(int numberOfOperations)
  {
    final String realm = "master";

    BulkRequest bulkRequest = BulkRequest.builder()
                                         .bulkRequestOperation(getCreateUserBulkOperations(numberOfOperations))
                                         .build();
    ScimAuthorization scimAuthorization = Mockito.spy(new ScimAuthorization(realm));

    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(EndpointPaths.BULK),
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               unauthorizedHttpHeaders,
                                                               scimAuthorization);

    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, scimResponse.getHttpStatus());
    Mockito.verify(scimAuthorization, Mockito.times(1)).authenticate(Mockito.anyMap(), Mockito.anyMap());

    String authenticateHeader = scimResponse.getHttpHeaders().get(HttpHeader.WWW_AUTHENTICATE);
    Assertions.assertNotNull(authenticateHeader);
    Assertions.assertEquals(authenticateHeader, "Basic realm=\"" + realm + "\", Bearer realm=\"" + realm + "\"");
  }

  /**
   * verifies positive case of executed authentication in bulk requests
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  public void testAuthenticatedWithBulk(int numberOfOperations)
  {
    final String realm = "master";

    BulkRequest bulkRequest = BulkRequest.builder()
                                         .bulkRequestOperation(getCreateUserBulkOperations(numberOfOperations))
                                         .build();
    ScimAuthorization scimAuthorization = Mockito.spy(new ScimAuthorization(realm));

    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(EndpointPaths.BULK),
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               authorizedHttpHeaders,
                                                               scimAuthorization);

    Assertions.assertEquals(HttpStatus.OK, scimResponse.getHttpStatus());
    Mockito.verify(scimAuthorization, Mockito.times(numberOfOperations))
           .authenticate(Mockito.anyMap(), Mockito.anyMap());

    String authenticateHeader = scimResponse.getHttpHeaders().get(HttpHeader.WWW_AUTHENTICATE);
    Assertions.assertNull(authenticateHeader);
  }

  /**
   * a custom scim authorization
   */
  @RequiredArgsConstructor
  public static class ScimAuthorization implements Authorization
  {

    /**
     * the realm that is represented by the current resource endpoint
     */
    private final String realm;

    /**
     * not used within this test
     */
    @Override
    public Set<String> getClientRoles()
    {
      return null;
    }

    /**
     * authenticates the user with basic authentication
     */
    @Override
    public boolean authenticate(Map<String, String> httpHeaders, Map<String, String> queryParams)
    {
      String authorization = httpHeaders.get(HttpHeader.AUTHORIZATION);
      return Optional.ofNullable(authorization)
                     .map(auth -> StringUtils.startsWithIgnoreCase(auth, "basic ")
                                  && StringUtils.endsWith(auth, "MTox"))
                     .orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRealm()
    {
      return realm;
    }
  }
}
