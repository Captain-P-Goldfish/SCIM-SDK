package de.captaingoldfish.scim.sdk.client.builder;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.exceptions.InvalidRequestException;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;


/**
 * author Pascal Knueppel <br>
 * created at: 07.12.2019 - 23:13 <br>
 * <br>
 */
public class CreateBuilder<T extends ResourceNode> extends RequestBuilder<T>
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
  public CreateBuilder(String fullUrl, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
  {
    super(null, null, responseEntityType, scimHttpClient);
    this.fullUrl = fullUrl;
  }

  public CreateBuilder(String baseUrl, String endpoint, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
  {
    super(baseUrl, endpoint, responseEntityType, scimHttpClient);
    this.fullUrl = null;
  }

  /**
   * @param resource sets the resource that should be sent to the service provider
   */
  @Override
  public CreateBuilder<T> setResource(String resource)
  {
    return (CreateBuilder<T>)super.setResource(resource);
  }

  /**
   * @param resource sets the resource that should be sent to the service provider
   */
  @Override
  public CreateBuilder<T> setResource(JsonNode resource)
  {
    return (CreateBuilder<T>)super.setResource(resource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServerResponse<T> sendRequest()
  {
    if (StringUtils.isBlank(getResource()))
    {
      throw new InvalidRequestException("no resource set");
    }
    return super.sendRequest();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isExpectedResponseCode(int httpStatus)
  {
    return HttpStatus.CREATED == httpStatus;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    HttpPost httpPost;
    if (StringUtils.isBlank(fullUrl))
    {
      httpPost = new HttpPost(getBaseUrl() + getEndpoint());
    }
    else
    {
      httpPost = new HttpPost(fullUrl);
    }
    StringEntity stringEntity = new StringEntity(getResource(), StandardCharsets.UTF_8);
    httpPost.setEntity(stringEntity);
    return httpPost;
  }
}
