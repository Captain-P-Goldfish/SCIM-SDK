package de.captaingoldfish.scim.sdk.client.builder;

import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import lombok.AccessLevel;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 13.12.2019 - 09:02 <br>
 * <br>
 * an abstract class for request builder that may use etags in the requests
 */
@Getter(AccessLevel.PROTECTED)
public abstract class ETagRequestBuilder<T extends ResourceNode> extends RequestBuilder<T>
{

  /**
   * the last known version of this resource
   */
  private ETag version;

  /**
   * if the If-Match header should be used
   */
  private boolean useIfMatch;

  /**
   * if the If-None-Match header should be used
   */
  private boolean useIfNoneMatch;

  public ETagRequestBuilder(String baseUrl,
                            ScimClientConfig scimClientConfig,
                            Class<T> responseEntityType,
                            ScimHttpClient scimHttpClient)
  {
    super(baseUrl, scimClientConfig, responseEntityType, scimHttpClient);
  }

  /**
   * uses the given version with a If-Match header in the request
   *
   * @param version the version to use in the request
   * @return this builder object
   */
  public ETagRequestBuilder<T> setETagForIfMatch(String version)
  {
    this.version = ETag.newInstance(version);
    if (useIfNoneMatch)
    {
      throw new IllegalStateException("cannot use both headers '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                                      + HttpHeader.IF_NONE_MATCH_HEADER + "' in a single request");
    }
    this.useIfMatch = true;
    return this;
  }

  /**
   * uses the given version with a If-Match header in the request
   *
   * @param version the version to use in the request
   * @return this builder object
   */
  public ETagRequestBuilder<T> setETagForIfNoneMatch(String version)
  {
    this.version = ETag.newInstance(version);
    if (useIfMatch)
    {
      throw new IllegalStateException("cannot use both headers '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                                      + HttpHeader.IF_NONE_MATCH_HEADER + "' in a single request");
    }
    this.useIfNoneMatch = true;
    return this;
  }

  /**
   * uses the given version with a If-Match header in the request
   *
   * @param version the version to use in the request
   * @return this builder object
   */
  public ETagRequestBuilder<T> setETagForIfMatch(ETag version)
  {
    this.version = version;
    if (useIfNoneMatch)
    {
      throw new IllegalStateException("cannot use both headers '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                                      + HttpHeader.IF_NONE_MATCH_HEADER + "' in a single request");
    }
    this.useIfMatch = true;
    return this;
  }

  /**
   * uses the given version with a If-Match header in the request
   *
   * @param version the version to use in the request
   * @return this builder object
   */
  public ETagRequestBuilder<T> setETagForIfNoneMatch(ETag version)
  {
    this.version = version;
    if (useIfMatch)
    {
      throw new IllegalStateException("cannot use both headers '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                                      + HttpHeader.IF_NONE_MATCH_HEADER + "' in a single request");
    }
    this.useIfNoneMatch = true;
    return this;
  }
}
