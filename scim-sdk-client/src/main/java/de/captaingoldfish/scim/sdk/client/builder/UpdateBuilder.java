package de.captaingoldfish.scim.sdk.client.builder;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.response.UpdateResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 12:04 <br>
 * <br>
 */
public class UpdateBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
{

  /**
   * the resource id that should be updated
   */
  private String id;

  public UpdateBuilder(String baseUrl, ScimClientConfig scimClientConfig, Class<T> responseEntityType)
  {
    super(baseUrl, scimClientConfig, responseEntityType);
  }

  /**
   * @param id sets the resource id of the resource that should be updated on the server
   */
  public UpdateBuilder<T> setId(String id)
  {
    if (StringUtils.isBlank(id))
    {
      throw new IllegalStateException("id must not be blank for update-requests");
    }
    this.id = id;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpdateBuilder<T> setResource(String resource)
  {
    return (UpdateBuilder<T>)super.setResource(resource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpdateBuilder<T> setResource(JsonNode resource)
  {
    return (UpdateBuilder<T>)super.setResource(resource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpdateBuilder<T> setEndpoint(String endpoint)
  {
    return (UpdateBuilder<T>)super.setEndpoint(endpoint);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpdateBuilder<T> setETagForIfMatch(String version)
  {
    return (UpdateBuilder<T>)super.setETagForIfMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpdateBuilder<T> setETagForIfNoneMatch(String version)
  {
    return (UpdateBuilder<T>)super.setETagForIfNoneMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpdateBuilder<T> setETagForIfMatch(ETag version)
  {
    return (UpdateBuilder<T>)super.setETagForIfMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpdateBuilder<T> setETagForIfNoneMatch(ETag version)
  {
    return (UpdateBuilder<T>)super.setETagForIfNoneMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected <T1 extends ScimResponse> T1 buildScimResponse(int httpResponseCode, String responseBody)
  {
    Class<T1> type = HttpStatus.OK == httpResponseCode ? (Class<T1>)UpdateResponse.class
      : (Class<T1>)ErrorResponse.class;
    return JsonHelper.readJsonDocument(responseBody, type);
  }

  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    if (StringUtils.isBlank(id))
    {
      throw new IllegalStateException("id must not be blank for get-requests");
    }
    HttpPut httpPut = new HttpPut(getBaseUrl() + getEndpoint() + "/" + id);
    if (StringUtils.isBlank(getResource()))
    {
      throw new IllegalArgumentException("resource for delete request must not be empty");
    }
    StringEntity stringEntity = new StringEntity(getResource(), StandardCharsets.UTF_8);
    httpPut.setEntity(stringEntity);
    if (isUseIfMatch())
    {
      httpPut.setHeader(HttpHeader.IF_MATCH_HEADER, getVersion().toString());
    }
    if (isUseIfNoneMatch())
    {
      httpPut.setHeader(HttpHeader.IF_NONE_MATCH_HEADER, getVersion().toString());
    }
    return httpPut;
  }
}
