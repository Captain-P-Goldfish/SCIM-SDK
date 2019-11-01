package de.gold.scim.common.response;

import java.util.Collections;
import java.util.List;

import de.gold.scim.common.constants.AttributeNames;
import de.gold.scim.common.constants.SchemaUris;
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

}
