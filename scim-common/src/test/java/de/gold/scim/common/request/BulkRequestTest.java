package de.gold.scim.common.request;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.common.constants.SchemaUris;
import de.gold.scim.common.utils.FileReferences;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 23:01 <br>
 * <br>
 */
public class BulkRequestTest implements FileReferences
{

  /**
   * verifies that the correct schema uri is set
   */
  @Test
  public void testSchemaUriIsSet()
  {
    BulkRequest bulkRequest = new BulkRequest();
    Assertions.assertEquals(1, bulkRequest.getSchemas().size());
    Assertions.assertEquals(SchemaUris.BULK_REQUEST_URI, bulkRequest.getSchemas().iterator().next());
  }

  /**
   * verifies that the getter and setter methods are working as expected
   */
  @Test
  public void testGetAndSetValues()
  {
    final Integer failOnErrors = 5;
    final List<BulkRequestOperation> operations = Arrays.asList(BulkRequestOperation.builder().build(),
                                                                BulkRequestOperation.builder().build());

    BulkRequest bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();

    Assertions.assertEquals(failOnErrors, bulkRequest.getFailOnErrors().get());
    Assertions.assertEquals(operations, bulkRequest.getBulkRequestOperations());

    Assertions.assertEquals(failOnErrors, bulkRequest.get("failOnErrors").intValue());
    Assertions.assertEquals(operations.size(), bulkRequest.get("Operations").size());
  }

  /**
   * verifies that the bulk request operation is never null if getter is called
   */
  @Test
  public void testOperationsIsNeverNull()
  {
    BulkRequest bulkRequest = new BulkRequest();
    Assertions.assertNotNull(bulkRequest.getBulkRequestOperations());
  }
}
