package de.captaingoldfish.scim.sdk.common.response;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 20:34 <br>
 * <br>
 * represents a simple bulk response
 */
public class BulkResponse extends ScimResponse
{

  /**
   * the http status that should be set with this response
   */
  private int httpStatus;

  @Builder
  public BulkResponse(List<BulkResponseOperation> bulkResponseOperation, int httpStatus)
  {
    this();
    setBulkResponseOperations(bulkResponseOperation);
    this.httpStatus = httpStatus;
  }

  public BulkResponse()
  {
    super(null);
    setSchemas(Collections.singletonList(SchemaUris.BULK_RESPONSE_URI));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return httpStatus;
  }

  /**
   * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
   * resource endpoint. REQUIRED.
   */
  public List<BulkResponseOperation> getBulkResponseOperations()
  {
    List<BulkResponseOperation> operations = getArrayAttribute(AttributeNames.RFC7643.OPERATIONS,
                                                               BulkResponseOperation.class);
    if (operations.isEmpty())
    {
      setAttribute(AttributeNames.RFC7643.OPERATIONS, operations);
    }
    return operations;
  }

  /**
   * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
   * resource endpoint. REQUIRED.
   */
  public void setBulkResponseOperations(List<BulkResponseOperation> bulkRequestOperations)
  {
    setAttribute(AttributeNames.RFC7643.OPERATIONS, bulkRequestOperations);
  }

  /**
   * tries to find a bulk response operation matching the given bulkId. It is also possible to get a bulk
   * request with a null-bulkId. Be sure to use that only if you are certain that only a single entry has a
   * null-bulkId
   *
   * @param bulkId the bulk id of the operation that should be extracted (null allowed)
   * @return the operation or an empty if no operation did match the bulkId
   */
  public Optional<BulkResponseOperation> getByBulkId(String bulkId)
  {
    return getBulkResponseOperations().stream().filter(op -> {
      boolean isNullMatch = !op.getBulkId().isPresent() && bulkId == null;
      boolean isBulkIdMatch = bulkId != null && op.getBulkId().map(bulkId::equals).orElse(false);
      return isNullMatch || isBulkIdMatch;
    }).findFirst();
  }

  /**
   * tries to find a bulk response operation by searching for a resource's id. This can be used on update/patch
   * or delete requests
   *
   * @param resourceId the resources id of the operation that should be extracted
   * @return the operation or an empty if no operation did match the resourceId
   */
  public Optional<BulkResponseOperation> getByResourceId(String resourceId)
  {
    return getBulkResponseOperations().stream().filter(op -> {
      return resourceId != null && op.getResourceId().map(resourceId::equals).orElseGet(() -> {
        return op.getResponse()
                 .map(node -> node.get(AttributeNames.RFC7643.ID))
                 .map(JsonNode::textValue)
                 .map(resourceId::equals)
                 .orElse(false);
      });
    }).findFirst();
  }

  /**
   * @return all operations that do have a bulkId
   */
  public List<BulkResponseOperation> getOperationsWithBulkId()
  {
    return getBulkResponseOperations().stream().filter(op -> op.getBulkId().isPresent()).collect(Collectors.toList());
  }

  /**
   * @return all operations that do not have a bulkId
   */
  public List<BulkResponseOperation> getOperationsWithoutBulkId()
  {
    return getBulkResponseOperations().stream().filter(op -> !op.getBulkId().isPresent()).collect(Collectors.toList());
  }

  /**
   * override lombok builder with public constructor
   */
  public static class BulkResponseBuilder
  {

    public BulkResponseBuilder()
    {}
  }
}
