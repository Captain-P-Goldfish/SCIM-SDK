package de.captaingoldfish.scim.sdk.jboss.sample;

import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.SortConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;
import de.captaingoldfish.scim.sdk.jboss.sample.handler.GroupHandler;
import de.captaingoldfish.scim.sdk.jboss.sample.handler.UserHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 24.11.2019 - 19:27 <br>
 * <br>
 * represents the configuration of the SCIM endpoint
 */
@Slf4j
@ApplicationScoped
public class ScimConfig
{

  /**
   * the resource endpoint which is the actual SCIM processing unit
   */
  @Getter
  private ResourceEndpoint resourceEndpoint;

  /**
   * iniitalize the resource endpoint
   */
  @PostConstruct
  public void init()
  {
    ServiceProvider serviceProvider = getServiceProvider();
    log.info("using service provider configuration: {}", serviceProvider.toPrettyString());
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    registerUserEndpoint();
    registerGroupEndpoint();
  }

  /**
   * builds a service provider configuration
   */
  private ServiceProvider getServiceProvider()
  {
    AuthenticationScheme authScheme = AuthenticationScheme.builder()
                                                          .name("Bearer")
                                                          .description("Authentication scheme using the OAuth "
                                                                       + "Bearer Token Standard")
                                                          .specUri("http://www.rfc-editor.org/info/rfc6750")
                                                          .type("oauthbearertoken")
                                                          .build();
    return ServiceProvider.builder()
                          .filterConfig(FilterConfig.builder().supported(true).maxResults(50).build())
                          .sortConfig(SortConfig.builder().supported(true).build())
                          .changePasswordConfig(ChangePasswordConfig.builder().supported(true).build())
                          .bulkConfig(BulkConfig.builder().supported(true).maxOperations(10).build())
                          .patchConfig(PatchConfig.builder().supported(true).build())
                          .authenticationSchemes(Collections.singletonList(authScheme))
                          .eTagConfig(ETagConfig.builder().supported(true).build())
                          .build();
  }

  /**
   * registers the users endpoint and activates auto filtering and auto sorting feature
   */
  private void registerUserEndpoint()
  {
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(new UserHandler()));
    userResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
  }

  /**
   * registers the groups endpoint and activates auto filtering and auto sorting feature
   */
  private void registerGroupEndpoint()
  {
    ResourceType groupResourceType = resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(new GroupHandler()));
    groupResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
  }

}
