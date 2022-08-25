package de.captaingoldfish.scim.sdk.common.request;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 21:12 <br>
 * <br>
 * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
 * resource endpoint. REQUIRED.
 */
public class BulkRequestOperation extends ScimObjectNode
{

  /**
   * these are the only http methods allowed by bulk
   */
  protected final static List<HttpMethod> VALID_METHODS = Arrays.asList(HttpMethod.POST,
                                                                        HttpMethod.PUT,
                                                                        HttpMethod.PATCH,
                                                                        HttpMethod.DELETE);

  /**
   * this identifier is exclusively used on bulk requests to be able to check if this request was already tried
   * to be handled once. If this happens and the second try fails too, this operation is marked as failure
   * within the bulk response. This is necessary in order to find bulkId references that cannot be resolved.
   */
  @Getter
  @Setter
  public String uniqueIdentifier;

  public BulkRequestOperation()
  {
    super(null);
  }

  @Builder
  public BulkRequestOperation(HttpMethod method,
                              String bulkId,
                              String path,
                              String data,
                              ETag version,
                              Boolean returnResource)
  {
    this();
    setMethod(method);
    setBulkId(bulkId);
    setPath(path);
    setData(data);
    setVersion(version);
    setReturnResource(returnResource);
  }

  /**
   * The HTTP method of the current operation. Possible values are "POST", "PUT", "PATCH", or "DELETE".
   * REQUIRED.
   */
  public HttpMethod getMethod()
  {
    return getStringAttribute(AttributeNames.RFC7643.METHOD).map(HttpMethod::valueOf).orElseThrow(() -> {
      return new BadRequestException("the 'method' attribute is mandatory", null, ScimType.Custom.INVALID_PARAMETERS);
    });
  }

  /**
   * The HTTP method of the current operation. Possible values are "POST", "PUT", "PATCH", or "DELETE".
   * REQUIRED.
   */
  public void setMethod(HttpMethod method)
  {
    if (method != null && !VALID_METHODS.contains(method))
    {
      throw new BadRequestException("bulk does only support the following methods '" + VALID_METHODS
                                    + "' but found method: " + method, null, ScimType.Custom.INVALID_PARAMETERS);
    }
    setAttribute(AttributeNames.RFC7643.METHOD, method == null ? null : method.name());
  }

  /**
   * The transient identifier of a newly created resource, unique within a bulk request and created by the
   * client. The bulkId serves as a surrogate resource id enabling clients to uniquely identify newly created
   * resources in the response and cross-reference new resources in and across operations within a bulk request.
   * REQUIRED when "method" is "POST".
   */
  public Optional<String> getBulkId()
  {
    return getStringAttribute(AttributeNames.RFC7643.BULK_ID);
  }

  /**
   * The transient identifier of a newly created resource, unique within a bulk request and created by the
   * client. The bulkId serves as a surrogate resource id enabling clients to uniquely identify newly created
   * resources in the response and cross-reference new resources in and across operations within a bulk request.
   * REQUIRED when "method" is "POST".
   */
  public void setBulkId(String bulkId)
  {
    setAttribute(AttributeNames.RFC7643.BULK_ID, bulkId);
  }

  /**
   * The resource's relative path to the SCIM service provider's root. If "method" is "POST", the value must
   * specify a resource type endpoint, e.g., /Users or /Groups, whereas all other "method" values must specify
   * the path to a specific resource, e.g., /Users/2819c223-7f76-453a-919d-413861904646. REQUIRED in a request.
   */
  public String getPath()
  {
    return getStringAttribute(AttributeNames.RFC7643.PATH).orElseThrow(() -> {
      return new BadRequestException("the 'path' attribute is mandatory", null, null);
    });
  }

  /**
   * The resource's relative path to the SCIM service provider's root. If "method" is "POST", the value must
   * specify a resource type endpoint, e.g., /Users or /Groups, whereas all other "method" values must specify
   * the path to a specific resource, e.g., /Users/2819c223-7f76-453a-919d-413861904646. REQUIRED in a request.
   */
  public void setPath(String path)
  {
    setAttribute(AttributeNames.RFC7643.PATH, path);
  }

  /**
   * The resource data as it would appear for a single SCIM POST, PUT, or PATCH operation. REQUIRED in a request
   * when "method" is "POST", "PUT", or "PATCH".
   */
  public Optional<String> getData()
  {
    return Optional.ofNullable(get(AttributeNames.RFC7643.DATA))
                   .map(jsonNode -> jsonNode.isTextual() ? jsonNode.textValue() : jsonNode.toString());
  }

  /**
   * The resource data as it would appear for a single SCIM POST, PUT, or PATCH operation. REQUIRED in a request
   * when "method" is "POST", "PUT", or "PATCH".
   */
  public void setData(String data)
  {
    if (StringUtils.isBlank(data))
    {
      return;
    }
    set(AttributeNames.RFC7643.DATA, JsonHelper.readJsonDocument(data));
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
    return getStringAttribute(AttributeNames.RFC7643.VERSION, ETag.class);
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
    setVersion(ETag.builder().weak(true).tag(version).build());
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
   * this field allows clients to explicitly ask to return the created or modified resource in the bulk
   * response.
   */
  public Optional<Boolean> isReturnResource()
  {
    return getBooleanAttribute(AttributeNames.Custom.RETURN_RESOURCE);
  }

  /**
   * this field allows clients to explicitly ask to return the created or modified resource in the bulk
   * response.
   */
  public void setReturnResource(Boolean returnResource)
  {
    setAttribute(AttributeNames.Custom.RETURN_RESOURCE, returnResource);
  }
}
