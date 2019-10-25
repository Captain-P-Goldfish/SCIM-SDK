package de.gold.scim.request;

import java.util.Collections;
import java.util.List;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.resources.AbstractSchemasHolder;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 20:34 <br>
 * <br>
 * represents a simple bulk request
 */
public class BulkResponse extends AbstractSchemasHolder
{

  public BulkResponse()
  {
    setSchemas(Collections.singletonList(SchemaUris.BULK_RESPONSE_URI));
  }

  @Builder
  public BulkResponse(List<BulkResponseOperation> bulkResponseOperation)
  {
    this();
    setBulkResponseOperations(bulkResponseOperation);
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
      this.setAttribute(AttributeNames.RFC7643.OPERATIONS, operations);
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
}
