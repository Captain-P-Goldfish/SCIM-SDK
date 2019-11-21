package de.captaingoldfish.scim.sdk.common.resources.complex;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;


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

  public static MetaBuilder builder()
  {
    return new MetaBuilder(new Meta());
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
  public Optional<ETag> getVersion()
  {
    return getStringAttribute(AttributeNames.RFC7643.VERSION).map(ETag::newInstance);
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
  public void setVersion(ETag version)
  {
    if (version == null)
    {
      remove(AttributeNames.RFC7643.VERSION);
    }
    else
    {
      set(AttributeNames.RFC7643.VERSION, version);
    }
  }

  /**
   * a builder class that is not generated with lombok because of the multiple setter-methods for created and
   * last modified
   */
  public static class MetaBuilder
  {

    /**
     * the meta object that should be built
     */
    private Meta meta;

    public MetaBuilder(Meta meta)
    {
      this.meta = meta;
    }

    /**
     * The "DateTime" that the resource was added to the service provider. This attribute MUST be a DateTime.
     */
    public MetaBuilder created(Instant created)
    {
      meta.setCreated(created);
      return this;
    }

    /**
     * The "DateTime" that the resource was added to the service provider. This attribute MUST be a DateTime.
     */
    public MetaBuilder created(LocalDateTime created)
    {
      meta.setCreated(created);
      return this;
    }

    /**
     * The "DateTime" that the resource was added to the service provider. This attribute MUST be a DateTime.
     */
    public MetaBuilder created(OffsetDateTime created)
    {
      meta.setCreated(created.toInstant());
      return this;
    }

    /**
     * The most recent DateTime that the details of this resource were updated at the service provider. If this
     * resource has never been modified since its initial creation, the value MUST be the same as the value of
     * "created".
     */
    public MetaBuilder lastModified(Instant lastModified)
    {
      meta.setLastModified(lastModified);
      return this;
    }

    /**
     * The most recent DateTime that the details of this resource were updated at the service provider. If this
     * resource has never been modified since its initial creation, the value MUST be the same as the value of
     * "created".
     */
    public MetaBuilder lastModified(LocalDateTime lastModified)
    {
      meta.setLastModified(lastModified);
      return this;
    }

    /**
     * The most recent DateTime that the details of this resource were updated at the service provider. If this
     * resource has never been modified since its initial creation, the value MUST be the same as the value of
     * "created".
     */
    public MetaBuilder lastModified(OffsetDateTime lastModified)
    {
      meta.setLastModified(lastModified.toInstant());
      return this;
    }

    /**
     * The URI of the resource being returned. This value MUST be the same as the "Content-Location" HTTP response
     * header (see Section 3.1.4.2 of [RFC7231]).
     */
    public MetaBuilder location(String location)
    {
      meta.setLocation(location);
      return this;
    }

    /**
     * The name of the resource type of the resource. This attribute has a mutability of "readOnly" and
     * "caseExact" as "true".
     */
    public MetaBuilder resourceType(String resourceType)
    {
      meta.setResourceType(resourceType);
      return this;
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
    public MetaBuilder version(String version)
    {
      meta.setVersion(ETag.builder().weak(true).tag(version).build());
      return this;
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
    public MetaBuilder version(ETag version)
    {
      meta.setVersion(version);
      return this;
    }

    /**
     * returns the built meta-object
     */
    public Meta build()
    {
      return meta;
    }

  }

}
