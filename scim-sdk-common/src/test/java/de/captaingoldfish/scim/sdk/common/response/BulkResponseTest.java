package de.captaingoldfish.scim.sdk.common.response;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;


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
  public void testGetOperationsWithBulkId()
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

  /**
   * verifies that the method {@link BulkResponse#getByBulkIds(List)}} operates as expected
   */
  @Test
  public void testGetByBulkIds()
  {
    final String op1BulkId = UUID.randomUUID().toString();
    final String op2BulkId = UUID.randomUUID().toString();
    final String op3BulkId = UUID.randomUUID().toString();
    BulkResponseOperation op1 = BulkResponseOperation.builder().bulkId(op1BulkId).build();
    BulkResponseOperation op2 = BulkResponseOperation.builder().bulkId(op2BulkId).build();
    BulkResponseOperation op3 = BulkResponseOperation.builder().bulkId(op3BulkId).build();
    BulkResponseOperation op4 = BulkResponseOperation.builder().build();
    final List<BulkResponseOperation> operations = Arrays.asList(op1, op2, op3, op4);

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    List<BulkResponseOperation> responseOperations = bulkResponse.getByBulkIds(new HashSet<>(Arrays.asList(op1BulkId,
                                                                                                           op3BulkId)))
                                                                 .collect(Collectors.toList());
    MatcherAssert.assertThat(responseOperations, Matchers.containsInAnyOrder(op1, op3));
  }

  /**
   * verifies that the method {@link BulkResponse#getSuccessfulOperations()}} returns the correct operations
   * correctly
   */
  @Test
  public void testGetSuccessfulOperations()
  {
    BulkResponseOperation op1 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.POST)
                                                     .status(HttpStatus.CREATED)
                                                     .build();
    BulkResponseOperation op2 = BulkResponseOperation.builder().method(HttpMethod.PUT).status(HttpStatus.OK).build();
    BulkResponseOperation op3 = BulkResponseOperation.builder().method(HttpMethod.PATCH).status(HttpStatus.OK).build();
    BulkResponseOperation op4 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.PATCH)
                                                     .status(HttpStatus.NO_CONTENT)
                                                     .build();
    BulkResponseOperation op5 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.DELETE)
                                                     .status(HttpStatus.NO_CONTENT)
                                                     .build();
    final List<BulkResponseOperation> operations = Arrays.asList(op1, op2, op3, op4, op5);

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    List<BulkResponseOperation> successfulOperations = bulkResponse.getSuccessfulOperations()
                                                                   .collect(Collectors.toList());
    MatcherAssert.assertThat(successfulOperations, Matchers.containsInAnyOrder(op1, op2, op3, op4, op5));

    List<BulkResponseOperation> failedOperations = bulkResponse.getFailedOperations().collect(Collectors.toList());
    ;
    Assertions.assertEquals(0, failedOperations.size());
  }

  /**
   * verifies that the method {@link BulkResponse#getFailedOperations()}} returns the correct operations
   * correctly
   */
  @Test
  public void testGetFailedOperations()
  {
    BulkResponseOperation op1 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.POST)
                                                     .status(HttpStatus.BAD_REQUEST)
                                                     .build();
    BulkResponseOperation op2 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.PUT)
                                                     .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                     .build();
    BulkResponseOperation op3 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.PATCH)
                                                     .status(HttpStatus.BAD_REQUEST)
                                                     .build();
    BulkResponseOperation op4 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.PATCH)
                                                     .status(HttpStatus.NO_CONTENT)
                                                     .build();
    BulkResponseOperation op5 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.DELETE)
                                                     .status(HttpStatus.UNAUTHORIZED)
                                                     .build();
    final List<BulkResponseOperation> operations = Arrays.asList(op1, op2, op3, op4, op5);

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    List<BulkResponseOperation> successfulOperations = bulkResponse.getSuccessfulOperations()
                                                                   .collect(Collectors.toList());
    MatcherAssert.assertThat(successfulOperations, Matchers.containsInAnyOrder(op4));

    List<BulkResponseOperation> failedOperations = bulkResponse.getFailedOperations().collect(Collectors.toList());
    MatcherAssert.assertThat(failedOperations, Matchers.containsInAnyOrder(op1, op2, op3, op5));
  }

  /**
   * verifies that the method {@link BulkResponse#getSuccessfulOperations(HttpMethod)}} returns the correct
   * operations correctly
   */
  @Test
  public void testGetSuccessfulOperationsWithHttpMethods()
  {
    BulkResponseOperation op1 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.POST)
                                                     .status(HttpStatus.CREATED)
                                                     .build();
    BulkResponseOperation op2 = BulkResponseOperation.builder().method(HttpMethod.PUT).status(HttpStatus.OK).build();
    BulkResponseOperation op3 = BulkResponseOperation.builder().method(HttpMethod.PATCH).status(HttpStatus.OK).build();
    BulkResponseOperation op4 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.PATCH)
                                                     .status(HttpStatus.NO_CONTENT)
                                                     .build();
    BulkResponseOperation op5 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.DELETE)
                                                     .status(HttpStatus.NO_CONTENT)
                                                     .build();
    final List<BulkResponseOperation> operations = Arrays.asList(op1, op2, op3, op4, op5);

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    {
      List<BulkResponseOperation> successfulOperations = bulkResponse.getSuccessfulOperations(HttpMethod.POST)
                                                                     .collect(Collectors.toList());
      MatcherAssert.assertThat(successfulOperations, Matchers.containsInAnyOrder(op1));
    }
    {
      List<BulkResponseOperation> successfulOperations = bulkResponse.getSuccessfulOperations(HttpMethod.PUT)
                                                                     .collect(Collectors.toList());
      MatcherAssert.assertThat(successfulOperations, Matchers.containsInAnyOrder(op2));
    }
    {
      List<BulkResponseOperation> successfulOperations = bulkResponse.getSuccessfulOperations(HttpMethod.PATCH)
                                                                     .collect(Collectors.toList());
      MatcherAssert.assertThat(successfulOperations, Matchers.containsInAnyOrder(op3, op4));
    }
    {
      List<BulkResponseOperation> successfulOperations = bulkResponse.getSuccessfulOperations(HttpMethod.DELETE)
                                                                     .collect(Collectors.toList());
      MatcherAssert.assertThat(successfulOperations, Matchers.containsInAnyOrder(op5));
    }
  }

  /**
   * verifies that the method {@link BulkResponse#getFailedOperations(HttpMethod)}} returns the correct
   * operations correctly
   */
  @Test
  public void testGetFailedOperationsWithHttpMethods()
  {
    BulkResponseOperation op1 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.POST)
                                                     .status(HttpStatus.BAD_REQUEST)
                                                     .build();
    BulkResponseOperation op2 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.PUT)
                                                     .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                     .build();
    BulkResponseOperation op3 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.PATCH)
                                                     .status(HttpStatus.BAD_REQUEST)
                                                     .build();
    BulkResponseOperation op4 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.PATCH)
                                                     .status(HttpStatus.NO_CONTENT)
                                                     .build();
    BulkResponseOperation op5 = BulkResponseOperation.builder()
                                                     .method(HttpMethod.DELETE)
                                                     .status(HttpStatus.UNAUTHORIZED)
                                                     .build();
    final List<BulkResponseOperation> operations = Arrays.asList(op1, op2, op3, op4, op5);

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    List<BulkResponseOperation> successfulOperations = bulkResponse.getSuccessfulOperations()
                                                                   .collect(Collectors.toList());
    MatcherAssert.assertThat(successfulOperations, Matchers.containsInAnyOrder(op4));

    {
      List<BulkResponseOperation> failedOperations = bulkResponse.getFailedOperations(HttpMethod.POST)
                                                                 .collect(Collectors.toList());
      MatcherAssert.assertThat(failedOperations, Matchers.containsInAnyOrder(op1));
    }
    {
      List<BulkResponseOperation> failedOperations = bulkResponse.getFailedOperations(HttpMethod.PUT)
                                                                 .collect(Collectors.toList());
      MatcherAssert.assertThat(failedOperations, Matchers.containsInAnyOrder(op2));
    }
    {
      List<BulkResponseOperation> failedOperations = bulkResponse.getFailedOperations(HttpMethod.PATCH)
                                                                 .collect(Collectors.toList());
      MatcherAssert.assertThat(failedOperations, Matchers.containsInAnyOrder(op3));
    }
    {
      List<BulkResponseOperation> failedOperations = bulkResponse.getFailedOperations(HttpMethod.DELETE)
                                                                 .collect(Collectors.toList());
      MatcherAssert.assertThat(failedOperations, Matchers.containsInAnyOrder(op5));
    }
  }
}
