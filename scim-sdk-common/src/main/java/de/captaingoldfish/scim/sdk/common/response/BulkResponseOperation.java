package de.captaingoldfish.scim.sdk.common.response;

import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 21:12 <br>
 * <br>
 * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
 * resource endpoint. REQUIRED.
 */
public class BulkResponseOperation extends ScimObjectNode
{

  public BulkResponseOperation()
  {
    super(null);
  }

  @Builder
  public BulkResponseOperation(HttpMethod method,
                               String bulkId,
                               String resourceId,
                               ETag version,
                               String location,
                               Integer status,
                               ScimObjectNode response)
  {
    this();
    setMethod(method);
    setBulkId(bulkId);
    setResourceId(resourceId);
    setVersion(version);
    setLocation(location);
    setStatus(status);
    setResponse(response);
  }

  /**
   * The HTTP method of the current operation. Possible values are "POST", "PUT", "PATCH", or "DELETE".
   * REQUIRED.
   */
  public HttpMethod getMethod()
  {
    return getStringAttribute(AttributeNames.RFC7643.METHOD).map(HttpMethod::valueOf).orElseThrow(() -> {
      return new InternalServerException("the 'method' attribute is mandatory", null, null);
    });
  }

  /**
   * The HTTP method of the current operation. Possible values are "POST", "PUT", "PATCH", or "DELETE".
   * REQUIRED.
   */
  public void setMethod(HttpMethod method)
  {
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
   * the unique identifier of the resource that is added to the response in order for the client to simplify the
   * parsing of the id element. This is a custom field not defined by SCIM.
   */
  public Optional<String> getResourceId()
  {
    return getStringAttribute(AttributeNames.RFC7643.ID);
  }

  /**
   * the unique identifier of the resource that is added to the response in order for the client to simplify the
   * parsing of the id element. This is a custom field not defined by SCIM.
   */
  public void setResourceId(String resourceId)
  {
    setAttribute(AttributeNames.RFC7643.ID, resourceId);
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
   * The resource endpoint URL. REQUIRED in a response, except in the event of a POST failure.
   */
  public Optional<String> getLocation()
  {
    return getStringAttribute(AttributeNames.RFC7643.LOCATION);
  }

  /**
   * The resource endpoint URL. REQUIRED in a response, except in the event of a POST failure.
   */
  public void setLocation(String location)
  {
    setAttribute(AttributeNames.RFC7643.LOCATION, location);
  }

  /**
   * The HTTP response status code for the requested operation. When indicating an error, the "response"
   * attribute MUST contain the detail error response as per Section 3.12.
   */
  public Integer getStatus()
  {
    return getLongAttribute(AttributeNames.RFC7643.STATUS).map(Long::intValue).orElseThrow(() -> {
      return new InternalServerException("attribute 'status' is mandatory", null, null);
    });
  }

  /**
   * The HTTP response status code for the requested operation. When indicating an error, the "response"
   * attribute MUST contain the detail error response as per Section 3.12.
   */
  public void setStatus(Integer status)
  {
    setAttribute(AttributeNames.RFC7643.STATUS, status == null ? null : status.longValue());
  }


  /**
   * The HTTP response body for the specified request operation. When indicating a response with an HTTP status
   * other than a 200-series response, the response body MUST be included. For normal completion, the server MAY
   * elect to omit the response body.
   */
  public Optional<ScimObjectNode> getResponse()
  {
    return getStringAttribute(AttributeNames.RFC7643.RESPONSE).map(value -> {
      return JsonHelper.readJsonDocument(value, ScimObjectNode.class);
    });
  }

  /**
   * The HTTP response body for the specified request operation. When indicating a response with an HTTP status
   * other than a 200-series response, the response body MUST be included. For normal completion, the server MAY
   * elect to omit the response body.
   */
  public void setResponse(ScimObjectNode response)
  {
    setAttribute(AttributeNames.RFC7643.RESPONSE, response);
  }

  /**
   * The HTTP response body for the specified request operation. When indicating a response with an HTTP status
   * other than a 200-series response, the response body MUST be included. For normal completion, the server MAY
   * elect to omit the response body.
   */
  public <T extends ScimObjectNode> Optional<T> getResponse(Class<T> type)
  {
    if (ErrorResponse.class.isAssignableFrom(type))
    {
      return (Optional<T>)Optional.ofNullable(get(AttributeNames.RFC7643.RESPONSE)).map(ErrorResponse::new);
    }
    return Optional.ofNullable(get(AttributeNames.RFC7643.RESPONSE))
                   .map(value -> JsonHelper.copyResourceToObject(value, type));
  }


  /**
   * override lombok builder with public constructor
   */
  public static class BulkResponseOperationBuilder
  {

    public BulkResponseOperationBuilder()
    {}
  }
}
