package de.captaingoldfish.scim.sdk.keycloak.services;

import java.time.Instant;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.ParseableResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ETagFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Slf4j
public class ScimResourceTypeService extends AbstractService
{


  public ScimResourceTypeService(KeycloakSession keycloakSession)
  {
    super(keycloakSession);
  }

  /**
   * gets an existing configuration from the database for the given resource type or creates a configuration in
   * the database that matches the settings of the given resource type if no entry for this resource type was
   * present yet
   * 
   * @param resourceType the resource type that might have a configuration in the database or not
   * @return the existing configuration or a configuration that matches the given resource type
   */
  public ScimResourceTypeEntity getOrCreateResourceTypeEntry(ResourceType resourceType)
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    Optional<ScimResourceTypeEntity> optionalResourceTypeEntity = getResourceTypeEntityByName(resourceType.getName());
    if (!optionalResourceTypeEntity.isPresent())
    {
      log.info("no database entry found for resource type {}. Entry will be created", resourceType.getName());
      return createNewResourceTypeEntry(resourceType, realmModel);
    }
    return optionalResourceTypeEntity.get();
  }

  /**
   * uses the given parsed resource type data and puts its values into the corresponding database entry
   * 
   * @param parseableResourceType the resource type data that was parsed from a http request body
   * @return empty if no entry with the given resource type name exists in the database and the updated database
   *         entry if the update was successful
   */
  public Optional<ScimResourceTypeEntity> updateDatabaseEntry(ParseableResourceType parseableResourceType)
  {
    Optional<ScimResourceTypeEntity> resourceTypeEntity = getResourceTypeEntityByName(parseableResourceType.getName());
    if (!resourceTypeEntity.isPresent())
    {
      return resourceTypeEntity;
    }
    ScimResourceTypeEntity scimResourceTypeEntity = resourceTypeEntity.get();
    setValuesOfEntity(scimResourceTypeEntity, parseableResourceType);
    scimResourceTypeEntity.setLastModified(Instant.now());
    getEntityManager().flush();
    return Optional.of(scimResourceTypeEntity);
  }

  /**
   * creates a new database entry for the given resource type
   * 
   * @param resourceType the resource type whose representation should be found within the database
   * @param realmModel the owning realm of the resource type
   * @return the database representation of the resource type
   */
  private ScimResourceTypeEntity createNewResourceTypeEntry(ResourceType resourceType, RealmModel realmModel)
  {
    ScimResourceTypeEntity scimResourceTypeEntity = ScimResourceTypeEntity.builder()
                                                                          .realmId(realmModel.getId())
                                                                          .name(resourceType.getName())
                                                                          .created(Instant.now())
                                                                          .build();
    getEntityManager().persist(scimResourceTypeEntity);
    setValuesOfEntity(scimResourceTypeEntity, resourceType);
    getEntityManager().flush();
    return scimResourceTypeEntity;
  }

  /**
   * tries to find a resource type within the database by its name
   * 
   * @param name the resource type name that may have a database entry
   * @return the database representation of the resource type or an empty
   */
  public Optional<ScimResourceTypeEntity> getResourceTypeEntityByName(String name)
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    try
    {
      return Optional.of(getEntityManager().createNamedQuery("getScimResourceType", ScimResourceTypeEntity.class)
                                           .setParameter("realmId", realmModel.getId())
                                           .setParameter("name", name)
                                           .getSingleResult());
    }
    catch (NoResultException ex)
    {
      return Optional.empty();
    }
  }

  /**
   * adds the values of the database entity into the scim representation of the resource type
   */
  public void updateResourceType(ResourceType resourceType, ScimResourceTypeEntity scimResourceTypeEntity)
  {
    resourceType.setDescription(scimResourceTypeEntity.getDescription());

    ResourceTypeFeatures features = resourceType.getFeatures();
    features.setResourceTypeDisabled(!scimResourceTypeEntity.isEnabled());
    features.setSingletonEndpoint(scimResourceTypeEntity.isSingletonEndpoint());
    features.setAutoFiltering(scimResourceTypeEntity.isAutoFiltering());
    features.setAutoSorting(scimResourceTypeEntity.isAutoSorting());
    features.setETagFeature(ETagFeature.builder().enabled(scimResourceTypeEntity.isEtagEnabled()).build());

    EndpointControlFeature endpointControl = features.getEndpointControlFeature();
    endpointControl.setCreateDisabled(scimResourceTypeEntity.isDisableCreate());
    endpointControl.setGetDisabled(scimResourceTypeEntity.isDisableGet());
    endpointControl.setListDisabled(scimResourceTypeEntity.isDisableList());
    endpointControl.setUpdateDisabled(scimResourceTypeEntity.isDisableUpdate());
    endpointControl.setDeleteDisabled(scimResourceTypeEntity.isDisableDelete());

    ResourceTypeAuthorization authorization = features.getAuthorization();
    authorization.setAuthenticated(scimResourceTypeEntity.isRequireAuthentication());

    resourceType.getMeta().ifPresent(meta -> {
      meta.setCreated(scimResourceTypeEntity.getCreated());
      meta.setLastModified(scimResourceTypeEntity.getLastModified());
    });
  }

  /**
   * adds the values of the given scim resource type into the database entity
   */
  private void setValuesOfEntity(ScimResourceTypeEntity scimResourceTypeEntity, ResourceType resourceType)
  {
    scimResourceTypeEntity.setDescription(resourceType.getDescription().orElse(null));

    ResourceTypeFeatures features = resourceType.getFeatures();
    scimResourceTypeEntity.setEnabled(!features.isResourceTypeDisabled());
    scimResourceTypeEntity.setSingletonEndpoint(features.isSingletonEndpoint());
    scimResourceTypeEntity.setAutoFiltering(features.isAutoFiltering());
    scimResourceTypeEntity.setAutoSorting(features.isAutoSorting());
    scimResourceTypeEntity.setEtagEnabled(features.getETagFeature().isEnabled());

    scimResourceTypeEntity.setDisableCreate(features.getEndpointControlFeature().isCreateDisabled());
    scimResourceTypeEntity.setDisableGet(features.getEndpointControlFeature().isGetDisabled());
    scimResourceTypeEntity.setDisableList(features.getEndpointControlFeature().isListDisabled());
    scimResourceTypeEntity.setDisableUpdate(features.getEndpointControlFeature().isUpdateDisabled());
    scimResourceTypeEntity.setDisableDelete(features.getEndpointControlFeature().isDeleteDisabled());

    scimResourceTypeEntity.setRequireAuthentication(features.getAuthorization().isAuthenticated());
    scimResourceTypeEntity.setLastModified(resourceType.getMeta().flatMap(Meta::getLastModified).orElse(Instant.now()));
  }

  /**
   * adds the values of the given scim resource type into the database entity
   */
  private void setValuesOfEntity(ScimResourceTypeEntity scimResourceTypeEntity, ParseableResourceType resourceType)
  {
    scimResourceTypeEntity.setDescription(resourceType.getDescription().orElse(null));

    ResourceTypeFeatures features = resourceType.getFeatures();
    scimResourceTypeEntity.setEnabled(!features.isResourceTypeDisabled());
    scimResourceTypeEntity.setSingletonEndpoint(features.isSingletonEndpoint());
    scimResourceTypeEntity.setAutoFiltering(features.isAutoFiltering());
    scimResourceTypeEntity.setAutoSorting(features.isAutoSorting());
    scimResourceTypeEntity.setEtagEnabled(features.getETagFeature().isEnabled());

    scimResourceTypeEntity.setDisableCreate(features.getEndpointControlFeature().isCreateDisabled());
    scimResourceTypeEntity.setDisableGet(features.getEndpointControlFeature().isGetDisabled());
    scimResourceTypeEntity.setDisableList(features.getEndpointControlFeature().isListDisabled());
    scimResourceTypeEntity.setDisableUpdate(features.getEndpointControlFeature().isUpdateDisabled());
    scimResourceTypeEntity.setDisableDelete(features.getEndpointControlFeature().isDeleteDisabled());

    scimResourceTypeEntity.setRequireAuthentication(features.getAuthorization().isAuthenticated());
    scimResourceTypeEntity.setLastModified(Instant.now());
  }
}
