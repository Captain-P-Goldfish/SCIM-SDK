package de.captaingoldfish.scim.sdk.common.response;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.resources.AbstractSchemasHolder;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:29 <br>
 * <br>
 * The abstract implementation for all responses created by this framework
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ScimResponse extends AbstractSchemasHolder
{

  /**
   * contains the http header attributes for the response
   */
  @Getter
  private Map<String, String> httpHeaders = new HashMap<>();

  public ScimResponse(JsonNode responseNode)
  {
    super();
    if (responseNode != null)
    {
      responseNode.fields().forEachRemaining(childNode -> {
        this.set(childNode.getKey(), childNode.getValue());
      });
    }
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
  }

  /**
   * will set the entity tag into the response headers
   *
   * @param meta the meta attribute that might contain a version-attribute which is the entity tag
   */
  protected void setETag(Meta meta)
  {
    if (meta != null && meta.getVersion().isPresent())
    {
      getHttpHeaders().put(HttpHeader.E_TAG_HEADER, meta.getVersion().get().getEntityTag());
    }
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
    if (this.size() != 0)
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
