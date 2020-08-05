package de.captaingoldfish.scim.sdk.keycloak.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
@Table(name = "SCIM_SCHEMA_EXTENSIONS")
public class ScimSchemaExtensionEntity
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
   * the owner of this entry
   */
  @ManyToOne
  @JoinColumn(name = "SCIM_RESOURCE_TYPE_ID", referencedColumnName = "ID")
  private ScimResourceTypeEntity scimResourceTypeEntity;

  /**
   * the schema extension uri reference
   */
  @Column(name = "SCHEMA")
  private String schema;

  /**
   * if the extension is required or not
   */
  @Column(name = "REQUIRED")
  private boolean required;

  @Builder
  public ScimSchemaExtensionEntity(ScimResourceTypeEntity scimResourceTypeEntity, String schema, boolean required)
  {
    this.scimResourceTypeEntity = scimResourceTypeEntity;
    this.schema = schema;
    this.required = required;
  }
}
