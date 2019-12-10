package de.captaingoldfish.scim.sdk.client.setup.scim;

import java.util.Collections;

import de.captaingoldfish.scim.sdk.client.setup.scim.handler.GroupHandler;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.SortConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 10.12.2019 - 13:02 <br>
 * <br>
 */
@Getter
public class ScimConfig
{

  /**
   * the server resource endpoint implementation
   */
  private ResourceEndpoint resourceEndpoint;

  /**
   * the service provider configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * the user resource type definition
   */
  private ResourceType userResourceType;

  /**
   * the group resource type definition
   */
  private ResourceType groupResourceType;

  public ScimConfig()
  {
    this.serviceProvider = getServiceProviderConfig();
    this.resourceEndpoint = new ResourceEndpoint(serviceProvider);
    this.userResourceType = getUserResourceType(resourceEndpoint);
    this.groupResourceType = getGroupResourceType(resourceEndpoint);
  }

  /**
   * create the service provider configuration
   */
  private ServiceProvider getServiceProviderConfig()
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
  private ResourceEndpoint getResourceEndpoint(ServiceProvider serviceProvider)
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
  private ResourceType getUserResourceType(ResourceEndpoint resourceEndpoint)
  {
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(new UserHandler()));
    userResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
    return userResourceType;
  }

  /**
   * gets the group resource type that is the core object of the /Groups endpoint. We will also activate the
   * auto-filtering extension so that filtering will work on the fly
   *
   * @param resourceEndpoint the resource endpoint that was previously defined
   * @return the group resource type
   */
  private ResourceType getGroupResourceType(ResourceEndpoint resourceEndpoint)
  {
    ResourceType groupResourceType = resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(new GroupHandler()));
    groupResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
    return groupResourceType;
  }
}
