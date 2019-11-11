package de.gold.scim.server.endpoints;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import de.gold.scim.common.constants.EndpointPaths;
import de.gold.scim.common.constants.HttpStatus;
import de.gold.scim.common.constants.ResourceTypeNames;
import de.gold.scim.common.constants.ScimType;
import de.gold.scim.common.constants.enums.HttpMethod;
import de.gold.scim.common.exceptions.BadRequestException;
import de.gold.scim.common.exceptions.NotImplementedException;
import de.gold.scim.common.exceptions.ResponseException;
import de.gold.scim.common.request.BulkRequest;
import de.gold.scim.common.request.BulkRequestOperation;
import de.gold.scim.common.resources.EnterpriseUser;
import de.gold.scim.common.resources.Group;
import de.gold.scim.common.resources.ServiceProvider;
import de.gold.scim.common.resources.User;
import de.gold.scim.common.resources.complex.Manager;
import de.gold.scim.common.resources.complex.Meta;
import de.gold.scim.common.resources.multicomplex.Member;
import de.gold.scim.common.response.BulkResponse;
import de.gold.scim.common.response.BulkResponseOperation;
import de.gold.scim.common.response.ErrorResponse;
import de.gold.scim.common.response.ScimResponse;
import de.gold.scim.common.utils.JsonHelper;
import de.gold.scim.server.endpoints.base.GroupEndpointDefinition;
import de.gold.scim.server.endpoints.base.UserEndpointDefinition;
import de.gold.scim.server.endpoints.handler.GroupHandlerImpl;
import de.gold.scim.server.endpoints.handler.UserHandlerImpl;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 08.11.2019 - 22:54 <br>
 * <br>
 */
@Slf4j
public class BulkEndpointTest extends AbstractBulkTest
{

  /**
   * a simple basic uri used in these tests
   */
  private static final String BASE_URI = "https://localhost/scim/v2";

  /**
   * the resource endpoint under test
   */
  private BulkEndpoint bulkEndpoint;

  /**
   * the service provider configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * a mockito spy to verify the class that have been made on this instance
   */
  private UserHandlerImpl userHandler;

  /**
   * a mockito spy to verify the class that have been made on this instance
   */
  private GroupHandlerImpl groupHandler;

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    serviceProvider = ServiceProvider.builder().build();
    userHandler = Mockito.spy(new UserHandlerImpl());
    groupHandler = Mockito.spy(new GroupHandlerImpl());
    ResourceEndpoint resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler),
                                                             new GroupEndpointDefinition(groupHandler));
    bulkEndpoint = new BulkEndpoint(resourceEndpoint, serviceProvider, resourceEndpoint.getResourceTypeFactory());
  }

  /**
   * will verify that a user can be created, updated and deleted when using bulk
   */
  @Test
  public void testSendBulkRequest()
  {
    final int maxOperations = 10;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations * 3);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    List<BulkRequestOperation> operations = new ArrayList<>();
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    operations.addAll(createOperations);
    final int failOnErrors = 0;
    BulkRequest bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    Assertions.assertEquals(maxOperations, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).createResource(Mockito.any());

    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertEquals(HttpMethod.POST, bulkResponseOperation.getMethod());
      Assertions.assertEquals(HttpStatus.CREATED, bulkResponseOperation.getStatus());
      Assertions.assertFalse(bulkResponseOperation.getResponse().isPresent());
      Assertions.assertTrue(bulkResponseOperation.getBulkId().isPresent());
      Assertions.assertTrue(bulkResponseOperation.getLocation().isPresent());
      MatcherAssert.assertThat(bulkResponseOperation.getLocation().get(),
                               Matchers.startsWith(BASE_URI + EndpointPaths.USERS));
    }
    operations = new ArrayList<>();
    operations.addAll(getUpdateUserBulkOperations(userHandler.getInMemoryMap().values()));
    operations.addAll(getDeleteUserBulkOperations(userHandler.getInMemoryMap().values()));
    bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();
    scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).updateResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).deleteResource(Mockito.any());

    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    for ( BulkResponseOperation bulkResponseOperation : responseOperations.subList(0, maxOperations - 1) )
    {
      Assertions.assertEquals(HttpMethod.PUT, bulkResponseOperation.getMethod());
      Assertions.assertEquals(HttpStatus.OK, bulkResponseOperation.getStatus());
      Assertions.assertFalse(bulkResponseOperation.getResponse().isPresent());
      Assertions.assertFalse(bulkResponseOperation.getBulkId().isPresent());
      Assertions.assertTrue(bulkResponseOperation.getLocation().isPresent());
      MatcherAssert.assertThat(bulkResponseOperation.getLocation().get(),
                               Matchers.startsWith(BASE_URI + EndpointPaths.USERS));
    }

    for ( BulkResponseOperation bulkResponseOperation : responseOperations.subList(maxOperations,
                                                                                   responseOperations.size() - 1) )
    {
      Assertions.assertEquals(HttpMethod.DELETE, bulkResponseOperation.getMethod());
      Assertions.assertEquals(HttpStatus.NO_CONTENT, bulkResponseOperation.getStatus());
      Assertions.assertFalse(bulkResponseOperation.getResponse().isPresent());
      Assertions.assertTrue(bulkResponseOperation.getBulkId().isPresent());
      Assertions.assertTrue(bulkResponseOperation.getLocation().isPresent());
    }
  }

  /**
   * shows that the request is validated and an exception is thrown if the bulk request is not conform to its
   * definition
   */
  @Test
  public void testSendBulkRequestWithJsonArrayInBody()
  {
    final int maxOperations = 10;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    try
    {
      bulkEndpoint.bulk(BASE_URI, createOperations.toString());
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      MatcherAssert.assertThat(ex.getDetail(), Matchers.containsString("document does not have a 'schemas'-attribute"));
    }
  }

  /**
   * verifies that an exception is thrown if a bulk post-request is missing a bulkId
   */
  @Test
  public void testBulkIdIsMissingOnPost()
  {
    final int maxOperations = 10;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    createOperations.get(createOperations.size() - 1).setBulkId(null);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;

    int responseSize = bulkResponse.getBulkResponseOperations().size();
    BulkResponseOperation bulkResponseOperation = bulkResponse.getBulkResponseOperations().get(responseSize - 1);
    MatcherAssert.assertThat(bulkResponseOperation.getResponse().get().getScimException().getClass(),
                             Matchers.typeCompatibleWith(ResponseException.class));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, bulkResponseOperation.getStatus());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, bulkResponseOperation.getResponse().get().getStatus());
    MatcherAssert.assertThat(bulkResponseOperation.getResponse().get().getDetail().get(),
                             Matchers.equalTo("missing 'bulkId' on BULK-POST request"));

    for ( int i = 0 ; i < bulkResponse.getBulkResponseOperations().size() - 1 ; i++ )
    {
      bulkResponseOperation = bulkResponse.getBulkResponseOperations().get(i);
      Assertions.assertFalse(bulkResponseOperation.getResponse().isPresent());
      Assertions.assertEquals(HttpStatus.CREATED, bulkResponseOperation.getStatus());
      MatcherAssert.assertThat(bulkResponseOperation.getLocation().get(),
                               Matchers.startsWith(BASE_URI + EndpointPaths.USERS));
    }
  }

  /**
   * checks that the bulk requests will be handled successfully for update and delete if the bulkId is missing
   */
  @ParameterizedTest
  @ValueSource(strings = {"PUT", "DELETE"})
  public void testBulkIdIsMissingOnOtherRequestsThanCreate(HttpMethod httpMethod)
  {
    final int maxOperations = 10;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);

    for ( int i = 0 ; i < maxOperations ; i++ )
    {
      String id = UUID.randomUUID().toString();
      Meta meta = Meta.builder()
                      .resourceType(ResourceTypeNames.USER)
                      .created(LocalDateTime.now())
                      .lastModified(LocalDateTime.now())
                      .build();
      User user = User.builder().id(id).userName(id).meta(meta).build();
      userHandler.getInMemoryMap().put(id, user);
    }
    List<BulkRequestOperation> operations = new ArrayList<>();
    switch (httpMethod)
    {
      case PUT:
        operations.addAll(getUpdateUserBulkOperations(userHandler.getInMemoryMap().values(), httpMethod));
        break;
      case DELETE:
        operations.addAll(getDeleteUserBulkOperations(userHandler.getInMemoryMap().values(), httpMethod));
        break;
      default:
        throw new IllegalStateException("not supported");
    }

    operations.forEach(operation -> operation.setBulkId(null));
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
  }

  /**
   * verifies that the processing of the operations is aborted after the failOnErrors value is exceeded
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  public void testFailOnErrorsWorks(int failOnErrors)
  {
    final int maxOperations = failOnErrors + 1;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    Mockito.doThrow(new BadRequestException("something bad", null, null))
           .when(userHandler)
           .createResource(Mockito.any());
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder()
                                         .failOnErrors(failOnErrors)
                                         .bulkRequestOperation(createOperations)
                                         .build();
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(failOnErrors, bulkResponse.getBulkResponseOperations().size());

    bulkResponse.getBulkResponseOperations().forEach(operation -> {
      Assertions.assertTrue(operation.getResponse().isPresent());
      ErrorResponse errorResponse = operation.getResponse().get();
      MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                               Matchers.typeCompatibleWith(ResponseException.class));
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
      Assertions.assertEquals("something bad", errorResponse.getDetail().get());
    });

    Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, bulkResponse.getHttpStatus());
    log.warn(bulkResponse.toPrettyString());
  }

  /**
   * verifies that bulk cannot be used if the service provider has set its support to false
   */
  @Test
  public void testFailIfBulkIsNotSupported()
  {
    final int maxOperations = 1;
    serviceProvider.getBulkConfig().setSupported(false);
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    try
    {
      bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
      Assertions.fail("this point must not be reached");
    }
    catch (NotImplementedException ex)
    {
      MatcherAssert.assertThat(ex.getDetail(), Matchers.equalTo("bulk is not supported by this service provider"));
    }
  }

  /**
   * verifies that exceeding the maximum number of operations will cause a {@link BadRequestException}
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  public void testFailIfMaxOperationsIsExceeded(int maxOperations)
  {
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations + 1);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    try
    {
      bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      Assertions.assertEquals("too many operations maximum number of operations is '" + maxOperations + "'",
                              ex.getDetail());
    }
  }

  /**
   * verifies that a {@link BadRequestException} is thrown if the maximum payload size is exceeded
   */
  @Test
  public void testFailIfMaxPayloadIsExceeded()
  {
    final int maxOperations = 10;
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();

    final long maxPayloadSize = bulkRequest.toString().getBytes().length - 1;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(maxPayloadSize);

    try
    {
      bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      Assertions.assertEquals("request body too large with '" + (maxPayloadSize + 1) + "'-bytes "
                              + "maximum payload size is '" + maxPayloadSize + "'",
                              ex.getDetail());
    }
  }

  /**
   * verifies that schema validation is executed on a bulk request
   */
  @Test
  public void testValidateBulkRequestWithSchema()
  {
    final int maxOperations = 1;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    BulkRequest bulkRequest = BulkRequest.builder().build();
    try
    {
      bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      Assertions.assertEquals("the attribute 'urn:ietf:params:scim:api:messages:2.0:BulkRequest:Operations' "
                              + "is required \n\tmutability: 'WRITE_ONLY'\n\treturned: 'NEVER'",
                              ex.getDetail());
    }
  }

  /**
   * verifies that failed post operations do not contain a location
   */
  @Test
  public void testNoLocationOnFailedPostBulkOperation()
  {
    final int maxOperations = 1;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    Mockito.doThrow(new BadRequestException("something bad", null, null))
           .when(userHandler)
           .createResource(Mockito.any());
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(maxOperations, bulkResponse.getBulkResponseOperations().size());
    Assertions.assertFalse(bulkResponse.getBulkResponseOperations().get(0).getLocation().isPresent());
  }

  /**
   * verifies that the location is correctly set if a bulk post request was successful
   */
  @Test
  public void testCorrectLocationOnSucceededBulkPostRequest()
  {
    final int maxOperations = 1;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(maxOperations, bulkResponse.getBulkResponseOperations().size());
    Assertions.assertTrue(bulkResponse.getBulkResponseOperations().get(0).getLocation().isPresent());
    Assertions.assertEquals(1, userHandler.getInMemoryMap().size());
    final String id = userHandler.getInMemoryMap().keySet().iterator().next();
    Assertions.assertEquals(BASE_URI + EndpointPaths.USERS + "/" + id,
                            bulkResponse.getBulkResponseOperations().get(0).getLocation().get());
  }

  /**
   * this test will send a bulk request in which a create group operation is referencing a user create operation
   * with a bulkId and the group operation is executed first. In the result the user must be created and the
   * bulkId reference must be exchanged for the id of the user
   */
  @Test
  public void testResolveBulkIdWithGroupMembers()
  {
    final int maxOperations = 2;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(1);

    String bulkId = createOperations.get(0).getBulkId().get();
    Member member = Member.builder().value("bulkId:" + bulkId).type(ResourceTypeNames.USER).build();
    Group group = Group.builder().displayName("admin").members(Collections.singletonList(member)).build();
    BulkRequestOperation requestOperation = BulkRequestOperation.builder()
                                                                .method(HttpMethod.POST)
                                                                .path(EndpointPaths.GROUPS)
                                                                .data(group.toString())
                                                                .bulkId(UUID.randomUUID().toString())
                                                                .build();
    createOperations.add(0, requestOperation);

    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    log.debug(bulkRequest.toPrettyString());
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    log.warn(bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseOperations.size());
    Assertions.assertEquals(1, groupHandler.getInMemoryMap().size());
    Group createGroup = groupHandler.getInMemoryMap().values().iterator().next();
    Assertions.assertEquals(1, createGroup.getMembers().size());
    User user = userHandler.getInMemoryMap().values().iterator().next();
    Assertions.assertEquals(user.getId().get(), createGroup.getMembers().get(0).getValue().get());
  }

  /**
   * verifies that a request will result in a {@link BadRequestException} if the bulkId is referencing its own
   * resource
   */
  @Test
  public void testBulkIdReferencesItself()
  {
    final int maxOperations = 1;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    List<BulkRequestOperation> createOperations = new ArrayList<>();
    String bulkId = UUID.randomUUID().toString();
    Member member = Member.builder().value("bulkId:" + bulkId).type(ResourceTypeNames.GROUPS).build();
    Group group = Group.builder().displayName("admin").members(Collections.singletonList(member)).build();
    BulkRequestOperation requestOperation = BulkRequestOperation.builder()
                                                                .method(HttpMethod.POST)
                                                                .path(EndpointPaths.GROUPS)
                                                                .data(group.toString())
                                                                .bulkId(bulkId)
                                                                .build();
    createOperations.add(requestOperation);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    Assertions.assertEquals(1, bulkResponse.getBulkResponseOperations().size());
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, bulkResponse.getBulkResponseOperations().get(0).getStatus());
    Assertions.assertEquals(bulkId, bulkResponse.getBulkResponseOperations().get(0).getBulkId().get());
    ErrorResponse errorResponse = bulkResponse.getBulkResponseOperations().get(0).getResponse().get();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
    Assertions.assertEquals("the bulkId '" + bulkId + "' is a self-reference. Self-references will not be resolved",
                            errorResponse.getDetail().get());
    Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, errorResponse.getScimType().get());
  }

  /**
   * verifies that the implementation does also work if two operations reference the same operation with a
   * bulkId
   */
  @Test
  public void testTwoOperationsDoReferenceTheSameOperation()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(1);

    String bulkId = createOperations.get(0).getBulkId().get();
    Member member = Member.builder().value("bulkId:" + bulkId).type(ResourceTypeNames.USER).build();
    Group group = Group.builder().displayName("admin").members(Collections.singletonList(member)).build();
    BulkRequestOperation requestOperation = BulkRequestOperation.builder()
                                                                .method(HttpMethod.POST)
                                                                .path(EndpointPaths.GROUPS)
                                                                .data(group.toString())
                                                                .bulkId(UUID.randomUUID().toString())
                                                                .build();
    Group group2 = JsonHelper.copyResourceToObject(group.deepCopy(), Group.class);
    group2.setDisplayName("root");
    BulkRequestOperation requestOperation2 = BulkRequestOperation.builder()
                                                                 .method(HttpMethod.POST)
                                                                 .path(EndpointPaths.GROUPS)
                                                                 .data(group2.toString())
                                                                 .bulkId(UUID.randomUUID().toString())
                                                                 .build();

    createOperations.add(0, requestOperation);
    createOperations.add(1, requestOperation2);

    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    log.debug(bulkRequest.toPrettyString());
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    log.warn(bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(3, responseOperations.size());
    Assertions.assertEquals(2, groupHandler.getInMemoryMap().size());
    Iterator<Group> groupIterator = groupHandler.getInMemoryMap().values().iterator();
    Group createGroup = groupIterator.next();
    Group createGroup2 = groupIterator.next();
    Assertions.assertEquals(1, createGroup.getMembers().size());
    User user = userHandler.getInMemoryMap().values().iterator().next();
    Assertions.assertEquals(user.getId().get(), createGroup.getMembers().get(0).getValue().get());

    Assertions.assertEquals(1, createGroup2.getMembers().size());
    Assertions.assertEquals(user.getId().get(), createGroup2.getMembers().get(0).getValue().get());
  }

  /**
   * verifies that circular references will cause an error and a {@link HttpStatus#CONFLICT} status code is
   * returned
   */
  @Test
  public void testCircularReference()
  {
    final int maxOperations = 2;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    List<BulkRequestOperation> createOperations = new ArrayList<>();

    String bulkId = UUID.randomUUID().toString();
    String bulkId2 = UUID.randomUUID().toString();

    Member member = Member.builder().value("bulkId:" + bulkId2).type(ResourceTypeNames.USER).build();
    Group group = Group.builder().displayName("admin").members(Collections.singletonList(member)).build();
    BulkRequestOperation requestOperation = BulkRequestOperation.builder()
                                                                .method(HttpMethod.POST)
                                                                .path(EndpointPaths.GROUPS)
                                                                .data(group.toString())
                                                                .bulkId(bulkId)
                                                                .build();
    Group group2 = JsonHelper.copyResourceToObject(group.deepCopy(), Group.class);
    group2.setDisplayName("root");
    group2.getMembers().get(0).setValue("bulkId:" + bulkId);
    BulkRequestOperation requestOperation2 = BulkRequestOperation.builder()
                                                                 .method(HttpMethod.POST)
                                                                 .path(EndpointPaths.GROUPS)
                                                                 .data(group2.toString())
                                                                 .bulkId(bulkId2)
                                                                 .build();
    createOperations.add(requestOperation);
    createOperations.add(requestOperation2);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    log.warn(bulkRequest.toPrettyString());
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Assertions.assertEquals(2, bulkResponse.getBulkResponseOperations().size());
    ErrorResponse firstResponse = bulkResponse.getBulkResponseOperations().get(0).getResponse().get();
    Assertions.assertEquals("the bulkIds '" + bulkId2 + "' and '" + bulkId + "' do form a circular "
                            + "reference that cannot be resolved.",
                            firstResponse.getDetail().get());
    Assertions.assertEquals(HttpStatus.CONFLICT, firstResponse.getHttpStatus());

    ErrorResponse secondResponse = bulkResponse.getBulkResponseOperations().get(1).getResponse().get();
    Assertions.assertEquals("the bulkIds '" + bulkId + "' and '" + bulkId2 + "' do form a circular "
                            + "reference that cannot be resolved.",
                            secondResponse.getDetail().get());
    Assertions.assertEquals(HttpStatus.CONFLICT, secondResponse.getHttpStatus());
  }

  /**
   * this test will verify that bulkIds are also resolved on extensions as the {@link EnterpriseUser}
   */
  @Test
  public void testBulkIdReferenceOnEnterpriseUserManager()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String bulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    User user = User.builder()
                    .userName(username)
                    .enterpriseUser(EnterpriseUser.builder()
                                                  .manager(Manager.builder().value("bulkId:" + bulkId).build())
                                                  .build())
                    .build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(UUID.randomUUID().toString())
                                       .method(HttpMethod.POST)
                                       .path(EndpointPaths.USERS)
                                       .data(user.toString())
                                       .build());
    user = User.builder().userName(UUID.randomUUID().toString()).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(bulkId)
                                       .method(HttpMethod.POST)
                                       .path(EndpointPaths.USERS)
                                       .data(user.toString())
                                       .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString());
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseOperations.size());
    Assertions.assertEquals(HttpStatus.CREATED, responseOperations.get(0).getStatus());
    Assertions.assertEquals(HttpStatus.CREATED, responseOperations.get(1).getStatus());
    Assertions.assertEquals(2, userHandler.getInMemoryMap().size());
    List<User> createdUsers = new ArrayList<>(userHandler.getInMemoryMap().values());
    createdUsers.forEach(createdUser -> log.warn(createdUser.toPrettyString()));
    Assertions.assertTrue(createdUsers.get(1).getEnterpriseUser().isPresent());
    Assertions.assertEquals(createdUsers.get(0).getId().get(),
                            createdUsers.get(1).getEnterpriseUser().get().getManager().get().getValue().get());
  }
}
