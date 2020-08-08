package de.captaingoldfish.scim.sdk.keycloak.services;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.SortConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;


/**
 * This implementation is used to handle actions on
 * {@link de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity} objects
 * 
 * @author Pascal Knueppel
 * @since 02.08.2020
 */
public class ScimServiceProviderService extends AbstractService
{

  public ScimServiceProviderService(KeycloakSession keycloakSession)
  {
    super(keycloakSession);
  }

  /**
   * @return the default {@link ScimServiceProviderEntity} configuration for the current realm
   */
  protected ScimServiceProviderEntity getDefaultServiceProviderConfiguration()
  {
    return ScimServiceProviderEntity.builder()
                                    .realmId(getKeycloakSession().getContext().getRealm().getId())
                                    .enabled(true)
                                    .filterSupported(true)
                                    .filterMaxResults(50)
                                    .sortSupported(true)
                                    .patchSupported(true)
                                    .etagSupported(true)
                                    .changePasswordSupported(false)
                                    .bulkSupported(true)
                                    .bulkMaxOperations(15)
                                    .bulkMaxPayloadSize(2 * 1024 * 1024) // 2MB
                                    .created(Instant.now())
                                    .build();
  }

  /**
   * gets a service provider from the database or creates a default {@link ServiceProvider} configuration if no
   * entry does currently exist for the current realm
   *
   * @return an already existing service provider or a newly created default setup
   */
  public ServiceProvider getServiceProvider()
  {
    Optional<ScimServiceProviderEntity> scimServiceProviderEntityOptional = getServiceProviderEntity();
    ScimServiceProviderEntity scimServiceProviderEntity;
    scimServiceProviderEntity = scimServiceProviderEntityOptional.orElseGet(this::createServiceProviderEntity);
    return toScimRepresentation(scimServiceProviderEntity);
  }

  /**
   * updates the {@link ServiceProvider} representation for the current realm
   * 
   * @param serviceProvider the updated {@link ServiceProvider} representation
   */
  public ServiceProvider updateServiceProvider(ServiceProvider serviceProvider)
  {
    Optional<ScimServiceProviderEntity> scimServiceProviderEntityOptional = getServiceProviderEntity();
    ScimServiceProviderEntity scimServiceProviderEntity;
    scimServiceProviderEntity = scimServiceProviderEntityOptional.orElseGet(this::createServiceProviderEntity);

    scimServiceProviderEntity.setEnabled(Optional.ofNullable(serviceProvider.get("enabled"))
                                                 .map(JsonNode::booleanValue)
                                                 .orElse(true));
    scimServiceProviderEntity.setFilterSupported(serviceProvider.getFilterConfig().isSupported());
    scimServiceProviderEntity.setFilterMaxResults(serviceProvider.getFilterConfig().getMaxResults());
    scimServiceProviderEntity.setSortSupported(serviceProvider.getSortConfig().isSupported());
    scimServiceProviderEntity.setPatchSupported(serviceProvider.getPatchConfig().isSupported());
    scimServiceProviderEntity.setEtagSupported(serviceProvider.getETagConfig().isSupported());
    scimServiceProviderEntity.setChangePasswordSupported(serviceProvider.getChangePasswordConfig().isSupported());
    scimServiceProviderEntity.setBulkSupported(serviceProvider.getBulkConfig().isSupported());
    scimServiceProviderEntity.setBulkMaxOperations(serviceProvider.getBulkConfig().getMaxOperations());
    scimServiceProviderEntity.setBulkMaxPayloadSize(serviceProvider.getBulkConfig().getMaxPayloadSize());
    scimServiceProviderEntity.setLastModified(Instant.now());
    getEntityManager().flush();
    return toScimRepresentation(scimServiceProviderEntity);
  }

  /**
   * @return either an existing representation from the database or an empty
   */
  public Optional<ScimServiceProviderEntity> getServiceProviderEntity()
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    EntityManager entityManager = getEntityManager();
    ScimServiceProviderEntity scimServiceProvider;
    try
    {
      scimServiceProvider = entityManager.createNamedQuery("getScimServiceProvider", ScimServiceProviderEntity.class)
                                         .setParameter("realmId", realmModel.getId())
                                         .getSingleResult();
      return Optional.of(entityManager.merge(scimServiceProvider));
    }
    catch (NoResultException ex)
    {
      return Optional.empty();
    }
  }

  /**
   * @return a new service provider instance that was just stored within the database
   */
  private ScimServiceProviderEntity createServiceProviderEntity()
  {
    EntityManager entityManager = getEntityManager();
    ScimServiceProviderEntity scimServiceProvider = getDefaultServiceProviderConfiguration();
    entityManager.merge(scimServiceProvider);
    entityManager.flush();
    return scimServiceProvider;
  }

  /**
   * translate the database entity representation to the SCIM representation
   * 
   * @param entity the entity representation of a {@link ServiceProvider}
   */
  protected ServiceProvider toScimRepresentation(ScimServiceProviderEntity entity)
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .authenticationSchemes(getAuthenticationSchemes())
                                                     .filterConfig(FilterConfig.builder()
                                                                               .supported(entity.isFilterSupported())
                                                                               .maxResults(entity.getFilterMaxResults())
                                                                               .build())
                                                     .sortConfig(SortConfig.builder()
                                                                           .supported(entity.isSortSupported())
                                                                           .build())
                                                     .patchConfig(PatchConfig.builder()
                                                                             .supported(entity.isPatchSupported())
                                                                             .build())
                                                     .eTagConfig(ETagConfig.builder()
                                                                           .supported(entity.isEtagSupported())
                                                                           .build())
                                                     .changePasswordConfig(ChangePasswordConfig.builder()
                                                                                               .supported(entity.isChangePasswordSupported())
                                                                                               .build())
                                                     .bulkConfig(BulkConfig.builder()
                                                                           .supported(entity.isBulkSupported())
                                                                           .maxOperations(entity.getBulkMaxOperations())
                                                                           .maxPayloadSize(entity.getBulkMaxPayloadSize())
                                                                           .build())
                                                     .build();
    serviceProvider.getMeta().ifPresent(meta -> {
      // meta is definitely present
      meta.setCreated(entity.getCreated());
      meta.setLastModified(Optional.ofNullable(entity.getLastModified()).orElse(entity.getCreated()));
      meta.setVersion(ETag.builder().tag(String.valueOf(entity.getVersion())).build());
    });
    serviceProvider.set("enabled", BooleanNode.valueOf(entity.isEnabled()));
    return serviceProvider;
  }

  /**
   * TODO authentication schemes must be handled later if clients can ce assigned to the SCIM endpoints
   * 
   * @return the list of available authentication methods that may be used to authenticate at the SCIM endpoints
   */
  public List<AuthenticationScheme> getAuthenticationSchemes()
  {
    return Collections.singletonList(AuthenticationScheme.builder()
                                                         .name("OAuth Bearer Token")
                                                         .description("Authentication scheme using the OAuth "
                                                                      + "Bearer Token Standard")
                                                         .specUri("http://www.rfc-editor.org/info/rfc6750")
                                                         // http://www.iana.org/assignments/http-authschemes/http-authschemes.xhtml
                                                         .type("Bearer")
                                                         .build());
  }

  /**
   * deletes the provider for the current realm
   */
  public void deleteProvider()
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    getEntityManager().createNamedQuery("removeScimServiceProvider")
                      .setParameter("realmId", realmModel.getId())
                      .executeUpdate();
  }
}
