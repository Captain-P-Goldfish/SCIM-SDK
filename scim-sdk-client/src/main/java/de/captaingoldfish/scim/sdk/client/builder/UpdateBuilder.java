package de.captaingoldfish.scim.sdk.client.builder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.utils.EncodingUtils;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 12:04 <br>
 * <br>
 */
public class UpdateBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
{

  /**
   * the fully qualified url to the required resource
   */
  private final String fullUrl;

  /**
   * if the resource should be retrieved by using the fully qualified url
   *
   * @param fullUrl the fully qualified url to the required resource
   * @param responseEntityType the type of the resource that should be returned
   * @param scimHttpClient the http client instance
   */
  public UpdateBuilder(String fullUrl, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
  {
    super(responseEntityType, scimHttpClient);
    this.fullUrl = fullUrl;
  }

  public UpdateBuilder(String baseUrl,
                       String endpoint,
                       String resourceId,
                       Class<T> responseEntityType,
                       ScimHttpClient scimHttpClient)
  {
    super(baseUrl, endpoint + (StringUtils.isBlank(resourceId) ? "" : "/" + EncodingUtils.urlEncode(resourceId)),
          responseEntityType, scimHttpClient);
    this.fullUrl = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpdateBuilder<T> setExpectedResponseHeaders(Map<String, String> requiredResponseHeaders)
  {
    return (UpdateBuilder<T>)super.setExpectedResponseHeaders(requiredResponseHeaders);
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
  protected boolean isExpectedResponseCode(int httpStatus)
  {
    return HttpStatus.OK == httpStatus;
  }

  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    HttpPut httpPut;
    if (StringUtils.isBlank(fullUrl))
    {
      httpPut = new HttpPut(getBaseUrl() + getEndpoint());
    }
    else
    {
      httpPut = new HttpPut(fullUrl);
    }
    if (StringUtils.isBlank(getResource()))
    {
      throw new IllegalArgumentException("resource for update request must not be empty");
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
