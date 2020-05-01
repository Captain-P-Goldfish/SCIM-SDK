package de.captaingoldfish.scim.sdk.client.builder;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
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

  public CreateBuilder(String baseUrl,
                       String endpoint,
                       ScimClientConfig scimClientConfig,
                       Class<T> responseEntityType,
                       ScimHttpClient scimHttpClient)
  {
    super(baseUrl, endpoint, responseEntityType, scimHttpClient);
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
    HttpPost httpPost = new HttpPost(getBaseUrl() + getEndpoint());
    StringEntity stringEntity = new StringEntity(getResource(), StandardCharsets.UTF_8);
    httpPost.setEntity(stringEntity);
    return httpPost;
  }
}
