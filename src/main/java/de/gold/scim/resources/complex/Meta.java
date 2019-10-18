package de.gold.scim.resources.complex;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 10:19 <br>
 * <br>
 * A complex attribute containing resource metadata. All "meta" sub-attributes are assigned by the service
 * provider (have a "mutability" of "readOnly"), and all of these sub-attributes have a "returned"
 * characteristic of "default". This attribute SHALL be ignored when provided by clients. "meta" contains the
 * following sub-attributes:
 */
public class Meta extends ScimObjectNode
{

  public Meta()
  {
    super(null);
  }

  @Builder
  public Meta(String resourceType, LocalDateTime created, LocalDateTime lastModified, String location, String version)
  {
    this();
    setResourceType(resourceType);
    setCreated(created);
    setLastModified(lastModified);
    setLocation(location);
    setVersion(version);
  }

  /**
   * The name of the resource type of the resource. This attribute has a mutability of "readOnly" and
   * "caseExact" as "true".
   */
  public Optional<String> getResourceType()
  {
    return getStringAttribute(AttributeNames.RFC7643.RESOURCE_TYPE);
  }

  /**
   * The name of the resource type of the resource. This attribute has a mutability of "readOnly" and
   * "caseExact" as "true".
   */
  public void setResourceType(String resourceType)
  {
    setAttribute(AttributeNames.RFC7643.RESOURCE_TYPE, resourceType);
  }

  /**
   * The "DateTime" that the resource was added to the service provider. This attribute MUST be a DateTime.
   */
  public Optional<Instant> getCreated()
  {
    return getDateTimeAttribute(AttributeNames.RFC7643.CREATED);
  }

  /**
   * The "DateTime" that the resource was added to the service provider. This attribute MUST be a DateTime.
   */
  public void setCreated(String dateTime)
  {

    setAttribute(AttributeNames.RFC7643.CREATED, dateTime);
  }

  /**
   * The "DateTime" that the resource was added to the service provider. This attribute MUST be a DateTime.
   */
  public void setCreated(OffsetDateTime dateTime)
  {
    setDateTimeAttribute(AttributeNames.RFC7643.CREATED, dateTime);
  }

  /**
   * The "DateTime" that the resource was added to the service provider. This attribute MUST be a DateTime.
   */
  public void setCreated(LocalDateTime dateTime)
  {
    setDateTimeAttribute(AttributeNames.RFC7643.CREATED, dateTime);
  }

  /**
   * The "DateTime" that the resource was added to the service provider. This attribute MUST be a DateTime.
   */
  public void setCreated(Instant dateTime)
  {
    setDateTimeAttribute(AttributeNames.RFC7643.CREATED, dateTime);
  }

  /**
   * The most recent DateTime that the details of this resource were updated at the service provider. If this
   * resource has never been modified since its initial creation, the value MUST be the same as the value of
   * "created".
   */
  public Optional<Instant> getLastModified()
  {
    return getDateTimeAttribute(AttributeNames.RFC7643.LAST_MODIFIED);
  }

  /**
   * The most recent DateTime that the details of this resource were updated at the service provider. If this
   * resource has never been modified since its initial creation, the value MUST be the same as the value of
   * "created".
   */
  public void setLastModified(String dateTime)
  {
    setAttribute(AttributeNames.RFC7643.LAST_MODIFIED, dateTime);
  }

  /**
   * The most recent DateTime that the details of this resource were updated at the service provider. If this
   * resource has never been modified since its initial creation, the value MUST be the same as the value of
   * "created".
   */
  public void setLastModified(Instant dateTime)
  {
    setDateTimeAttribute(AttributeNames.RFC7643.LAST_MODIFIED, dateTime);
  }

  /**
   * The most recent DateTime that the details of this resource were updated at the service provider. If this
   * resource has never been modified since its initial creation, the value MUST be the same as the value of
   * "created".
   */
  public void setLastModified(OffsetDateTime dateTime)
  {
    setDateTimeAttribute(AttributeNames.RFC7643.LAST_MODIFIED, dateTime);
  }

  /**
   * The most recent DateTime that the details of this resource were updated at the service provider. If this
   * resource has never been modified since its initial creation, the value MUST be the same as the value of
   * "created".
   */
  public void setLastModified(LocalDateTime dateTime)
  {
    setDateTimeAttribute(AttributeNames.RFC7643.LAST_MODIFIED, dateTime);
  }

  /**
   * The URI of the resource being returned. This value MUST be the same as the "Content-Location" HTTP response
   * header (see Section 3.1.4.2 of [RFC7231]).
   */
  public Optional<String> getLocation()
  {
    return getStringAttribute(AttributeNames.RFC7643.LOCATION);
  }

  /**
   * The URI of the resource being returned. This value MUST be the same as the "Content-Location" HTTP response
   * header (see Section 3.1.4.2 of [RFC7231]).
   */
  public void setLocation(String location)
  {
    setAttribute(AttributeNames.RFC7643.LOCATION, location);
  }

  /**
   * The version of the resource being returned. This value must be the same as the entity-tag (ETag) HTTP
   * response header (see Sections 2.1 and 2.3 of [RFC7232]). This attribute has "caseExact" as "true". Service
   * provider support for this attribute is optional and subject to the service provider's support for
   * versioning (see Section 3.14 of [RFC7644]). If a service provider provides "version" (entity-tag) for a
   * representation and the generation of that entity-tag does not satisfy all of the characteristics of a
   * strong validator (see Section 2.1 of [RFC7232]), then the origin server MUST mark the "version"
   * (entity-tag) as weak by prefixing its opaque value with "W/" (case sensitive).
   */
  public Optional<String> getVersion()
  {
    return getStringAttribute(AttributeNames.RFC7643.VERSION);
  }

  /**
   * The version of the resource being returned. This value must be the same as the entity-tag (ETag) HTTP
   * response header (see Sections 2.1 and 2.3 of [RFC7232]). This attribute has "caseExact" as "true". Service
   * provider support for this attribute is optional and subject to the service provider's support for
   * versioning (see Section 3.14 of [RFC7644]). If a service provider provides "version" (entity-tag) for a
   * representation and the generation of that entity-tag does not satisfy all of the characteristics of a
   * strong validator (see Section 2.1 of [RFC7232]), then the origin server MUST mark the "version"
   * (entity-tag) as weak by prefixing its opaque value with "W/" (case sensitive).
   */
  public void setVersion(String version)
  {
    setAttribute(AttributeNames.RFC7643.VERSION, version);
  }

}
