package de.captaingoldfish.scim.sdk.common.request;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.AbstractSchemasHolder;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 20:34 <br>
 * <br>
 * represents a simple bulk request
 */
public class BulkRequest extends AbstractSchemasHolder
{

  public BulkRequest()
  {
    setSchemas(Collections.singletonList(SchemaUris.BULK_REQUEST_URI));
  }

  @Builder
  public BulkRequest(Integer failOnErrors, List<BulkRequestOperation> bulkRequestOperation)
  {
    this();
    setFailOnErrors(failOnErrors);
    setBulkRequestOperations(bulkRequestOperation);
  }

  /**
   * An integer specifying the number of errors that the service provider will accept before the operation is
   * terminated and an error response is returned. OPTIONAL in a request. Not valid in a response.
   */
  public Optional<Integer> getFailOnErrors()
  {
    return getLongAttribute(AttributeNames.RFC7643.FAIL_ON_ERRORS).map(Long::intValue);
  }

  /**
   * An integer specifying the number of errors that the service provider will accept before the operation is
   * terminated and an error response is returned. OPTIONAL in a request. Not valid in a response.
   */
  public void setFailOnErrors(Integer failOnErrors)
  {
    setAttribute(AttributeNames.RFC7643.FAIL_ON_ERRORS, failOnErrors == null ? null : failOnErrors.longValue());
  }

  /**
   * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
   * resource endpoint. REQUIRED.
   */
  public List<BulkRequestOperation> getBulkRequestOperations()
  {
    List<BulkRequestOperation> operations = getArrayAttribute(AttributeNames.RFC7643.OPERATIONS,
                                                              BulkRequestOperation.class);
    if (operations.isEmpty())
    {
      this.setAttribute(AttributeNames.RFC7643.OPERATIONS, (ObjectNode)null);
    }
    return operations;
  }

  /**
   * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
   * resource endpoint. REQUIRED.
   */
  public void setBulkRequestOperations(List<BulkRequestOperation> bulkRequestOperation)
  {
    setAttribute(AttributeNames.RFC7643.OPERATIONS, bulkRequestOperation);
  }
}
