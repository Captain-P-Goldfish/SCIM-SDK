package de.captaingoldfish.scim.sdk.client.builder;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.utils.EncodingUtils;


/**
 * author Pascal Knueppel <br>
 * created at: 13.12.2019 - 08:21 <br>
 * <br>
 */
public class GetBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
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
  public GetBuilder(String fullUrl, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
  {
    super(responseEntityType, scimHttpClient);
    this.fullUrl = fullUrl;
  }


  public GetBuilder(String baseUrl,
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
  public GetBuilder<T> setExpectedResponseHeaders(Map<String, String> requiredResponseHeaders)
  {
    return (GetBuilder<T>)super.setExpectedResponseHeaders(requiredResponseHeaders);
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
    HttpGet httpGet;
    if (StringUtils.isBlank(fullUrl))
    {
      httpGet = new HttpGet(getBaseUrl() + getEndpoint());
    }
    else
    {
      httpGet = new HttpGet(fullUrl);
    }
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
