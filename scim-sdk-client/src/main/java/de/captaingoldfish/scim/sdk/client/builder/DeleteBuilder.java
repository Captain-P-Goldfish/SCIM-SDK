package de.captaingoldfish.scim.sdk.client.builder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;

import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 11:35 <br>
 * <br>
 */
public class DeleteBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
{


  public DeleteBuilder(String baseUrl,
                       String endpoint,
                       String resourceId,
                       Class<T> responseEntityType,
                       ScimHttpClient scimHttpClient)
  {
    super(baseUrl, endpoint + (StringUtils.isBlank(resourceId) ? "" : "/" + resourceId), responseEntityType,
          scimHttpClient);
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
  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    HttpDelete httpDelete = new HttpDelete(getBaseUrl() + getEndpoint());
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
}
