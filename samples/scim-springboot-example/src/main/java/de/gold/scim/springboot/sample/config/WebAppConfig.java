package de.gold.scim.springboot.sample.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import de.gold.scim.common.resources.ServiceProvider;
import de.gold.scim.common.resources.complex.AuthenticationScheme;
import de.gold.scim.common.resources.complex.BulkConfig;
import de.gold.scim.common.resources.complex.ChangePasswordConfig;
import de.gold.scim.common.resources.complex.FilterConfig;
import de.gold.scim.common.resources.complex.PatchConfig;
import de.gold.scim.common.resources.complex.SortConfig;
import de.gold.scim.server.endpoints.ResourceEndpoint;
import de.gold.scim.server.endpoints.base.UserEndpointDefinition;
import de.gold.scim.server.schemas.ResourceType;
import de.gold.scim.server.schemas.ResourceTypeFeatures;
import de.gold.scim.springboot.sample.handler.UserHandler;


/**
 * author Pascal Knueppel <br>
 * created at: 02.11.2019 - 23:58 <br>
 * <br>
 */
@Configuration
@EnableWebMvc
@ComponentScan("de.gold.scim.springboot.sample")
public class WebAppConfig
{

  /**
   * create the service provider configuration
   */
  @Bean
  public ServiceProvider getServiceProviderConfig()
  {
    AuthenticationScheme authScheme = AuthenticationScheme.builder()
                                                          .name("OAuth Bearer Token")
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
                          .build();
  }

  /**
   * creates a resource endpoint for scim
   *
   * @param serviceProvider the service provider configuration
   * @return the resource endpoint
   */
  @Bean
  public ResourceEndpoint getResourceEndpoint(ServiceProvider serviceProvider)
  {
    return new ResourceEndpoint(serviceProvider);
  }

  /**
   * gets the user resource type that is the core object of the /Users endpoint. We will also activate the
   * auto-filtering extension so that filtering will work on the fly
   *
   * @param resourceEndpoint the resource endpoint that was previously defined
   * @return the user resource type
   */
  @Bean
  public ResourceType getUserResourceType(ResourceEndpoint resourceEndpoint)
  {
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(new UserHandler()));
    userResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
    return userResourceType;
  }

}
