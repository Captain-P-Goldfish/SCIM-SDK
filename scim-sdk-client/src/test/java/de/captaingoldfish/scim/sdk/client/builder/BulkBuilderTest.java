package de.captaingoldfish.scim.sdk.client.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class BulkBuilderTest extends HttpServerMockup
{

  /**
   * some tests might change the configuration of the scim configuration so this method will set back this
   * configuration
   */
  @AfterEach
  public void resetConfiguration()
  {
    scimConfig.getServiceProvider().getBulkConfig().setMaxOperations(10);
    scimConfig.getServiceProvider().getBulkConfig().setMaxPayloadSize((long)(Math.pow(1024, 2) * 2));
  }

  /**
   * verifies that a bulk request is correctly resolved after return from the server
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testCreateBulkRequest(boolean useFullUrl)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl, null);
    ServerResponse<BulkResponse> response = bulkBuilder.bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .bulkId(UUID.randomUUID().toString())
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * causes a precondition failed response from the server by causing errors in the bulk request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testCreateWithPreconditionFailed(boolean useFullUrl)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl, null);
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0)
                                                       .bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * causes a payload too large error on the server side
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testPayloadTooLarge(boolean useFullUrl)
  {
    scimConfig.getServiceProvider().getBulkConfig().setMaxPayloadSize(1L);
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl, null);
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0)
                                                       .bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
  }

  /**
   * causes a too many operations error on the server side
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testTooManyOperations(boolean useFullUrl)
  {
    scimConfig.getServiceProvider().getBulkConfig().setMaxOperations(0);
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl, null);
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0)
                                                       .bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
  }

  /**
   * assumes that a wrong server was called and a totally unknown response is returned
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testCommunicationWithWrongServer(boolean useFullUrl)
  {
    setGetResponseBody(() -> "an internal server error occurred");
    setGetResponseStatus(() -> HttpStatus.INTERNAL_SERVER_ERROR);
    setGetResponseHeaders(() -> {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeader.CONTENT_TYPE_HEADER, MediaType.TEXT_PLAIN_VALUE);
      return headers;
    });

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    String url = useFullUrl ? getServerUrl() + EndpointPaths.BULK : getServerUrl();
    BulkBuilder bulkBuilder = new BulkBuilder(url, scimHttpClient, useFullUrl, null);
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0)
                                                       .bulkRequestOperation(EndpointPaths.USERS)
                                                       .method(HttpMethod.POST)
                                                       .data(User.builder().userName("goldfish").build())
                                                       .sendRequest();
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    Assertions.assertEquals(getGetResponseBody().get(), response.getResponseBody());
  }

  /**
   * verifies that a request with too many operations that has no bulkId references is correctly split into
   * several requests
   */
  @Test
  public void testTooManyOperationsWithAutomaticSplittingEnabled_simple()
  {
    final int maxNumberOfOperations = 3;
    scimConfig.getServiceProvider().getBulkConfig().setMaxOperations(maxNumberOfOperations);

    List<BulkRequestOperation> requestOperations = new ArrayList<>();
    requestOperations.addAll(Arrays.asList(createBulkRequestOperation("user 1"),
                                           createBulkRequestOperation("user 2"),
                                           createBulkRequestOperation("user 3"),
                                           createBulkRequestOperation("user 4"),
                                           createBulkRequestOperation("user 5"),
                                           createBulkRequestOperation("user 6"),
                                           createBulkRequestOperation("user 7")));

    setVerifyRequestAttributes((httpExchange, body) -> {
      BulkRequest bulkRequest = JsonHelper.readJsonDocument(body, BulkRequest.class);
      Assertions.assertEquals(maxNumberOfOperations, bulkRequest.getBulkRequestOperations().size());

      // prepare verification for the next nested request
      setVerifyRequestAttributes((httpExchange1, body2) -> {
        BulkRequest bulkRequest2 = JsonHelper.readJsonDocument(body2, BulkRequest.class);
        Assertions.assertEquals(maxNumberOfOperations, bulkRequest2.getBulkRequestOperations().size());

        // prepare verification for the next nested request
        setVerifyRequestAttributes((httpExchange2, body3) -> {
          BulkRequest bulkRequest3 = JsonHelper.readJsonDocument(body3, BulkRequest.class);
          Assertions.assertEquals(1, bulkRequest3.getBulkRequestOperations().size());
        });
      });
    });


    ScimClientConfig scimClientConfig = ScimClientConfig.builder().enableAutomaticBulkRequestSplitting(true).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    BulkBuilder bulkBuilder = Mockito.spy(new BulkBuilder(getServerUrl(), scimHttpClient, false,
                                                          scimConfig.getServiceProvider()));
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0).addOperations(requestOperations).sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    BulkResponse bulkResponse = response.getResource();
    Assertions.assertEquals(requestOperations.size(), bulkResponse.getBulkResponseOperations().size());
    // verify that 3 responses have been evaluated
    Mockito.verify(bulkBuilder, Mockito.times(3)).toResponse(Mockito.any());
    log.debug(bulkResponse.toPrettyString());
  }

  /**
   * verifies that an error message is appropriately thrown if one of the requests did fail
   */
  @Test
  public void testTooManyOperationsWithAutomaticSplittingEnabled_simpleWithError()
  {
    final int maxNumberOfOperations = 3;
    scimConfig.getServiceProvider().getBulkConfig().setMaxOperations(maxNumberOfOperations);

    List<BulkRequestOperation> requestOperations = new ArrayList<>();
    requestOperations.addAll(Arrays.asList(createBulkRequestOperation("user 1"),
                                           createBulkRequestOperation("user 2"),
                                           createBulkRequestOperation("user 3"),
                                           createBulkRequestOperation("user 4"),
                                           // empty operation is unparseable on the server
                                           BulkRequestOperation.builder().build()));

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().enableAutomaticBulkRequestSplitting(true).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    BulkBuilder bulkBuilder = Mockito.spy(new BulkBuilder(getServerUrl(), scimHttpClient, false,
                                                          scimConfig.getServiceProvider()));
    IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                                                       () -> bulkBuilder.failOnErrors(0)
                                                                        .addOperations(requestOperations)
                                                                        .sendRequest());
    MatcherAssert.assertThat(ex.getMessage(),
                             Matchers.startsWith("The bulk request failed with status: 400 and message"));
  }

  /**
   * verifies that an error message is appropriately thrown if the server misses returning a bulkId
   */
  @Test
  public void testTooManyOperationsWithAutomaticSplittingEnabled_simpleWithError2()
  {
    final int maxNumberOfOperations = 3;
    scimConfig.getServiceProvider().getBulkConfig().setMaxOperations(maxNumberOfOperations);
    setGetResponseBody(() -> {
      // empty data on response will cause validation error if the bulkId is not present
      BulkResponseOperation responseOperation = BulkResponseOperation.builder().build();
      BulkResponse bulkResponse = BulkResponse.builder()
                                              .bulkResponseOperation(Collections.singletonList(responseOperation))
                                              .build();
      return bulkResponse.toString();
    });
    setGetResponseStatus(() -> HttpStatus.OK);

    List<BulkRequestOperation> requestOperations = new ArrayList<>();
    requestOperations.addAll(Arrays.asList(createBulkRequestOperation("user 1"),
                                           createBulkRequestOperation("user 2"),
                                           createBulkRequestOperation("user 3"),
                                           createBulkRequestOperation("user 4")));

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().enableAutomaticBulkRequestSplitting(true).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    BulkBuilder bulkBuilder = Mockito.spy(new BulkBuilder(getServerUrl(), scimHttpClient, false,
                                                          scimConfig.getServiceProvider()));
    IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                                                       () -> bulkBuilder.failOnErrors(0)
                                                                        .addOperations(requestOperations)
                                                                        .sendRequest());
    MatcherAssert.assertThat(ex.getMessage(),
                             Matchers.startsWith("Missing bulkId in response cannot resolve relations of split operations."));
  }

  /**
   * verifies that the id of the resources is also resolved if only present in the location-attribute
   */
  @Test
  public void testTooManyOperationsWithAutomaticSplittingEnabled_simpleResolveIdFromLocation()
  {
    final int maxNumberOfOperations = 3;
    scimConfig.getServiceProvider().getBulkConfig().setMaxOperations(maxNumberOfOperations);
    setManipulateResponse(responseBody -> {
      BulkResponse bulkResponse = JsonHelper.readJsonDocument(responseBody, BulkResponse.class);
      List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
      responseOperations.forEach(op -> op.setResourceId(null));
      bulkResponse.setBulkResponseOperations(responseOperations);
      return bulkResponse.toString();
    });
    setGetResponseStatus(() -> HttpStatus.OK);

    List<BulkRequestOperation> requestOperations = new ArrayList<>();
    requestOperations.addAll(Arrays.asList(createBulkRequestOperation("user 1"),
                                           createBulkRequestOperation("user 2"),
                                           createBulkRequestOperation("user 3"),
                                           createBulkRequestOperation("user 4")));

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().enableAutomaticBulkRequestSplitting(true).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    BulkBuilder bulkBuilder = Mockito.spy(new BulkBuilder(getServerUrl(), scimHttpClient, false,
                                                          scimConfig.getServiceProvider()));
    bulkBuilder.failOnErrors(0).addOperations(requestOperations).sendRequest();
    Mockito.verify(bulkBuilder, Mockito.times(requestOperations.size())).getIdFromLocationAttribute(Mockito.any());
  }

  /**
   * verifies that complex structures with bulkIds are also resolved correctly if several resources are
   * dependent on one another
   */
  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4, 5, 6})
  public void testTooManyOperationsWithAutomaticSplittingEnabled_Complex(int maxNumberOfOperations)
  {
    scimConfig.getServiceProvider().getBulkConfig().setMaxOperations(maxNumberOfOperations);


    setGetResponseStatus(() -> HttpStatus.OK);

    List<BulkRequestOperation> requestOperations = createComplexBulkReferenceRequests();

    ScimClientConfig scimClientConfig = ScimClientConfig.builder().enableAutomaticBulkRequestSplitting(true).build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    BulkBuilder bulkBuilder = Mockito.spy(new BulkBuilder(getServerUrl(), scimHttpClient, false,
                                                          scimConfig.getServiceProvider()));
    ServerResponse<BulkResponse> response = bulkBuilder.failOnErrors(0).addOperations(requestOperations).sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    BulkResponse bulkResponse = response.getResource();
    Assertions.assertEquals(requestOperations.size(), bulkResponse.getBulkResponseOperations().size());
    Mockito.verify(bulkBuilder, Mockito.times((int)Math.ceil((double)requestOperations.size() / maxNumberOfOperations)))
           .toResponse(Mockito.any());
  }

  private BulkRequestOperation createBulkRequestOperation(String username)
  {
    return BulkRequestOperation.builder()
                               .bulkId(UUID.randomUUID().toString())
                               .method(HttpMethod.POST)
                               .path(EndpointPaths.USERS)
                               .returnResource(true)
                               .data(User.builder().userName(username).build().toString())
                               .build();
  }

  private BulkRequestOperation createBulkRequestOperation(String bulkId, String username)
  {
    return BulkRequestOperation.builder()
                               .bulkId(bulkId)
                               .method(HttpMethod.POST)
                               .path(EndpointPaths.USERS)
                               .returnResource(true)
                               .data(User.builder().userName(username).build().toString())
                               .build();
  }

  private BulkRequestOperation createDependentBulkRequestOperation(String bulkId,
                                                                   String groupName,
                                                                   String... memberReferences)
  {
    Group.GroupBuilder groupBuilder = Group.builder();
    List<Member> groupMembers = new ArrayList<>();
    for ( String memberReference : memberReferences )
    {
      groupMembers.add(Member.builder()
                             .value(String.format("%s:%s", AttributeNames.RFC7643.BULK_ID, memberReference))
                             .build());
    }
    groupBuilder.members(groupMembers);
    Group group = groupBuilder.displayName(groupName).build();
    return BulkRequestOperation.builder()
                               .bulkId(bulkId)
                               .method(HttpMethod.POST)
                               .path(EndpointPaths.GROUPS)
                               .returnResource(true)
                               .data(group.toString())
                               .build();
  }

  /**
   * this method will create a complex bulk-request-operation sequence that could be represented as the
   * following tree where each number represents an operation and the relations a bulkId-reference to another
   * resource
   *
   * <pre>
   *     (936)              (619)              (219)              (333)              (987)              (222)
   *       |                                     |                  |
   *       |                                     |                  |
   *     (123)                                  / \                 |
   *       |  \                                /   \               /|
   *       |   \                              /     \             / |
   *       |    ---------- (443)--------------       - (488) -----  |
   *       |                                             |          |
   *       |                                             |          |
   *       |                                            / \         |
   *     (582)                                         /   \        |
   *                                                  /     \       |
   *                                                 /       \      |
   *                                               (523)      |     |
   *                                                 |        |     |
   *                                                  \       |     |
   *                                                   \      |     |
   *                                                     -- (111) --
   * </pre>
   */
  private List<BulkRequestOperation> createComplexBulkReferenceRequests()
  {
    List<BulkRequestOperation> operationList = new ArrayList<>();
    // first start with the unparented operations
    BulkRequestOperation _619 = createBulkRequestOperation("619", "619");
    BulkRequestOperation _987 = createBulkRequestOperation("987", "987");
    BulkRequestOperation _222 = createBulkRequestOperation("222", "222");
    operationList.addAll(Arrays.asList(_619, _987, _222));

    // now start with the first layer, the leafs
    BulkRequestOperation _582 = createBulkRequestOperation("582", "582");
    BulkRequestOperation _443 = createBulkRequestOperation("443", "443");
    BulkRequestOperation _111 = createBulkRequestOperation("111", "111");
    operationList.addAll(Arrays.asList(_582, _443, _111));

    // next the second layer
    BulkRequestOperation _123 = createDependentBulkRequestOperation("123", "123", "582", "443");
    BulkRequestOperation _523 = createDependentBulkRequestOperation("523", "523", "111");
    operationList.addAll(Arrays.asList(_123, _523));

    // and the fourth layer
    BulkRequestOperation _936 = createDependentBulkRequestOperation("936", "936", "123");
    BulkRequestOperation _488 = createDependentBulkRequestOperation("488", "488", "523", "111");
    operationList.addAll(Arrays.asList(_936, _488));

    // and the last layer
    BulkRequestOperation _219 = createDependentBulkRequestOperation("219", "219", "443", "488");
    BulkRequestOperation _333 = createDependentBulkRequestOperation("333", "333", "488", "111");
    operationList.addAll(Arrays.asList(_219, _333));

    // make sure that order is not important by shuffling the operations
    Collections.shuffle(operationList);
    return operationList;
  }
}
