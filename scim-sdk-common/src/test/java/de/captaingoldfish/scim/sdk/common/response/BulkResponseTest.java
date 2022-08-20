package de.captaingoldfish.scim.sdk.common.response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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

  /**
   * verifies that the method {@link BulkResponse#getByBulkId(String)}} operates as expected
   */
  @Test
  public void testGetByBulkId()
  {
    final String op1BulkId = UUID.randomUUID().toString();
    final String op2BulkId = UUID.randomUUID().toString();
    BulkResponseOperation op1 = BulkResponseOperation.builder().bulkId(op1BulkId).build();
    BulkResponseOperation op2 = BulkResponseOperation.builder().bulkId(op2BulkId).build();
    BulkResponseOperation op3 = BulkResponseOperation.builder().build();
    final List<BulkResponseOperation> operations = Arrays.asList(op1, op2, op3);

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    Assertions.assertEquals(bulkResponse.getByBulkId(op1BulkId).get(), op1);
    Assertions.assertEquals(bulkResponse.getByBulkId(op2BulkId).get(), op2);
    Assertions.assertEquals(bulkResponse.getByBulkId(null).get(), op3);
  }

  /**
   * verifies that the method {@link BulkResponse#getOperationsWithoutBulkId()}} operates as expected
   */
  @Test
  public void testGetOperationsWithoutABulkId()
  {
    final String op1BulkId = UUID.randomUUID().toString();
    final String op2BulkId = UUID.randomUUID().toString();
    BulkResponseOperation op1 = BulkResponseOperation.builder().bulkId(op1BulkId).build();
    BulkResponseOperation op2 = BulkResponseOperation.builder().bulkId(op2BulkId).build();
    BulkResponseOperation op3 = BulkResponseOperation.builder().build();
    BulkResponseOperation op4 = BulkResponseOperation.builder().build();
    final List<BulkResponseOperation> operations = Arrays.asList(op1, op2, op3, op4);

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    List<BulkResponseOperation> responseOperations = bulkResponse.getOperationsWithoutBulkId();
    MatcherAssert.assertThat(responseOperations, Matchers.containsInAnyOrder(op3, op4));
  }

  /**
   * verifies that the method {@link BulkResponse#getOperationsWithBulkId()}} operates as expected
   */
  @Test
  public void testGetOperationsWitBulkId()
  {
    final String op1BulkId = UUID.randomUUID().toString();
    final String op2BulkId = UUID.randomUUID().toString();
    BulkResponseOperation op1 = BulkResponseOperation.builder().bulkId(op1BulkId).build();
    BulkResponseOperation op2 = BulkResponseOperation.builder().bulkId(op2BulkId).build();
    BulkResponseOperation op3 = BulkResponseOperation.builder().build();
    BulkResponseOperation op4 = BulkResponseOperation.builder().build();
    final List<BulkResponseOperation> operations = Arrays.asList(op1, op2, op3, op4);

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    List<BulkResponseOperation> responseOperations = bulkResponse.getOperationsWithBulkId();
    MatcherAssert.assertThat(responseOperations, Matchers.containsInAnyOrder(op1, op2));
  }
}
