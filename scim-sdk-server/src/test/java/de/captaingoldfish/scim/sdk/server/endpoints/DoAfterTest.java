package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;


/**
 * author Pascal Knueppel <br>
 * created at: 14.07.2020 - 11:04 <br>
 * <br>
 */
public class DoAfterTest extends AbstractEndpointTest
{


  /**
   * verifies that the doAfterExecution consumer is being executed with isError = false if no error occurred
   */
  @Test
  public void testDoAfterExecutionPositive()
  {
    AtomicInteger counter = new AtomicInteger(0);
    AtomicBoolean wasExecuted = new AtomicBoolean(false);
    BiConsumer<ScimResponse, Boolean> doAfterExecution = (scimResponse, isError) -> {
      Assertions.assertFalse(isError);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      wasExecuted.set(true);
      counter.incrementAndGet();
    };
    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(EndpointPaths.USERS),
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               doAfterExecution);
    Assertions.assertTrue(wasExecuted.get());
    Assertions.assertEquals(1, counter.get());
  }


  /**
   * verifies that the doAfterExecution consumer is being executed with isError = true if a ScimException was
   * thrown
   */
  @Test
  public void testDoAfterExecutionNegative()
  {
    AtomicInteger counter = new AtomicInteger(0);
    AtomicBoolean wasExecuted = new AtomicBoolean(false);
    BiConsumer<ScimResponse, Boolean> doAfterExecution = (scimResponse, isError) -> {
      Assertions.assertTrue(isError);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      wasExecuted.set(true);
      counter.incrementAndGet();
    };

    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(EndpointPaths.USERS) + "/1",
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               doAfterExecution);
    Assertions.assertTrue(wasExecuted.get());
    Assertions.assertEquals(1, counter.get());
  }

  /**
   * verifies that the doAfterExecution consumer is being executed with isError = true if an InternalError
   * occurred. In this case a NullPointer
   */
  @Test
  public void testDoAfterExecutionNegativeWithNullPointer()
  {
    AtomicInteger counter = new AtomicInteger(0);
    AtomicBoolean wasExecuted = new AtomicBoolean(false);
    BiConsumer<ScimResponse, Boolean> doAfterExecution = (scimResponse, isError) -> {
      Assertions.assertTrue(isError);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                               Matchers.typeCompatibleWith(InternalServerException.class));
      wasExecuted.set(true);
      counter.incrementAndGet();
    };

    Mockito.doThrow(NullPointerException.class).when(userHandler).getResource(Mockito.anyString(), Mockito.any());

    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(EndpointPaths.USERS) + "/1",
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               doAfterExecution);
    Assertions.assertTrue(wasExecuted.get());
    Assertions.assertEquals(1, counter.get());
  }

  /**
   * verifies that the doAfterExecution consumer is being executed with isError = false if a bulk request with
   * several operations was executed and that the consumer will be executed only once
   */
  @Test
  public void testDoAfterExecutionPositivieForBulk()
  {
    AtomicInteger counter = new AtomicInteger(0);
    AtomicBoolean wasExecuted = new AtomicBoolean(false);
    BiConsumer<ScimResponse, Boolean> doAfterExecution = (scimResponse, isError) -> {
      Assertions.assertFalse(isError);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
      wasExecuted.set(true);
      counter.incrementAndGet();
    };

    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(getCreateUserBulkOperations(2)).build();
    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(EndpointPaths.BULK),
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders,
                                                               doAfterExecution);
    Assertions.assertTrue(wasExecuted.get());
    Assertions.assertEquals(1, counter.get());
  }

  /**
   * verifies that the doAfterExecution consumer is being executed with isError = true if a bulk request with
   * several operations was executed and that the consumer will be executed only once
   */
  @Test
  public void testDoAfterExecutionNegativeForBulkWithScimException()
  {
    AtomicInteger counter = new AtomicInteger(0);
    AtomicBoolean wasExecuted = new AtomicBoolean(false);
    BiConsumer<ScimResponse, Boolean> doAfterExecution = (scimResponse, isError) -> {
      Assertions.assertTrue(isError);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
      wasExecuted.set(true);
      counter.incrementAndGet();
    };

    Mockito.doThrow(ConflictException.class).when(userHandler).createResource(Mockito.any(), Mockito.any());

    BulkRequest bulkRequest = BulkRequest.builder()
                                         .failOnErrors(0)
                                         .bulkRequestOperation(getCreateUserBulkOperations(2))
                                         .build();
    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(EndpointPaths.BULK),
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders,
                                                               doAfterExecution);
    Assertions.assertTrue(wasExecuted.get());
    Assertions.assertEquals(1, counter.get());
  }

  /**
   * verifies that the doAfterExecution consumer is being executed with isError = true if a bulk request with
   * several operations was executed and that the consumer will be executed only once
   */
  @Test
  public void testDoAfterExecutionNegativeForBulkWithNullPointerException()
  {
    AtomicInteger counter = new AtomicInteger(0);
    AtomicBoolean wasExecuted = new AtomicBoolean(false);
    BiConsumer<ScimResponse, Boolean> doAfterExecution = (scimResponse, isError) -> {
      Assertions.assertTrue(isError);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
      wasExecuted.set(true);
      counter.incrementAndGet();
    };

    Mockito.doThrow(NullPointerException.class).when(userHandler).createResource(Mockito.any(), Mockito.any());

    BulkRequest bulkRequest = BulkRequest.builder()
                                         .failOnErrors(0)
                                         .bulkRequestOperation(getCreateUserBulkOperations(2))
                                         .build();
    ScimResponse scimResponse = resourceEndpoint.handleRequest(getUrl(EndpointPaths.BULK),
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders,
                                                               doAfterExecution);
    Assertions.assertTrue(wasExecuted.get());
    Assertions.assertEquals(1, counter.get());
  }
}
