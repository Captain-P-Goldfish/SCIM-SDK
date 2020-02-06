package de.captaingoldfish.scim.sdk.client.builder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.GetResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 13.12.2019 - 08:21 <br>
 * <br>
 */
public class GetBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
{

  /**
   * the resource id that should be returned
   */
  private String id;


  public GetBuilder(String baseUrl, ScimClientConfig scimClientConfig, Class<T> responseEntityType)
  {
    super(baseUrl, scimClientConfig, responseEntityType);
  }


  /**
   * @param id sets the resource id of the resource that should be returned from the server
   */
  public GetBuilder<T> setId(String id)
  {
    if (StringUtils.isBlank(id))
    {
      throw new IllegalStateException("id must not be blank for get-requests");
    }
    this.id = id;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GetBuilder<T> setEndpoint(String endpoint)
  {
    return (GetBuilder<T>)super.setEndpoint(endpoint);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GetBuilder<T> setETagForIfMatch(String version)
  {
    return (GetBuilder<T>)super.setETagForIfMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GetBuilder<T> setETagForIfNoneMatch(String version)
  {
    return (GetBuilder<T>)super.setETagForIfNoneMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GetBuilder<T> setETagForIfMatch(ETag version)
  {
    return (GetBuilder<T>)super.setETagForIfMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GetBuilder<T> setETagForIfNoneMatch(ETag version)
  {
    return (GetBuilder<T>)super.setETagForIfNoneMatch(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected <T1 extends ScimResponse> T1 buildScimResponse(int httpResponseCode, String responseBody)
  {
    Class<T1> type = HttpStatus.OK == httpResponseCode ? (Class<T1>)GetResponse.class : (Class<T1>)ErrorResponse.class;
    return JsonHelper.readJsonDocument(responseBody, type);
  }

  /**
   * @return a get request to the desired resource
   */
  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    if (StringUtils.isBlank(id))
    {
      throw new IllegalStateException("id must not be blank for get-requests");
    }
    HttpGet httpGet = new HttpGet(getBaseUrl() + getEndpoint() + "/" + id);
    if (isUseIfMatch())
    {
      httpGet.setHeader(HttpHeader.IF_MATCH_HEADER, getVersion().toString());
    }
    if (isUseIfNoneMatch())
    {
      httpGet.setHeader(HttpHeader.IF_NONE_MATCH_HEADER, getVersion().toString());
    }
    return httpGet;
  }
}
