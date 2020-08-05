package de.captaingoldfish.scim.sdk.keycloak.entities;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.keycloak.models.utils.KeycloakModelUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "SCIM_RESOURCE_TYPE")
public class ScimResourceTypeEntity
{

  /**
   * primary key
   */
  @Id
  @Column(name = "ID")
  @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity. This avoids an extra
                               // SQL
  @Setter(AccessLevel.PROTECTED)
  private String id = KeycloakModelUtils.generateId();

  /**
   * JPA version column that is used as ETag on SCIM
   */
  @Version
  @Column(name = "VERSION")
  private long version;

  /**
   * id of the owning realm<br>
   * <b>NOTE:</b><br>
   * this column wasn't set as foreign key to make realm deletion easier
   */
  @Column(name = "REALM_ID")
  private String realmId;

  /**
   * the ID attribute of the resource type json structure
   */
  @Column(name = "RESOURCE_TYPE_ID")
  private String resourceTypeId;

  /**
   * the NAME attribute is actually the main attribute for resource types not the ID attribute
   */
  @Column(name = "NAME")
  private String name;

  /**
   * a simple human readable description for this resource type
   */
  @Column(name = "DESCRIPTION")
  private String description;

  /**
   * the main schema that represents this resource type
   */
  @Column(name = "SCHEMA")
  private String schema;

  /**
   * the endpoint path under which this resourceType is reachable. Only a single path part is allowed <br>
   * <b>/Users</b>: correct <br>
   * <b>/Users/Resource</b>: error
   */
  @Column(name = "ENDPOINT")
  private String endpoint;

  /**
   * the schema extensions of this resource type
   */
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "scimResourceTypeEntity")
  private List<ScimSchemaExtensionEntity> schemaExtensions;

  /**
   * if this resource type is enabled or disabled
   */
  @Column(name = "ENABLED")
  private boolean enabled = true;

  /**
   * makes this resource type to singleton endpoint
   */
  @Column(name = "SINGLETON_ENDPOINT")
  private boolean singletonEndpoint;

  /**
   * activates the auto filtering feature of the SCIM-SDK
   */
  @Column(name = "AUTO_FILTERING")
  private boolean autoFiltering;

  /**
   * activates the auto sorting feature of the SCIM-SDK
   */
  @Column(name = "AUTO_SORTING")
  private boolean autoSorting;

  /**
   * disables creation for resources of this resource type
   */
  @Column(name = "DISABLE_CREATE")
  private boolean disableCreate;

  /**
   * disables reading of resources for this resource type
   */
  @Column(name = "DISABLE_GET")
  private boolean disableGet;

  /**
   * disables update and patch for resources of this resource type
   */
  @Column(name = "DISABLE_UPDATE")
  private boolean disableUpdate;

  /**
   * disables deletion for resources of this resource type
   */
  @Column(name = "DISABLE_DELETE")
  private boolean disableDelete;

  /**
   * if access to this resource type requires authentication
   */
  @Column(name = "REQUIRE_AUTHENTICATION")
  private boolean requireAuthentication = true;

  /**
   * when this resource type was created
   */
  @Column(name = "CREATED")
  private Instant created;

  /**
   * when this resource type was last modified
   */
  @Column(name = "LAST_MODIFIED")
  private Instant lastModified;

  @Builder
  public ScimResourceTypeEntity(String realmId,
                                String resourceTypeId,
                                String name,
                                String description,
                                String schema,
                                String endpoint,
                                List<ScimSchemaExtensionEntity> schemaExtensions,
                                Boolean enabled,
                                boolean singletonEndpoint,
                                boolean autoFiltering,
                                boolean autoSorting,
                                boolean disableCreate,
                                boolean disableGet,
                                boolean disableUpdate,
                                boolean disableDelete,
                                Boolean requireAuthentication,
                                Instant created,
                                Instant lastModified)
  {
    this.realmId = realmId;
    this.resourceTypeId = resourceTypeId;
    this.name = name;
    this.description = description;
    this.schema = schema;
    this.endpoint = endpoint;
    this.schemaExtensions = schemaExtensions;
    this.enabled = Optional.ofNullable(enabled).orElse(true);
    this.singletonEndpoint = singletonEndpoint;
    this.autoFiltering = autoFiltering;
    this.autoSorting = autoSorting;
    this.disableCreate = disableCreate;
    this.disableGet = disableGet;
    this.disableUpdate = disableUpdate;
    this.disableDelete = disableDelete;
    this.requireAuthentication = Optional.ofNullable(requireAuthentication).orElse(true);
    this.created = created;
    this.lastModified = lastModified;
  }
}
