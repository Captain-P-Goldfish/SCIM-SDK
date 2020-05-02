package de.captaingoldfish.scim.sdk.client.builder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import lombok.AccessLevel;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 08.03.2020 <br>
 * <br>
 */
public class BulkBuilder extends RequestBuilder<BulkResponse>
{

  /**
   * the builder object to build the bulk request
   */
  private final BulkRequest.BulkRequestBuilder builder;

  /**
   * the bulk request operations that should be executed
   */
  @Getter(AccessLevel.PROTECTED)
  private final List<BulkRequestOperation> bulkRequestOperationList;

  public BulkBuilder(String baseUrl, ScimClientConfig scimClientConfig, ScimHttpClient scimHttpClient)
  {
    super(baseUrl, EndpointPaths.BULK, BulkResponse.class, scimHttpClient);

    builder = BulkRequest.builder();
    bulkRequestOperationList = new ArrayList<>();
    builder.bulkRequestOperation(bulkRequestOperationList);
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

  /**
   * overrides the default method from the superclass to have easier control of the resource that will be put
   * into the request body
   */
  @Override
  protected String getResource()
  {
    return builder.build().toString();
  }

  /**
   * checks if the response contains a schema-uri that matches the value of
   * {@link de.captaingoldfish.scim.sdk.common.constants.SchemaUris#BULK_RESPONSE_URI}
   */
  @Override
  protected Function<HttpResponse, Boolean> isResponseParseable()
  {
    return httpResponse -> {
      String responseBody = httpResponse.getResponseBody();
      if (StringUtils.isNotBlank(responseBody) && responseBody.contains(SchemaUris.BULK_RESPONSE_URI))
      {
        return true;
      }
      return false;
    };
  }

  /**
   * sets how many errors are allowed on the server side before the request should be rolled back
   *
   * @param failOnErrors the number of errors that are accepted on the server side
   */
  public BulkBuilder failOnErrors(Integer failOnErrors)
  {
    builder.failOnErrors(failOnErrors);
    return this;
  }

  /**
   * sets the path to the resource endpoint e.g. "/Users" or "/Groups"
   */
  public BulkRequestOperationCreator bulkRequestOperation(String path)
  {
    return bulkRequestOperation(path, null);
  }

  /**
   * sets the path to the resource endpoint e.g. "/Users" or "/Groups"
   *
   * @param path "/Users", "/Groups" or any other registered resource path
   * @param id the id of an existing resource in case of patch, update or delete
   */
  public BulkRequestOperationCreator bulkRequestOperation(String path, String id)
  {
    String idPath = StringUtils.isBlank(id) ? "" : "/" + id;
    return new BulkRequestOperationCreator(this, path + idPath);
  }

  /**
   * an additional build step class that allows to set the values of a bulk operation
   */
  public static class BulkRequestOperationCreator
  {

    /**
     * the owning top level class reference
     */
    private final BulkBuilder bulkBuilder;

    /**
     * the builder object that is used to build the operation
     */
    private final BulkRequestOperation.BulkRequestOperationBuilder builder = BulkRequestOperation.builder();

    public BulkRequestOperationCreator(BulkBuilder bulkBuilder, String path)
    {
      this.bulkBuilder = bulkBuilder;
      builder.path(path);
    }

    /**
     * sets the http method for this bulk operation
     */
    public BulkRequestOperationCreator method(HttpMethod method)
    {
      builder.method(method);
      return this;
    }

    /**
     * sets the bulkId for this operation. Required if http method is post and optional in any other cases
     */
    public BulkRequestOperationCreator bulkId(String bulkId)
    {
      builder.bulkId(bulkId);
      return this;
    }

    /**
     * sets the request body for this operation if any is required
     */
    public BulkRequestOperationCreator data(String data)
    {
      builder.data(data);
      return this;
    }

    /**
     * sets the request body for this operation if any is required
     */
    public BulkRequestOperationCreator data(JsonNode data)
    {
      builder.data(data.toString());
      return this;
    }

    /**
     * sets the etag version for this operation which may be used on update, path and delete requests
     */
    public BulkRequestOperationCreator version(ETag version)
    {
      builder.version(version);
      return this;
    }

    /**
     * @return builds the operation object and returns to the owning top level instance
     */
    public BulkBuilder next()
    {
      bulkBuilder.getBulkRequestOperationList().add(builder.build());
      return bulkBuilder;
    }

    /**
     * builds the operation and directly sends the request to the server
     */
    public ServerResponse<BulkResponse> sendRequest()
    {
      return sendRequest(Collections.emptyMap());
    }

    /**
     * builds the operation and directly sends the request to the server
     */
    public ServerResponse<BulkResponse> sendRequest(Map<String, String[]> httpHeaders)
    {
      return next().sendRequest(httpHeaders);
    }
  }
}
