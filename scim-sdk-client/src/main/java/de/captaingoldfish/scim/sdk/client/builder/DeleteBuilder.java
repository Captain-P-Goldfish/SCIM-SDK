package de.captaingoldfish.scim.sdk.client.builder;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.http.HttpDelete;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.utils.EncodingUtils;
import lombok.SneakyThrows;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 11:35 <br>
 * <br>
 */
public class DeleteBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
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
  public DeleteBuilder(String fullUrl, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
  {
    super(responseEntityType, scimHttpClient);
    this.fullUrl = fullUrl;
  }

  public DeleteBuilder(String baseUrl,
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
   * @param resource sets the resource that should be sent to the service provider
   */
  public RequestBuilder<T> setResource(String resource)
  {
    return super.setResource(resource);
  }

  /**
   * @param resource sets the resource that should be sent to the service provider
   */
  public RequestBuilder<T> setResource(JsonNode resource)
  {
    return super.setResource(resource);
  }

  /**
   * Kept protected for delete requests to prevent misunderstandings. The delete-response-header check is
   * disabled by default because we do not expect any content-types on delete for example
   */
  @Override
  protected DeleteBuilder<T> setExpectedResponseHeaders(Map<String, String> requiredResponseHeaders)
  {
    return (DeleteBuilder<T>)super.setExpectedResponseHeaders(requiredResponseHeaders);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isExpectedResponseCode(int httpStatus)
  {
    return HttpStatus.NO_CONTENT == httpStatus;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DeleteBuilder<T> setETagForIfMatch(String version)
  {
    return (DeleteBuilder<T>)super.setETagForIfMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DeleteBuilder<T> setETagForIfNoneMatch(String version)
  {
    return (DeleteBuilder<T>)super.setETagForIfNoneMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DeleteBuilder<T> setETagForIfMatch(ETag version)
  {
    return (DeleteBuilder<T>)super.setETagForIfMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DeleteBuilder<T> setETagForIfNoneMatch(ETag version)
  {
    return (DeleteBuilder<T>)super.setETagForIfNoneMatch(version);
  }

  /**
   * @return a delete request to the desired resource
   */
  @SneakyThrows
  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    HttpDelete httpDelete;
    if (StringUtils.isBlank(fullUrl))
    {
      httpDelete = new HttpDelete(getBaseUrl() + getEndpoint());
    }
    else
    {
      httpDelete = new HttpDelete(fullUrl);
    }
    if (StringUtils.isNotBlank(getResource()))
    {
      httpDelete.setEntity(new StringEntity(getResource()));
    }
    if (isUseIfMatch())
    {
      httpDelete.setHeader(HttpHeader.IF_MATCH_HEADER, getVersion().toString());
    }
    if (isUseIfNoneMatch())
    {
      httpDelete.setHeader(HttpHeader.IF_NONE_MATCH_HEADER, getVersion().toString());
    }
    return httpDelete;
  }

  /**
   * a delete response does not require any response headers
   */
  @Override
  protected Map<String, String> getRequiredResponseHeaders()
  {
    return new HashMap<>();
  }
}
