package de.captaingoldfish.scim.sdk.keycloak.scim;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.GroupHandler;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimResourceTypeService;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderService;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 04.02.2020
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScimConfiguration
{

  /**
   * holds a different SCIM configuration and endpoints for each realm <br />
   * <br />
   * the key of the map is the name of the realm
   */
  @Getter(AccessLevel.PROTECTED) // used for unit tests
  private static final Map<String, ResourceEndpoint> RESOURCE_ENDPOINT_MAP = new HashMap<>();

  /**
   * gets the SCIM resource endpoint for the given realm
   *
   * @param keycloakSession used to check for existing {@link ServiceProvider}s in the database
   * @return the SCIM resource endpoint for the given realm
   */
  public static ResourceEndpoint getScimEndpoint(KeycloakSession keycloakSession)
  {
    RealmModel realm = keycloakSession.getContext().getRealm();
    ResourceEndpoint resourceEndpoint = RESOURCE_ENDPOINT_MAP.get(realm.getName());
    if (resourceEndpoint == null)
    {
      resourceEndpoint = createNewResourceEndpoint(keycloakSession);
      RESOURCE_ENDPOINT_MAP.put(realm.getName(), resourceEndpoint);
    }
    return resourceEndpoint;
  }

  /**
   * creates a new resource endpoint for the current realm
   */
  private static ResourceEndpoint createNewResourceEndpoint(KeycloakSession keycloakSession)
  {
    ScimServiceProviderService scimServiceProviderService = new ScimServiceProviderService(keycloakSession);
    ServiceProvider serviceProvider = scimServiceProviderService.getServiceProvider();
    ResourceEndpoint resourceEndpoint = new ResourceEndpoint(serviceProvider);

    ScimResourceTypeService resourceTypeService = new ScimResourceTypeService(keycloakSession);

    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(new UserHandler()));
    ResourceTypeAuthorization userAuthorization = ResourceTypeAuthorization.builder().authenticated(false).build();
    userResourceType.setFeatures(ResourceTypeFeatures.builder()
                                                     .autoFiltering(true)
                                                     .autoSorting(true)
                                                     .authorization(userAuthorization)
                                                     .build());
    setUserAttributeRestrictions(userResourceType);
    ScimResourceTypeEntity userResourceTypeEntity = resourceTypeService.getOrCreateResourceTypeEntry(userResourceType);
    resourceTypeService.updateResourceType(userResourceType, userResourceTypeEntity);

    ResourceType groupResourceType = resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(new GroupHandler()));
    ResourceTypeAuthorization groupAuthorization = ResourceTypeAuthorization.builder().authenticated(false).build();
    groupResourceType.setFeatures(ResourceTypeFeatures.builder()
                                                      .autoFiltering(true)
                                                      .autoSorting(true)
                                                      .authorization(groupAuthorization)
                                                      .build());
    ScimResourceTypeEntity groupResourceTypeEntity = resourceTypeService.getOrCreateResourceTypeEntry(groupResourceType);
    resourceTypeService.updateResourceType(groupResourceType, groupResourceTypeEntity);

    return resourceEndpoint;
  }

  /**
   * sets attribute validation on the user attribute "username"<br>
   * <br>
   * <b>NOTE:</b><br>
   * This method is simply an example to show what else the SCIM-SDK API can do for you
   *
   * @param userResourceType the resource type to access the username attribute
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Attribute-validation">
   *      https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Attribute-validation </a>
   */
  private static void setUserAttributeRestrictions(ResourceType userResourceType)
  {
    Schema user = userResourceType.getMainSchema();
    SchemaAttribute username = user.getSchemaAttribute(AttributeNames.RFC7643.USER_NAME);
    username.setPattern("[a-z\\-_ ]+");
  }

}
