package de.captaingoldfish.scim.sdk.common.response;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 23:34 <br>
 * <br>
 */
public class BulkResponseTest
{


  /**
   * verifies that the correct schema uri is set
   */
  @Test
  public void testSchemaUriIsSet()
  {
    BulkResponse bulkResponse = new BulkResponse();
    Assertions.assertEquals(1, bulkResponse.getSchemas().size());
    Assertions.assertEquals(SchemaUris.BULK_RESPONSE_URI, bulkResponse.getSchemas().iterator().next());
  }

  /**
   * verifies that the getter and setter methods are working as expected
   */
  @Test
  public void testGetAndSetValues()
  {
    final List<BulkResponseOperation> operations = Collections.singletonList(BulkResponseOperation.builder().build());

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    Assertions.assertEquals(operations, bulkResponse.getBulkResponseOperations());

    Assertions.assertEquals(operations.size(), bulkResponse.get("Operations").size());
  }

  /**
   * verifies that the bulk request operation is never null if getter is called
   */
  @Test
  public void testOperationsIsNeverNull()
  {
    BulkResponse bulkResponse = new BulkResponse();
    Assertions.assertNotNull(bulkResponse.getBulkResponseOperations());
  }
}
