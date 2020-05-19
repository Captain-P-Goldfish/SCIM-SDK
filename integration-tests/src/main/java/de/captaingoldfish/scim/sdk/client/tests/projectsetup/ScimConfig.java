package de.captaingoldfish.scim.sdk.client.tests.projectsetup;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import de.captaingoldfish.scim.sdk.client.tests.constants.TestConstants;
import de.captaingoldfish.scim.sdk.client.tests.scim.GroupHandler;
import de.captaingoldfish.scim.sdk.client.tests.scim.UserHandler;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.SortConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * configuration for the scim server
 */
@Configuration
public class ScimConfig implements WebMvcConfigurer
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
                          .eTagConfig(ETagConfig.builder().supported(true).build())
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
    userResourceType.getFeatures().getAuthorization().setRoles(TestConstants.ADMIN_ROLE);
    Schema schema = userResourceType.getMainSchema();
    SchemaAttribute username = schema.getSchemaAttribute(AttributeNames.RFC7643.USER_NAME);
    username.setPattern("[\\w_]+");
    return userResourceType;
  }

  /**
   * gets the group resource type that is the core object of the /Groups endpoint. We will also activate the
   * auto-filtering extension so that filtering will work on the fly
   *
   * @param resourceEndpoint the resource endpoint that was previously defined
   * @return the group resource type
   */
  @Bean
  public ResourceType getGroupResourceType(ResourceEndpoint resourceEndpoint)
  {
    ResourceType groupResourceType = resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(new GroupHandler()));
    groupResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
    groupResourceType.getFeatures().getAuthorization().setRoles(TestConstants.ADMIN_ROLE);
    return groupResourceType;
  }
}
