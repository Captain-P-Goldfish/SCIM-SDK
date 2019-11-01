package de.gold.scim.server.response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.gold.scim.server.constants.AttributeNames;
import de.gold.scim.server.constants.ScimType;
import de.gold.scim.server.constants.enums.HttpMethod;
import de.gold.scim.server.exceptions.BadRequestException;
import de.gold.scim.server.exceptions.InternalServerException;
import de.gold.scim.server.resources.base.ScimObjectNode;
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

  /**
   * these are the only http methods allowed by bulk
   */
  protected final static List<HttpMethod> VALID_METHODS = Arrays.asList(HttpMethod.POST,
                                                                        HttpMethod.PUT,
                                                                        HttpMethod.PATCH,
                                                                        HttpMethod.DELETE);

  public BulkResponseOperation()
  {
    super(null);
  }

  @Builder
  public BulkResponseOperation(HttpMethod method,
                               String bulkId,
                               String version,
                               String location,
                               Integer status,
                               ErrorResponse response)
  {
    this();
    setMethod(method);
    setBulkId(bulkId);
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
   * The current resource version. Version MAY be used if the service provider supports entity-tags (ETags)
   * (Section 2.3 of [RFC7232]) and "method" is "PUT", "PATCH", or "DELETE".
   */
  public Optional<String> getVersion()
  {
    return getStringAttribute(AttributeNames.RFC7643.VERSION);
  }

  /**
   * The current resource version. Version MAY be used if the service provider supports entity-tags (ETags)
   * (Section 2.3 of [RFC7232]) and "method" is "PUT", "PATCH", or "DELETE".
   */
  public void setVersion(String version)
  {
    setAttribute(AttributeNames.RFC7643.VERSION, version);
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
  public Optional<ErrorResponse> getResponse()
  {
    return Optional.ofNullable(get(AttributeNames.RFC7643.RESPONSE)).map(ErrorResponse::new);
  }

  /**
   * The HTTP response body for the specified request operation. When indicating a response with an HTTP status
   * other than a 200-series response, the response body MUST be included. For normal completion, the server MAY
   * elect to omit the response body.
   */
  public void setResponse(ErrorResponse response)
  {
    setAttribute(AttributeNames.RFC7643.RESPONSE, response);
  }

}
