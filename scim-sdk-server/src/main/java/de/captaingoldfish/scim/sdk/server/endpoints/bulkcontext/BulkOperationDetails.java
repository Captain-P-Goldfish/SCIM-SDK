package de.captaingoldfish.scim.sdk.server.endpoints.bulkcontext;

import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import lombok.Data;


/**
 * @author Pascal Knueppel
 * @since 23.08.2025
 */
@Data
public class BulkOperationDetails
{

  /**
   * the request operation of a bulk request
   */
  private final BulkRequestOperation bulkRequestOperation;

  /**
   * the corresponding response to the {@link #bulkRequestOperation}
   */
  private final BulkResponseOperation bulkResponseOperation;

}
