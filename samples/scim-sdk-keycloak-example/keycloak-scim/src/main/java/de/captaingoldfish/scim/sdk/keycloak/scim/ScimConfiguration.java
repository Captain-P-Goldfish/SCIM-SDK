package de.captaingoldfish.scim.sdk.keycloak.scim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.RealmModel;

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
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.GroupHandler;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScimConfiguration
{

  /**
   * holds a different SCIM configuration and endpoints for each realm <br />
   * <br />
   * the key of the map is the name of the realm
   */
  private static final Map<String, ResourceEndpoint> RESOURCE_ENDPOINT_MAP = new HashMap<>();

  /**
   * gets the SCIM resource endpoint for the given realm
   *
   * @param realm the for which the resource endpoint should be returned
   * @return the SCIM resource endpoint for the given realm
   */
  public static ResourceEndpoint getScimEndpoint(RealmModel realm)
  {
    ResourceEndpoint resourceEndpoint = RESOURCE_ENDPOINT_MAP.get(realm.getName());
    if (resourceEndpoint == null)
    {
      resourceEndpoint = createNewResourceEndpoint();
      RESOURCE_ENDPOINT_MAP.put(realm.getName(), resourceEndpoint);
    }
    return resourceEndpoint;
  }

  /**
   * creates a new resource endpoint for the current realm
   */
  private static ResourceEndpoint createNewResourceEndpoint()
  {
    ResourceEndpoint resourceEndpoint = new ResourceEndpoint(getServiceProviderConfig());

    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(new UserHandler()));
    userResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
    setUserAttributeRestrictions(userResourceType);

    ResourceType groupResourceType = resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(new GroupHandler()));
    groupResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());


    return resourceEndpoint;
  }

  /**
   * sets attribute validation on the user attribute "username"
   *
   * @param userResourceType the resource type to access the username attribute
   */
  private static void setUserAttributeRestrictions(ResourceType userResourceType)
  {
    Schema user = userResourceType.getMainSchema();
    SchemaAttribute username = user.getSchemaAttribute(AttributeNames.RFC7643.USER_NAME);
    username.setPattern("[a-z\\-_ ]+");
  }

  /**
   * create the service provider configuration
   */
  private static ServiceProvider getServiceProviderConfig()
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

}
