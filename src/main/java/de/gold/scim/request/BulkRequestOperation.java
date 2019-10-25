package de.gold.scim.request;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.resources.base.ScimObjectNode;
import de.gold.scim.utils.JsonHelper;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 21:12 <br>
 * <br>
 * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
 * resource endpoint. REQUIRED.
 */
public class BulkRequestOperation extends ScimObjectNode
{

  public BulkRequestOperation()
  {
    super(null);
  }

  @Builder
  public BulkRequestOperation(String method, String bulkId, String path, String data)
  {
    this();
    setMethod(method);
    setBulkId(bulkId);
    setPath(path);
    setData(data);
  }

  /**
   * The HTTP method of the current operation. Possible values are "POST", "PUT", "PATCH", or "DELETE".
   * REQUIRED.
   */
  public String getMethod()
  {
    return getStringAttribute(AttributeNames.RFC7643.METHOD).orElseThrow(() -> {
      return new BadRequestException("the 'method' attribute is mandatory", null, null);
    });
  }

  /**
   * The HTTP method of the current operation. Possible values are "POST", "PUT", "PATCH", or "DELETE".
   * REQUIRED.
   */
  public void setMethod(String method)
  {
    setAttribute(AttributeNames.RFC7643.METHOD, method);
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
  public String getData()
  {
    return Optional.ofNullable(get(AttributeNames.RFC7643.DATA)).map(JsonNode::toString).orElseThrow(() -> {
      return new BadRequestException("the attribute 'data' is mandatory", null, null);
    });
  }

  /**
   * The resource data as it would appear for a single SCIM POST, PUT, or PATCH operation. REQUIRED in a request
   * when "method" is "POST", "PUT", or "PATCH".
   */
  public void setData(String data)
  {
    set(AttributeNames.RFC7643.DATA, JsonHelper.readJsonDocument(data));
  }
}
