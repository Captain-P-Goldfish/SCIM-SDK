package de.gold.scim.common.response;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.common.constants.HttpHeader;
import de.gold.scim.common.resources.AbstractSchemasHolder;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:29 <br>
 * <br>
 * The abstract implementation for all responses created by this framework
 */
public abstract class ScimResponse extends AbstractSchemasHolder
{

  /**
   * contains the http header attributes for the response
   */
  @Getter
  private Map<String, String> httpHeaders;

  public ScimResponse(JsonNode responseNode)
  {
    super();
    if (responseNode != null)
    {
      responseNode.fields().forEachRemaining(childNode -> {
        this.set(childNode.getKey(), childNode.getValue());
      });
    }
    this.httpHeaders = new HashMap<>();
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
  }

  /**
   * builds a response object that should be usable with most of the common rest apis
   *
   * @return a jax-rs response containing the response body and the http headers
   */
  public Response buildResponse()
  {
    Response.ResponseBuilder responseBuilder = Response.status(getHttpStatus());
    httpHeaders.forEach(responseBuilder::header);
    if (!this.isEmpty())
    {
      responseBuilder.entity(toString());
    }
    return responseBuilder.build();
  }

  /**
   * the http status code of the response
   */
  public abstract int getHttpStatus();

}
