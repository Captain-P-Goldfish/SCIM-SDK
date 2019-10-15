package de.gold.scim.resources;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.resources.base.ScimObjectNode;
import de.gold.scim.resources.complex.Meta;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 11:23 <br>
 * <br>
 * Each SCIM resource (Users, Groups, etc.) includes the following common attributes. With the exception of
 * the "ServiceProviderConfig" and "ResourceType" server discovery endpoints and their associated resources,
 * these attributes MUST be defined for all resources, including any extended resource types. When accepted by
 * a service provider (e.g., after a SCIM create), the attributes "id" and "meta" (and its associated
 * sub-attributes) MUST be assigned values by the service provider. Common attributes are considered to be
 * part of every base resource schema and do not use their own "schemas" URI. For backward compatibility, some
 * existing schema definitions MAY list common attributes as part of the schema. The attribute characteristics
 * (see Section 2.2) listed here SHALL take precedence over older definitions that may be included in existing
 * schemas.
 */
public abstract class ResourceNode extends ScimObjectNode
{

  public ResourceNode()
  {
    super(null);
  }

  /**
   * @return the list of schemas witin this resource
   */
  public List<String> getSchemas()
  {
    return getArrayAttribute(AttributeNames.SCHEMAS);
  }

  /**
   * adds a list of schemas to this resource
   */
  public void setSchemas(List<String> schemas)
  {
    setStringAttributeList(AttributeNames.SCHEMAS, schemas);
  }

  /**
   * A unique identifier for a SCIM resource as defined by the service provider. Each representation of the
   * resource MUST include a non-empty "id" value. This identifier MUST be unique across the SCIM service
   * provider's entire set of resources. It MUST be a stable, non-reassignable identifier that does not change
   * when the same resource is returned in subsequent requests. The value of the "id" attribute is always issued
   * by the service provider and MUST NOT be specified by the client. The string "bulkId" is a reserved keyword
   * and MUST NOT be used within any unique identifier value. The attribute characteristics are "caseExact" as
   * "true", a mutability of "readOnly", and a "returned" characteristic of "always". See Section 9 for
   * additional considerations regarding privacy.
   */
  public Optional<String> getId()
  {
    return getStringAttribute(AttributeNames.ID);
  }

  /**
   * A unique identifier for a SCIM resource as defined by the service provider. Each representation of the
   * resource MUST include a non-empty "id" value. This identifier MUST be unique across the SCIM service
   * provider's entire set of resources. It MUST be a stable, non-reassignable identifier that does not change
   * when the same resource is returned in subsequent requests. The value of the "id" attribute is always issued
   * by the service provider and MUST NOT be specified by the client. The string "bulkId" is a reserved keyword
   * and MUST NOT be used within any unique identifier value. The attribute characteristics are "caseExact" as
   * "true", a mutability of "readOnly", and a "returned" characteristic of "always". See Section 9 for
   * additional considerations regarding privacy.
   */
  public void setId(String id)
  {
    setAttribute(AttributeNames.ID, id);
  }

  /**
   * A String that is an identifier for the resource as defined by the provisioning client. The "externalId" may
   * simplify identification of a resource between the provisioning client and the service provider by allowing
   * the client to use a filter to locate the resource with an identifier from the provisioning domain,
   * obviating the need to store a local mapping between the provisioning domain's identifier of the resource
   * and the identifier used by the service provider. Each resource MAY include a non-empty "externalId" value.
   * The value of the "externalId" attribute is always issued by the provisioning client and MUST NOT be
   * specified by the service provider. The service provider MUST always interpret the externalId as scoped to
   * the provisioning domain. While the server does not enforce uniqueness, it is assumed that the value's
   * uniqueness is controlled by the client setting the value. See Section 9 for additional considerations
   * regarding privacy. This attribute has "caseExact" as "true" and a mutability of "readWrite". This attribute
   * is OPTIONAL.
   */
  public Optional<String> getExternalId()
  {
    return getStringAttribute(AttributeNames.EXTERNAL_ID);
  }

  /**
   * A String that is an identifier for the resource as defined by the provisioning client. The "externalId" may
   * simplify identification of a resource between the provisioning client and the service provider by allowing
   * the client to use a filter to locate the resource with an identifier from the provisioning domain,
   * obviating the need to store a local mapping between the provisioning domain's identifier of the resource
   * and the identifier used by the service provider. Each resource MAY include a non-empty "externalId" value.
   * The value of the "externalId" attribute is always issued by the provisioning client and MUST NOT be
   * specified by the service provider. The service provider MUST always interpret the externalId as scoped to
   * the provisioning domain. While the server does not enforce uniqueness, it is assumed that the value's
   * uniqueness is controlled by the client setting the value. See Section 9 for additional considerations
   * regarding privacy. This attribute has "caseExact" as "true" and a mutability of "readWrite". This attribute
   * is OPTIONAL.
   */
  public void setExternalId(String externalId)
  {
    setAttribute(AttributeNames.EXTERNAL_ID, externalId);
  }

  /**
   * A complex attribute containing resource metadata. All "meta" sub-attributes are assigned by the service
   * provider (have a "mutability" of "readOnly"), and all of these sub-attributes have a "returned"
   * characteristic of "default". This attribute SHALL be ignored when provided by clients. "meta" contains the
   * following sub-attributes:
   */
  public Optional<Meta> getMeta()
  {
    return getObjectAttribute(AttributeNames.META, Meta.class);
  }

  /**
   * A complex attribute containing resource metadata. All "meta" sub-attributes are assigned by the service
   * provider (have a "mutability" of "readOnly"), and all of these sub-attributes have a "returned"
   * characteristic of "default". This attribute SHALL be ignored when provided by clients. "meta" contains the
   * following sub-attributes:
   */
  public void setMeta(ObjectNode objectNode)
  {
    setAttribute(AttributeNames.META, objectNode);
  }
}
