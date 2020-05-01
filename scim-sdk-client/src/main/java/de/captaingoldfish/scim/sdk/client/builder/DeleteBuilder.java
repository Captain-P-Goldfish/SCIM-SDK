package de.captaingoldfish.scim.sdk.client.builder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.response.DeleteResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 11:35 <br>
 * <br>
 */
public class DeleteBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
{

  /**
   * the resource id that should be returned
   */
  private String id;


  public DeleteBuilder(String baseUrl,
                       String endpoint,
                       ScimClientConfig scimClientConfig,
                       Class<T> responseEntityType,
                       ScimHttpClient scimHttpClient)
  {
    super(baseUrl, endpoint, scimClientConfig, responseEntityType, scimHttpClient);
  }


  /**
   * @param id sets the resource id of the resource that should be delete from the server
   */
  public DeleteBuilder<T> setId(String id)
  {
    if (StringUtils.isBlank(id))
    {
      throw new IllegalStateException("id must not be blank for delete-requests");
    }
    this.id = id;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected <T1 extends ScimResponse> T1 buildScimResponse(int httpResponseCode, String responseBody)
  {
    Class<T1> type = HttpStatus.NO_CONTENT == httpResponseCode ? (Class<T1>)DeleteResponse.class
      : (Class<T1>)ErrorResponse.class;
    return JsonHelper.readJsonDocument(responseBody, type);
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
    if (StringUtils.isBlank(id))
    {
      throw new IllegalStateException("id must not be blank for delete-requests");
    }
    HttpDelete httpDelete = new HttpDelete(getBaseUrl() + getEndpoint() + "/" + id);
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
