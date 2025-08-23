package de.captaingoldfish.scim.sdk.server.endpoints.bulkcontext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import lombok.Data;


/**
 * this object gathers infos about the currently active bulk-request
 *
 * @author Pascal Knueppel
 * @since 23.08.2025
 */
@Data
public class BulkRequestContext
{

  /**
   * the successful operations
   */
  private final Map<String, BulkOperationDetails> successfulOperations = new HashMap<>();

  /**
   * the failed operations
   */
  private final Map<String, BulkOperationDetails> failedOperations = new HashMap<>();

  /**
   * the operation that is handled right now
   */
  private BulkRequestOperation currentlyHandledOperation;

  /**
   * the failOnErrors value from the request
   */
  private int failOnErrors;

  /**
   * the current amount of errors that did already occur.
   */
  private int currentNumberOfErrors;

  /**
   * if the {@link #currentlyHandledOperation} is the last operation to handle within the bulk-request
   */
  private boolean isLastOperation;

  public void addSuccessOperation(BulkRequestOperation operation, BulkResponseOperation responseOperation)
  {
    String bulkId = getBulkIdOfOperation(operation);
    successfulOperations.put(bulkId, new BulkOperationDetails(operation, responseOperation));
  }

  public void addFailedOperation(BulkRequestOperation operation, BulkResponseOperation responseOperation)
  {
    String bulkId = getBulkIdOfOperation(operation);
    failedOperations.put(bulkId, new BulkOperationDetails(operation, responseOperation));
  }

  /**
   * gets the current bulkId of the bulk request operation and sets a bulkId if no bulkId was assigned yet
   */
  private String getBulkIdOfOperation(BulkRequestOperation operation)
  {
    String bulkId = operation.getBulkId().orElseGet(UUID.randomUUID()::toString);
    if (!operation.getBulkId().isPresent())
    {
      operation.setBulkId(bulkId);
    }
    return bulkId;
  }

}
