package de.captaingoldfish.scim.sdk.client.builder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


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
   * the fully qualified url to the required resource
   */
  private final String fullUrl;

  /**
   * the patch request operations that will be filled in this builders execution
   */
  private List<PatchRequestOperation> operations;

  /**
   * if the resource should be retrieved by using the fully qualified url
   *
   * @param fullUrl the fully qualified url to the required resource
   * @param responseEntityType the type of the resource that should be returned
   * @param scimHttpClient the http client instance
   */
  public PatchBuilder(String fullUrl, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
  {
    super(responseEntityType, scimHttpClient);
    operations = new ArrayList<>();
    this.fullUrl = fullUrl;
  }

  public PatchBuilder(String baseUrl,
                      String endpoint,
                      String resourceId,
                      Class<T> responseEntityType,
                      ScimHttpClient scimHttpClient)
  {
    super(baseUrl, endpoint + (StringUtils.isBlank(resourceId) ? "" : "/" + resourceId), responseEntityType,
          scimHttpClient);
    operations = new ArrayList<>();
    this.fullUrl = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PatchBuilder<T> setExpectedResponseHeaders(Map<String, String> requiredResponseHeaders)
  {
    return (PatchBuilder<T>)super.setExpectedResponseHeaders(requiredResponseHeaders);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isExpectedResponseCode(int httpStatus)
  {
    return HttpStatus.OK == httpStatus || HttpStatus.NO_CONTENT == httpStatus;
  }

  /**
   * sets the patch request directly if already build into the request context
   *
   * @param resource the patch request representation
   */
  public PatchBuilder<T> setPatchResource(PatchOpRequest resource)
  {
    PatchBuilder<T> patchBuilder = (PatchBuilder<T>)super.setResource(resource);
    this.operations = resource.getOperations();
    return patchBuilder;
  }

  /**
   * sets the patch request directly if already build into the request context
   *
   * @param resource the patch request representation
   */
  public PatchBuilder<T> setPatchResource(String resource)
  {
    PatchBuilder<T> patchBuilder = (PatchBuilder<T>)super.setResource(resource);
    PatchOpRequest patchOpRequest = JsonHelper.readJsonDocument(resource, PatchOpRequest.class);
    this.operations = patchOpRequest.getOperations();
    return patchBuilder;
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
    HttpPatch httpPatch;
    if (StringUtils.isBlank(fullUrl))
    {
      httpPatch = new HttpPatch(getBaseUrl() + getEndpoint());
    }
    else
    {
      httpPatch = new HttpPatch(fullUrl);
    }
    StringEntity stringEntity = new StringEntity(getResource(), StandardCharsets.UTF_8);
    httpPatch.setEntity(stringEntity);
    return httpPatch;
  }

  /**
   * @return the resource that will be sent to the server
   */
  @Override
  public final String getResource()
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
     * sets a list of nodes that might be a simple text-nodes, json objects or json arrays that will then be added
     * into an array node.
     *
     * @param valueNode list of simple text-nodes, json objects or json arrays
     */
    public PatchOperationBuilder<T> valueNodes(List<? extends JsonNode> valueNodes)
    {
      ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
      valueNodes.forEach(arrayNode::add);
      builder.valueNode(arrayNode);
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
