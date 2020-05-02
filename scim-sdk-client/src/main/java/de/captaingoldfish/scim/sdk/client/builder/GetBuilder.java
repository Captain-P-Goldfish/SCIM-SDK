package de.captaingoldfish.scim.sdk.client.builder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;


/**
 * author Pascal Knueppel <br>
 * created at: 13.12.2019 - 08:21 <br>
 * <br>
 */
public class GetBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
{


  public GetBuilder(String baseUrl,
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
  protected boolean isExpectedResponseCode(int httpStatus)
  {
    return HttpStatus.OK == httpStatus;
  }

  /**
   * @return a get request to the desired resource
   */
  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    HttpGet httpGet = new HttpGet(getBaseUrl() + getEndpoint());
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
