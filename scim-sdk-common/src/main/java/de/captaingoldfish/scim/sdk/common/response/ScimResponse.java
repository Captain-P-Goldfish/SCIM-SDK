package de.captaingoldfish.scim.sdk.common.response;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import tools.jackson.databind.JsonNode;

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
      responseNode.properties().forEach(childNode -> {
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
   * @deprecated since 1.32.0. This method will be removed in version 1.33.0. Use
   *             {@link #buildJakartaResponse()} instead.
   */
  @Deprecated
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
   * builds a response object that should be usable with most of the common rest apis
   *
   * @return a jakarta-rs response containing the response body and the http headers
   * @deprecated since 1.32.0. This method will be removed in version 1.34.0. Will be renamed to
   *             {@link #buildResponse()} in version 1.33.0.
   */
  @Deprecated
  public jakarta.ws.rs.core.Response buildJakartaResponse()
  {
    jakarta.ws.rs.core.Response.ResponseBuilder responseBuilder = jakarta.ws.rs.core.Response.status(getHttpStatus());
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
