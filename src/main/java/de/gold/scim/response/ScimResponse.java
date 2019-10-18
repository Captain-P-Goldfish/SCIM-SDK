package de.gold.scim.response;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.constants.HttpHeader;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:29 <br>
 * <br>
 * The abstract implementation for all responses created by this framework
 */
public abstract class ScimResponse
{

  /**
   * contains the http header attributes for the response
   */
  @Getter
  private Map<String, String> httpHeaders;

  public ScimResponse()
  {
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
    final String responseDocument = toJsonDocument();
    if (StringUtils.isNotBlank(responseDocument))
    {
      responseBuilder.entity(responseDocument);
    }
    return responseBuilder.build();
  }

  /**
   * the http status code of the response
   */
  public abstract int getHttpStatus();

  /**
   * will translate this response into a valid json document
   */
  public abstract String toJsonDocument();
}
