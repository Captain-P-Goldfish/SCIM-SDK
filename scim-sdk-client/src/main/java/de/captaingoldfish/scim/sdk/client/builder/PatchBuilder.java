package de.captaingoldfish.scim.sdk.client.builder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;


/**
 * allows to build a patch request that can be sent to the server<br>
 * <br>
 * created at: 01.05.2020
 *
 * @author Pascal Knüppel
 */
public class PatchBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
{

  /**
   * the patch request operations that will be filled in this builders execution
   */
  private List<PatchRequestOperation> operations;

  public PatchBuilder(String baseUrl,
                      String endpoint,
                      String resourceId,
                      Class<T> responseEntityType,
                      ScimHttpClient scimHttpClient)
  {
    super(baseUrl, endpoint + (StringUtils.isBlank(resourceId) ? "" : "/" + resourceId), responseEntityType,
          scimHttpClient);
    operations = new ArrayList<>();
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
   * sets the patch request directly if already build into the request context
   *
   * @param resource the patch request representation
   */
  public RequestBuilder<T> setPatchResource(PatchOpRequest resource)
  {
    return super.setResource(resource);
  }

  /**
   * sets the patch request directly if already build into the request context
   *
   * @param resource the patch request representation
   */
  public RequestBuilder<T> setPatchResource(String resource)
  {
    return super.setResource(resource);
  }

  /**
   * @return an operation builder to add a new operation
   */
  public PatchOperationBuilder<T> addOperation()
  {
    return new PatchOperationBuilder<>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    HttpPatch httpPatch = new HttpPatch(getBaseUrl() + getEndpoint());
    StringEntity stringEntity = new StringEntity(getResource(), StandardCharsets.UTF_8);
    httpPatch.setEntity(stringEntity);
    return httpPatch;
  }

  /**
   * @return the resource that will be sent to the server
   */
  @Override
  protected String getResource()
  {
    return new PatchOpRequest(operations).toString();
  }

  /**
   * used to build a simple patch operation
   */
  public static class PatchOperationBuilder<T extends ResourceNode>
  {

    /**
     * the owning top level class instance
     */
    private PatchBuilder<T> patchBuilder;

    /**
     * the builder instance that is used to prepare the patch operation
     */
    private PatchRequestOperation.PatchRequestOperationBuilder builder;

    public PatchOperationBuilder(PatchBuilder<T> patchBuilder)
    {
      this.patchBuilder = patchBuilder;
      this.builder = PatchRequestOperation.builder();
    }

    /**
     * sets the optional path attribute.<br>
     * <br>
     * Note that the valueNode is considered the resource itself if not path attribute is set
     *
     * @param path the scim node name e.g. "name.givenName" or "emails[primary eq true]" or just "userName"
     */
    public PatchOperationBuilder<T> path(String path)
    {
      builder.path(path);
      return this;
    }

    /**
     * sets the operation that should be executed on the resource<br>
     * <br>
     * Note that the "path"-attribute is required for a remove operation
     *
     * @param op the operation that should be executed
     */
    public PatchOperationBuilder<T> op(PatchOp op)
    {
      builder.op(op);
      return this;
    }

    /**
     * sets a single value to this patch operation
     */
    public PatchOperationBuilder<T> value(String values)
    {
      builder.values(Collections.singletonList(values));
      return this;
    }

    /**
     * sets a set of values that might be added, replaced or removed from the targeted resource
     */
    public PatchOperationBuilder<T> values(List<String> values)
    {
      builder.values(values);
      return this;
    }

    /**
     * sets a node that might be a simple text-node a json object or a json array that will then be added,
     * replaced or removed from the target
     *
     * @param valueNode a simple text-node, a json object or a json array
     */
    public PatchOperationBuilder<T> valueNode(JsonNode valueNode)
    {
      builder.valueNode(valueNode);
      return this;
    }

    /**
     * builds this operation
     */
    public PatchBuilder<T> build()
    {
      patchBuilder.operations.add(builder.build());
      return patchBuilder;
    }

    /**
     * builds this patch operation and goes on to the next operation
     */
    public PatchOperationBuilder<T> next()
    {
      build();
      return new PatchOperationBuilder<>(patchBuilder);
    }
  }

}
