package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.SortConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.GroupHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;


/**
 * author Pascal Knueppel <br>
 * created at: 14.07.2020 - 11:06 <br>
 * <br>
 */
public class AbstractEndpointTest extends AbstractBulkTest implements FileReferences
{

  /**
   * a simple basic uri used in these tests
   */
  protected static final String BASE_URI = "https://localhost/scim/v2";

  /**
   * the resource endpoint under test
   */
  protected ResourceEndpoint resourceEndpoint;

  /**
   * a watchable user handler implementation
   */
  protected UserHandlerImpl userHandler;

  /**
   * a watchable group handler implementation
   */
  protected GroupHandlerImpl groupHandler;

  /**
   * the service provider configuration
   */
  protected ServiceProvider serviceProvider;

  /**
   * the http header map that is validated on a request and contains authentication details
   */
  protected Map<String, String> httpHeaders = new HashMap<>();

  /**
   * create the service provider configuration
   */
  protected ServiceProvider getServiceProviderConfig()
  {
    AuthenticationScheme authScheme = AuthenticationScheme.builder()
                                                          .type("httpbasic")
                                                          .description("Username Password authentication challenge")
                                                          .specUri("https://tools.ietf.org/html/rfc7617")
                                                          .name("Basic")
                                                          .build();
    AuthenticationScheme authScheme2 = AuthenticationScheme.builder()
                                                           .type("oauthbearertoken")
                                                           .description("Authentication scheme using the OAuth "
                                                                        + "Bearer Token Standard")
                                                           .specUri("http://www.rfc-editor.org/info/rfc6750")
                                                           .name("Bearer")
                                                           .build();
    return ServiceProvider.builder()
                          .filterConfig(FilterConfig.builder().supported(true).maxResults(50).build())
                          .sortConfig(SortConfig.builder().supported(true).build())
                          .changePasswordConfig(ChangePasswordConfig.builder().supported(true).build())
                          .bulkConfig(BulkConfig.builder().supported(true).maxOperations(50).build())
                          .patchConfig(PatchConfig.builder().supported(true).build())
                          .authenticationSchemes(Arrays.asList(authScheme, authScheme2))
                          .eTagConfig(ETagConfig.builder().supported(true).build())
                          .build();
  }

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    serviceProvider = getServiceProviderConfig();
    userHandler = Mockito.spy(new UserHandlerImpl(true));
    groupHandler = Mockito.spy(new GroupHandlerImpl());
    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler),
                                            new GroupEndpointDefinition(groupHandler));
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
  }

  /**
   * gets the whole url of the scim provider
   *
   * @param endpoint the current endpoint that should be accessed
   */
  protected String getUrl(String endpoint)
  {
    return BASE_URI + endpoint;
  }
}
