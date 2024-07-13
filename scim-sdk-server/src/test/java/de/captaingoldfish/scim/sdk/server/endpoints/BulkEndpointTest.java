package de.captaingoldfish.scim.sdk.server.endpoints;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResponseException;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseGetOperation;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.custom.endpoints.BulkIdReferencesEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.custom.resourcehandler.BulkIdReferencesResourceHandler;
import de.captaingoldfish.scim.sdk.server.custom.resources.BulkIdReferences;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.GroupHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.patch.PatchOperationHandler;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 08.11.2019 - 22:54 <br>
 * <br>
 */
@Slf4j
public class BulkEndpointTest extends AbstractBulkTest implements FileReferences
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
   * a mockito spy to verify the class that have been made on this instance
   */
  private BulkIdReferencesResourceHandler bulkIdReferencesResourceHandler;

  /**
   * a resource type consumer that can be dynamically changed during the test execution
   */
  private Consumer<ResourceType> dynamicResourceTypeConsumer;

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    serviceProvider = ServiceProvider.builder().build();
    userHandler = Mockito.spy(new UserHandlerImpl(true));
    groupHandler = Mockito.spy(new GroupHandlerImpl());
    bulkIdReferencesResourceHandler = Mockito.spy(new BulkIdReferencesResourceHandler());
    ResourceEndpoint resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler),
                                                             new GroupEndpointDefinition(groupHandler),
                                                             new BulkIdReferencesEndpointDefinition(bulkIdReferencesResourceHandler));
    bulkEndpoint = new BulkEndpoint(resourceEndpoint, serviceProvider, resourceEndpoint.getResourceTypeFactory(),
                                    new HashMap<>(), new HashMap<>(),
                                    resourceType -> Optional.ofNullable(dynamicResourceTypeConsumer)
                                                            .ifPresent(consumer -> consumer.accept(resourceType)));
  }

  /**
   * will verify that a user can be created, updated and deleted when using bulk
   */
  @Test
  public void testSendBulkRequest()
  {
    int[] executionCounter = new int[]{0};
    dynamicResourceTypeConsumer = resourceType -> {
      executionCounter[0]++;
      Assertions.assertEquals(EndpointPaths.USERS, resourceType.getEndpoint());
    };

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
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(maxOperations, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).createResource(Mockito.any(), Mockito.notNull());
    Assertions.assertEquals(bulkResponse.getBulkResponseOperations().size(), executionCounter[0]);

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
    scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).updateResource(Mockito.any(), Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).deleteResource(Mockito.any(), Mockito.notNull());

    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    for ( BulkResponseOperation bulkResponseOperation : responseOperations.subList(0, maxOperations - 1) )
    {
      Assertions.assertEquals(HttpMethod.PUT, bulkResponseOperation.getMethod());
      Assertions.assertEquals(HttpStatus.OK, bulkResponseOperation.getStatus());
      Assertions.assertFalse(bulkResponseOperation.getResponse().isPresent());
      Assertions.assertTrue(bulkResponseOperation.getBulkId().isPresent());
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
      Assertions.assertTrue(bulkResponseOperation.getResourceId().isPresent());
    }
  }

  /**
   * will verify that the server does not respond with the resources on bulk-responses if the feature is
   * disabled even if the client explicitly asks for the resources
   */
  @Test
  public void testServerDoesNotAllowResourceToBeReturnedOnBulk()
  {
    final int maxOperations = 10;
    serviceProvider.getBulkConfig().setReturnResourcesEnabled(false);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    List<BulkRequestOperation> operations = new ArrayList<>();
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations, true);
    operations.addAll(createOperations);
    final int failOnErrors = 0;
    BulkRequest bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertFalse(bulkResponseOperation.getResponse().isPresent());
    }
  }

  /**
   * will verify that the created resources are returned on a bulk-response if the client did explicitly ask for
   * the resource to be returned. This works only if the feature is enabled.
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testServerDoesAllowResourceToBeReturnedAndClientAsksForIt(boolean returnResource)
  {
    final int maxOperations = 10;
    serviceProvider.getBulkConfig().setReturnResourcesEnabled(true);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    List<BulkRequestOperation> operations = new ArrayList<>();
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations, returnResource);
    operations.addAll(createOperations);
    final int failOnErrors = 0;
    BulkRequest bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertEquals(returnResource, bulkResponseOperation.getResponse().isPresent());
      if (returnResource)
      {
        User user = bulkResponseOperation.getResponse(User.class).orElse(null);
        Assertions.assertEquals(SchemaUris.USER_URI, user.getSchemas().iterator().next());
      }
    }
  }

  /**
   * will verify that the server does not respond with the resource if the explicit resource type has disabled
   * returning of resources
   */
  @Test
  public void testServerDoesNotReturnBlockedResources()
  {
    final int maxOperations = 10;
    serviceProvider.getBulkConfig().setReturnResourcesEnabled(true);
    serviceProvider.getBulkConfig().setReturnResourcesByDefault(true);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    ResourceType userResourceType = bulkEndpoint.getResourceTypeFactory().getResourceType(EndpointPaths.USERS);
    userResourceType.getFeatures().setDenyReturnResourcesOnBulk(true);

    List<BulkRequestOperation> operations = new ArrayList<>();
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations, true);
    operations.addAll(createOperations);
    final int failOnErrors = 0;
    BulkRequest bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertFalse(bulkResponseOperation.getResponse().isPresent());
    }
  }

  /**
   * will verify that the server does put the resources into the response by default if the configuration is set
   * to true and the resource is not blocked even if the client did not ask for the resource
   */
  @Test
  public void testServerDoesReturnResourcesByDefault()
  {
    final int maxOperations = 10;
    serviceProvider.getBulkConfig().setReturnResourcesByDefault(true);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);

    List<BulkRequestOperation> operations = new ArrayList<>();
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations, null);
    operations.addAll(createOperations);
    final int failOnErrors = 0;
    BulkRequest bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertTrue(bulkResponseOperation.getResponse().isPresent());
    }
  }

  /**
   * verifies that the client can tell the server that the resources should not be returned if the resources are
   * returned by default.
   */
  @Test
  public void testClientCanDenyReturnOfResources()
  {
    final int maxOperations = 10;
    serviceProvider.getBulkConfig().setReturnResourcesByDefault(true);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);

    List<BulkRequestOperation> operations = new ArrayList<>();
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations, false);
    operations.addAll(createOperations);
    final int failOnErrors = 0;
    BulkRequest bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertFalse(bulkResponseOperation.getResponse().isPresent());
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
      bulkEndpoint.bulk(BASE_URI, createOperations.toString(), null);
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      MatcherAssert.assertThat(ex.getDetail(), Matchers.containsString("Document does not have a 'schemas'-attribute"));
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
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;

    int responseSize = bulkResponse.getBulkResponseOperations().size();
    BulkResponseOperation bulkResponseOperation = bulkResponse.getBulkResponseOperations().get(responseSize - 1);
    MatcherAssert.assertThat(bulkResponseOperation.getResponse(ErrorResponse.class).get().getScimException().getClass(),
                             Matchers.typeCompatibleWith(ResponseException.class));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, bulkResponseOperation.getStatus());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST,
                            bulkResponseOperation.getResponse(ErrorResponse.class).get().getStatus());
    MatcherAssert.assertThat(bulkResponseOperation.getResponse(ErrorResponse.class).get().getDetail().get(),
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
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
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
           .createResource(Mockito.any(), Mockito.any());
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder()
                                         .failOnErrors(failOnErrors)
                                         .bulkRequestOperation(createOperations)
                                         .build();
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(bulkRequest.getBulkRequestOperations().size(),
                            bulkResponse.getBulkResponseOperations().size());

    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    // check first operation
    responseOperations.subList(0, responseOperations.size() - 1).forEach(operation -> {
      Assertions.assertTrue(operation.getResponse().isPresent());
      ErrorResponse errorResponse = operation.getResponse(ErrorResponse.class).get();
      MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                               Matchers.typeCompatibleWith(ResponseException.class));
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
      Assertions.assertEquals("something bad", errorResponse.getDetail().get());
    });

    // should be always a single
    final BulkResponseOperation preconditionFailedOperation = responseOperations.get(responseOperations.size() - 1);

    Assertions.assertTrue(preconditionFailedOperation.getResponse().isPresent());
    ErrorResponse errorResponse = preconditionFailedOperation.getResponse(ErrorResponse.class).get();
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(ResponseException.class));
    final String errorMessage = String.format("Operation with bulkId '%s' at iteration '%s' was not handled due to "
                                              + "previous failed precondition",
                                              preconditionFailedOperation.getBulkId().orElse(null),
                                              responseOperations.size());
    Assertions.assertEquals(errorMessage, errorResponse.getDetail().get());

    Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, errorResponse.getHttpStatus());
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
      bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
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
      bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      Assertions.assertEquals("too many operations maximum number of operations is '" + maxOperations + "' but got '"
                              + createOperations.size() + "'",
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
      bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
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
      bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      Assertions.assertEquals("Required 'WRITE_ONLY' attribute "
                              + "'urn:ietf:params:scim:api:messages:2.0:BulkRequest:Operations' is missing",
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
           .createResource(Mockito.any(), Mockito.any());
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
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
    ScimResponse scimResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
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
    serviceProvider.getETagConfig().setSupported(true);
    ResourceType usersResourceType = bulkEndpoint.getResourceTypeFactory().getResourceType(EndpointPaths.USERS);
    usersResourceType.getFeatures().getETagFeature().setEnabled(true);
    ResourceType groupsResourceType = bulkEndpoint.getResourceTypeFactory().getResourceType(EndpointPaths.GROUPS);
    groupsResourceType.getFeatures().getETagFeature().setEnabled(true);

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
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseOperations.size());
    Assertions.assertEquals(1, groupHandler.getInMemoryMap().size(), bulkResponse.toPrettyString());
    Group createGroup = groupHandler.getInMemoryMap().values().iterator().next();
    Assertions.assertEquals(1, createGroup.getMembers().size());
    User user = userHandler.getInMemoryMap().values().iterator().next();
    Assertions.assertEquals(user.getId().get(), createGroup.getMembers().get(0).getValue().get());
    responseOperations.forEach(operation -> {
      Assertions.assertTrue(operation.getResourceId().isPresent());
      Assertions.assertTrue(operation.getVersion().isPresent());
    });
  }

  /**
   * this test is based on a bug found in version 1.15.3 in which the bulk-operations-value was replaced with a
   * previous operation while resolving a bulkId-reference. We will send the following bulk-request:
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:BulkRequest" ],
   *   "Operations" : [ {
   *     "method" : "PATCH",
   *     "path" : "/Groups/d13ae6ec-89ca-4b85-92ba-168742ce210d",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *       "Operations" : [ {
   *         "path" : "members[value eq \"52c912c0-238a-4d23-9694-f9f6a8cc18a2\"]",
   *         "op" : "remove"
   *       } ]
   *     }
   *   }, {
   *     "method" : "DELETE",
   *     "path" : "/Users/52c912c0-238a-4d23-9694-f9f6a8cc18a2"
   *   }, {
   *     "method" : "POST",
   *     "bulkId" : "4dacaf8b-fe57-4552-b5f0-ac10e4fd890d",
   *     "path" : "/Users",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *       "id" : "60949e99-31a6-4b53-9e56-f24f2b104343",
   *       "userName" : "goldfish"
   *     }
   *   }, {
   *     "method" : "PATCH",
   *     "path" : "/Groups/d13ae6ec-89ca-4b85-92ba-168742ce210d",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *       "Operations" : [ {
   *         "op" : "add",
   *         "value" : [ {
   *           "value" : "bulkId:4dacaf8b-fe57-4552-b5f0-ac10e4fd890d"
   *         } ]
   *       } ]
   *     }
   *   } ]
   * }
   * </pre>
   *
   * The bug that was found caused the first patch request to be executed again instead of the second patch
   * request that contains the bulkId-reference. This bug came with the release 1.15.0. In previous releases it
   * was working.
   */
  @Test
  public void testBulkIdResolvesCorrectly()
  {
    final int maxOperations = 4;
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    User chuck = User.builder().id(UUID.randomUUID().toString()).userName("chuck").build();
    Group adminGroup = Group.builder()
                            .id(UUID.randomUUID().toString())
                            .displayName("admin")
                            .members(Arrays.asList(Member.builder().value(chuck.getId().get()).build()))
                            .meta(Meta.builder().created(Instant.now()).lastModified(Instant.now()).build())
                            .build();
    userHandler.getInMemoryMap().put(chuck.getId().get(), chuck);
    groupHandler.getInMemoryMap().put(adminGroup.getId().get(), adminGroup);


    List<BulkRequestOperation> bulkOperations = new ArrayList<>();
    {
      List<PatchRequestOperation> removeChuckMemberOp = new ArrayList<>();
      removeChuckMemberOp.add(PatchRequestOperation.builder()
                                                   .op(PatchOp.REMOVE)
                                                   .path(String.format("members[value eq \"%s\"]", chuck.getId().get()))
                                                   .build());
      bulkOperations.add(BulkRequestOperation.builder()
                                             .method(HttpMethod.PATCH)
                                             .path(EndpointPaths.GROUPS + "/" + adminGroup.getId().get())
                                             .data(PatchOpRequest.builder()
                                                                 .operations(removeChuckMemberOp)
                                                                 .build()
                                                                 .toString())
                                             .build());
    }
    {
      bulkOperations.add(BulkRequestOperation.builder()
                                             .method(HttpMethod.DELETE)
                                             .path(EndpointPaths.USERS + "/" + chuck.getId().get())
                                             .build());
    }
    User goldfish = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").build();
    final String addUserBulkId = UUID.randomUUID().toString();
    {
      bulkOperations.add(BulkRequestOperation.builder()
                                             .bulkId(addUserBulkId)
                                             .method(HttpMethod.POST)
                                             .path(EndpointPaths.USERS)
                                             .data(goldfish.toString())
                                             .build());
    }
    {
      List<PatchRequestOperation> addGoldfishMember = new ArrayList<>();
      addGoldfishMember.add(PatchRequestOperation.builder()
                                                 .op(PatchOp.ADD)
                                                 .path(AttributeNames.RFC7643.MEMBERS)
                                                 .valueNode(Member.builder()
                                                                  .value(String.format("%s:%s",
                                                                                       AttributeNames.RFC7643.BULK_ID,
                                                                                       addUserBulkId))
                                                                  .build())
                                                 .build());
      bulkOperations.add(BulkRequestOperation.builder()
                                             .method(HttpMethod.PATCH)
                                             .path(EndpointPaths.GROUPS + "/" + adminGroup.getId().get())
                                             .data(PatchOpRequest.builder()
                                                                 .operations(addGoldfishMember)
                                                                 .build()
                                                                 .toString())
                                             .build());
    }

    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(bulkOperations).build();

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);

    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());

    Assertions.assertEquals(1, userHandler.getInMemoryMap().size());
    Assertions.assertEquals("goldfish", userHandler.getInMemoryMap().values().iterator().next().getUserName().get());
    goldfish = userHandler.getInMemoryMap().values().iterator().next();

    adminGroup = groupHandler.getInMemoryMap().get(adminGroup.getId().get());
    Assertions.assertEquals(1, adminGroup.getMembers().size());
    Assertions.assertEquals(goldfish.getId().get(), adminGroup.getMembers().get(0).getValue().get());
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
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(1, bulkResponse.getBulkResponseOperations().size());
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, bulkResponse.getBulkResponseOperations().get(0).getStatus());
    Assertions.assertEquals(bulkId, bulkResponse.getBulkResponseOperations().get(0).getBulkId().get());
    ErrorResponse errorResponse = bulkResponse.getBulkResponseOperations()
                                              .get(0)
                                              .getResponse(ErrorResponse.class)
                                              .get();
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
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(3, responseOperations.size());
    Assertions.assertEquals(2, groupHandler.getInMemoryMap().size(), bulkResponse.toPrettyString());
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
   * verifies that a bulkId-reference is resolved even within the uri
   */
  @Test
  public void testResolveBulkIdReferenceWithinUri()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    List<BulkRequestOperation> requestOperationList = getCreateUserBulkOperations(1);

    String bulkId = requestOperationList.get(0).getBulkId().get();
    User user = User.builder().userName(UUID.randomUUID().toString()).build();
    String resourcePath = EndpointPaths.USERS + "/" + AttributeNames.RFC7643.BULK_ID + ":" + bulkId;
    BulkRequestOperation requestOperation = BulkRequestOperation.builder()
                                                                .method(HttpMethod.PUT)
                                                                .path(resourcePath)
                                                                .data(user.toString())
                                                                .bulkId(UUID.randomUUID().toString())
                                                                .build();
    requestOperationList.add(0, requestOperation);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(requestOperationList).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseList = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseList.size());
    Assertions.assertEquals(HttpStatus.CREATED, responseList.get(0).getStatus(), bulkResponse.toPrettyString());
    Assertions.assertEquals(bulkId, responseList.get(0).getBulkId().get());
    Assertions.assertEquals(HttpStatus.OK, responseList.get(1).getStatus());
    Assertions.assertEquals(requestOperation.getBulkId().get(), responseList.get(1).getBulkId().get());
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
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Assertions.assertEquals(2, bulkResponse.getBulkResponseOperations().size());
    ErrorResponse firstResponse = bulkResponse.getBulkResponseOperations()
                                              .get(0)
                                              .getResponse(ErrorResponse.class)
                                              .get();
    MatcherAssert.assertThat(firstResponse.getDetail().get(), Matchers.containsString(bulkId));
    MatcherAssert.assertThat(firstResponse.getDetail().get(), Matchers.containsString(bulkId2));
    MatcherAssert.assertThat(bulkResponse.toPrettyString(),
                             firstResponse.getDetail().get(),
                             Matchers.matchesPattern("the bulkIds '.*?' and '.*?' form a direct or indirect "
                                                     + "circular reference that cannot be resolved."));

    Assertions.assertEquals(HttpStatus.CONFLICT, firstResponse.getHttpStatus());

    ErrorResponse secondResponse = bulkResponse.getBulkResponseOperations()
                                               .get(1)
                                               .getResponse(ErrorResponse.class)
                                               .get();
    Assertions.assertEquals(String.format("the operation failed because the following bulkId-references could not be "
                                          + "resolved [bulkId:%s]",
                                          bulkId2),
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
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseOperations.size());
    Assertions.assertEquals(HttpStatus.CREATED, responseOperations.get(0).getStatus(), bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.CREATED, responseOperations.get(1).getStatus());
    Assertions.assertEquals(2, userHandler.getInMemoryMap().size());
    List<User> createdUsers = new ArrayList<>(userHandler.getInMemoryMap().values());
    Assertions.assertEquals(1,
                            (int)createdUsers.stream()
                                             .filter(jsonNodes -> jsonNodes.getEnterpriseUser().isPresent())
                                             .count());
    User normalUser = createdUsers.stream()
                                  .filter(jsonNodes -> !jsonNodes.getEnterpriseUser().isPresent())
                                  .findAny()
                                  .get();
    User enterpriseUser = createdUsers.stream()
                                      .filter(jsonNodes -> jsonNodes.getEnterpriseUser().isPresent())
                                      .findAny()
                                      .get();
    Assertions.assertEquals(normalUser.getId().get(),
                            enterpriseUser.getEnterpriseUser().get().getManager().get().getValue().get());
  }

  /**
   * verifies that a dedicated error message is given if the given bulkId-reference is not resolvable
   */
  @Test
  public void testBulkIdReferenceDoesNotExist()
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
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus(), bulkResponse.toPrettyString());
    List<BulkResponseOperation> responseOperationList = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(1, responseOperationList.size(), bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.CONFLICT,
                            responseOperationList.get(0).getStatus(),
                            bulkResponse.toPrettyString());
    Assertions.assertTrue(responseOperationList.get(0).getResponse(ErrorResponse.class).isPresent(),
                          bulkResponse.toPrettyString());
    Assertions.assertTrue(responseOperationList.get(0).getResponse(ErrorResponse.class).get().getDetail().isPresent(),
                          bulkResponse.toPrettyString());
    Assertions.assertEquals("the operation failed because the following bulkId-references could not be "
                            + "resolved [bulkId:" + bulkId + "]",
                            responseOperationList.get(0).getResponse(ErrorResponse.class).get().getDetail().get(),
                            bulkResponse.toPrettyString());
  }

  /**
   * does the same as {@link #testBulkIdReferenceDoesNotExist()} but in this case the bulkId-reference is on the
   * uri
   */
  @Test
  public void testBulkIdReferenceDoesNotExistOnUri()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String bulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    User user = User.builder().userName(username).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(UUID.randomUUID().toString())
                                       .method(HttpMethod.PUT)
                                       .path(EndpointPaths.USERS + "/" + AttributeNames.RFC7643.BULK_ID + ":" + bulkId)
                                       .data(user.toString())
                                       .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus(), bulkResponse.toPrettyString());
    List<BulkResponseOperation> responseOperationList = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(1, responseOperationList.size(), bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.CONFLICT,
                            responseOperationList.get(0).getStatus(),
                            bulkResponse.toPrettyString());
    Assertions.assertTrue(responseOperationList.get(0).getResponse(ErrorResponse.class).isPresent(),
                          bulkResponse.toPrettyString());
    Assertions.assertTrue(responseOperationList.get(0).getResponse(ErrorResponse.class).get().getDetail().isPresent(),
                          bulkResponse.toPrettyString());
    Assertions.assertEquals("the operation failed because the following bulkId-references could not be "
                            + "resolved [bulkId:" + bulkId + "]",
                            responseOperationList.get(0).getResponse(ErrorResponse.class).get().getDetail().get(),
                            bulkResponse.toPrettyString());
  }

  /**
   * verifies that a {@link BadRequestException} is thrown if a bulkId-reference is malformed
   */
  @Test
  public void testInvalidBulkIdReference()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String bulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    final String invalidBulkId = AttributeNames.RFC7643.BULK_ID + ":" + bulkId + ":helloWorld";
    User user = User.builder().userName(username).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(UUID.randomUUID().toString())
                                       .method(HttpMethod.PUT)
                                       .path(EndpointPaths.USERS + "/" + invalidBulkId)
                                       .data(user.toString())
                                       .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus(), bulkResponse.toPrettyString());
    List<BulkResponseOperation> responseOperationList = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(1, responseOperationList.size(), bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST,
                            responseOperationList.get(0).getStatus(),
                            bulkResponse.toPrettyString());
    Assertions.assertTrue(responseOperationList.get(0).getResponse(ErrorResponse.class).isPresent(),
                          bulkResponse.toPrettyString());
    Assertions.assertTrue(responseOperationList.get(0).getResponse(ErrorResponse.class).get().getDetail().isPresent(),
                          bulkResponse.toPrettyString());
    Assertions.assertEquals("the value '" + invalidBulkId + "' is not a valid bulkId reference",
                            responseOperationList.get(0).getResponse(ErrorResponse.class).get().getDetail().get(),
                            bulkResponse.toPrettyString());
  }

  /**
   * verifies that a {@link BadRequestException} is thrown if a bulkId-reference is malformed on the uri
   */
  @Test
  public void testInvalidBulkIdReferenceOnUri()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String bulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    final String invalidBulkId = "bulkId:" + bulkId + ":helloWorld";
    User user = User.builder()
                    .userName(username)
                    .enterpriseUser(EnterpriseUser.builder()
                                                  .manager(Manager.builder().value(invalidBulkId).build())
                                                  .build())
                    .build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(UUID.randomUUID().toString())
                                       .method(HttpMethod.POST)
                                       .path(EndpointPaths.USERS)
                                       .data(user.toString())
                                       .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus(), bulkResponse.toPrettyString());
    List<BulkResponseOperation> responseOperationList = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(1, responseOperationList.size(), bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST,
                            responseOperationList.get(0).getStatus(),
                            bulkResponse.toPrettyString());
    Assertions.assertTrue(responseOperationList.get(0).getResponse(ErrorResponse.class).isPresent(),
                          bulkResponse.toPrettyString());
    Assertions.assertTrue(responseOperationList.get(0).getResponse(ErrorResponse.class).get().getDetail().isPresent(),
                          bulkResponse.toPrettyString());
    Assertions.assertEquals("the value '" + invalidBulkId + "' is not a valid bulkId reference",
                            responseOperationList.get(0).getResponse(ErrorResponse.class).get().getDetail().get(),
                            bulkResponse.toPrettyString());
  }

  /**
   * verifies that bulkIds in a patch-add operation without a path are correctly resolved
   */
  @Test
  public void testBulkIdReferenceOnPatchAddNoPath()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String bulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    final String id = UUID.randomUUID().toString();
    User user = User.builder()
                    .id(id)
                    .userName(username)
                    .meta(Meta.builder().created(LocalDateTime.now()).lastModified(LocalDateTime.now()).build())
                    .build();
    userHandler.getInMemoryMap().put(id, user);

    List<PatchRequestOperation> requestOperations = new ArrayList<>();
    User patchUserRep = User.builder()
                            .enterpriseUser(EnterpriseUser.builder()
                                                          .manager(Manager.builder().value("bulkId:" + bulkId).build())
                                                          .build())
                            .build();
    requestOperations.add(PatchRequestOperation.builder()
                                               .op(PatchOp.ADD)
                                               .values(Arrays.asList(patchUserRep.toString()))
                                               .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(requestOperations).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(UUID.randomUUID().toString())
                                       .method(HttpMethod.PATCH)
                                       .path(EndpointPaths.USERS + "/" + id)
                                       .data(patchOpRequest.toString())
                                       .build());

    User patchedUser = User.builder().userName(UUID.randomUUID().toString()).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(bulkId)
                                       .method(HttpMethod.POST)
                                       .path(EndpointPaths.USERS)
                                       .data(patchedUser.toString())
                                       .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseOperations.size());
    Assertions.assertEquals(HttpStatus.CREATED, responseOperations.get(0).getStatus(), bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.OK, responseOperations.get(1).getStatus());

    Assertions.assertEquals(2, userHandler.getInMemoryMap().size());
    User newCreatedUser = userHandler.getInMemoryMap()
                                     .values()
                                     .stream()
                                     .filter(u -> !u.getEnterpriseUser().isPresent())
                                     .findAny()
                                     .get();
    user = userHandler.getInMemoryMap()
                      .values()
                      .stream()
                      .filter(u -> u.getEnterpriseUser().isPresent())
                      .findAny()
                      .get();
    Assertions.assertEquals(newCreatedUser.getId().get(),
                            user.getEnterpriseUser().get().getManager().get().getValue().get());
  }

  /**
   * verifies that circular references will cause a conflict in patch requests
   */
  @Test
  public void testBulkIdReferenceOnPatchAddNoPathWithCircularReference()
  {
    final int maxOperations = 2;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String createBulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    final String id = UUID.randomUUID().toString();
    User user = User.builder()
                    .id(id)
                    .userName(username)
                    .meta(Meta.builder().created(LocalDateTime.now()).lastModified(LocalDateTime.now()).build())
                    .build();
    userHandler.getInMemoryMap().put(id, user);

    List<PatchRequestOperation> requestOperations = new ArrayList<>();
    User patchUserRep = User.builder()
                            .enterpriseUser(EnterpriseUser.builder()
                                                          .manager(Manager.builder()
                                                                          .value("bulkId:" + createBulkId)
                                                                          .build())
                                                          .build())
                            .build();
    requestOperations.add(PatchRequestOperation.builder()
                                               .op(PatchOp.ADD)
                                               .values(Arrays.asList(patchUserRep.toString()))
                                               .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(requestOperations).build();
    String patchBulkId = UUID.randomUUID().toString();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(patchBulkId)
                                       .method(HttpMethod.PATCH)
                                       .path(EndpointPaths.USERS + "/" + id)
                                       .data(patchOpRequest.toString())
                                       .build());

    Group patchedGroup = Group.builder()
                              .displayName("admin")
                              .members(Arrays.asList(Member.builder()
                                                           .type("User")
                                                           .value("bulkId:" + patchBulkId)
                                                           .build()))
                              .build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(createBulkId)
                                       .method(HttpMethod.POST)
                                       .path(EndpointPaths.GROUPS)
                                       .data(patchedGroup.toString())
                                       .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseOperations.size());
    Assertions.assertEquals(HttpStatus.CONFLICT, responseOperations.get(0).getStatus());

    ErrorResponse errorResponse = responseOperations.get(0).getResponse(ErrorResponse.class).get();
    MatcherAssert.assertThat(errorResponse.getDetail().get(), Matchers.containsString(createBulkId));
    MatcherAssert.assertThat(errorResponse.getDetail().get(), Matchers.containsString(patchBulkId));
    MatcherAssert.assertThat(errorResponse.getDetail().get(),
                             Matchers.matchesPattern("the bulkIds '.*?' and '.*?' form a direct or indirect circular "
                                                     + "reference that cannot be resolved."));

    Assertions.assertEquals(HttpStatus.CONFLICT, responseOperations.get(1).getStatus());

    ErrorResponse errorResponse2 = responseOperations.get(1).getResponse(ErrorResponse.class).get();
    Assertions.assertEquals(String.format("the operation failed because the following bulkId-references could not be "
                                          + "resolved [bulkId:%s]",
                                          createBulkId),
                            errorResponse2.getDetail().get());
    Assertions.assertEquals(1, userHandler.getInMemoryMap().size());
  }

  /**
   * verifies that a self references will cause a {@link BadRequestException} in case of patch
   */
  @Test
  public void testBulkIdReferenceOnPatchAddNoPathWithSelfReference()
  {
    final int maxOperations = 1;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String bulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    final String id = UUID.randomUUID().toString();
    User user = User.builder()
                    .id(id)
                    .userName(username)
                    .meta(Meta.builder().created(LocalDateTime.now()).lastModified(LocalDateTime.now()).build())
                    .build();
    userHandler.getInMemoryMap().put(id, user);

    List<PatchRequestOperation> requestOperations = new ArrayList<>();
    User patchUserRep = User.builder()
                            .enterpriseUser(EnterpriseUser.builder()
                                                          .manager(Manager.builder().value("bulkId:" + bulkId).build())
                                                          .build())
                            .build();
    requestOperations.add(PatchRequestOperation.builder()
                                               .op(PatchOp.ADD)
                                               .values(Arrays.asList(patchUserRep.toString()))
                                               .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(requestOperations).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(bulkId)
                                       .method(HttpMethod.PATCH)
                                       .path(EndpointPaths.USERS + "/" + id)
                                       .data(patchOpRequest.toString())
                                       .build());

    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(1, responseOperations.size());
    BulkResponseOperation responseOperation = responseOperations.get(0);
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseOperation.getStatus());
    Assertions.assertTrue(responseOperation.getResponse().isPresent());
    Assertions.assertEquals("the bulkId '" + bulkId + "' is a self-reference. Self-references will not be resolved",
                            responseOperation.getResponse(ErrorResponse.class).get().getDetail().get());
  }

  /**
   * verifies that bulkIds in a patch-add operation with a path and a subattribute are correctly resolved. The
   * used path in this test looks like this: manager[displayname eq "chuck"].value
   */
  @Test
  public void testBulkIdReferenceOnPatchAddWithPathAndSubAttribute()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String bulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    final String id = UUID.randomUUID().toString();
    User user = User.builder()
                    .id(id)
                    .userName(username)
                    .enterpriseUser(EnterpriseUser.builder()
                                                  .manager(Manager.builder()
                                                                  .value("123456")
                                                                  .displayName("chuck")
                                                                  .build())
                                                  .build())
                    .meta(Meta.builder().created(LocalDateTime.now()).lastModified(LocalDateTime.now()).build())
                    .build();
    userHandler.getInMemoryMap().put(id, user);

    List<PatchRequestOperation> requestOperations = new ArrayList<>();
    requestOperations.add(PatchRequestOperation.builder()
                                               .op(PatchOp.ADD)
                                               .path("manager[displayname eq \"chuck\"].value")
                                               .values(Arrays.asList(AttributeNames.RFC7643.BULK_ID + ":" + bulkId))
                                               .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(requestOperations).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(UUID.randomUUID().toString())
                                       .method(HttpMethod.PATCH)
                                       .path(EndpointPaths.USERS + "/" + id)
                                       .data(patchOpRequest.toString())
                                       .build());

    User patchedUser = User.builder().userName(UUID.randomUUID().toString()).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(bulkId)
                                       .method(HttpMethod.POST)
                                       .path(EndpointPaths.USERS)
                                       .data(patchedUser.toString())
                                       .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseOperations.size());
    Assertions.assertEquals(HttpStatus.CREATED, responseOperations.get(0).getStatus(), bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.OK, responseOperations.get(1).getStatus());
    Assertions.assertEquals(2, userHandler.getInMemoryMap().size());
    User patched = userHandler.getInMemoryMap().get(id);
    User referenced = userHandler.getInMemoryMap()
                                 .values()
                                 .stream()
                                 .filter(u -> !u.getId().get().equals(id))
                                 .findAny()
                                 .get();
    Assertions.assertEquals(referenced.getId().get(),
                            patched.getEnterpriseUser().get().getManager().get().getValue().get());
  }

  /**
   * verifies that bulkIds in a patch-add operation with a path to a complex attribute are correctly resolved.
   * The used path in this test looks like this: manager[displayname eq "chuck"]
   */
  @Test
  public void testBulkIdReferenceOnPatchAddWithPathOnComplexAttribute()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String bulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    final String id = UUID.randomUUID().toString();
    User user = User.builder()
                    .id(id)
                    .userName(username)
                    .enterpriseUser(EnterpriseUser.builder()
                                                  .manager(Manager.builder()
                                                                  .value("123456")
                                                                  .displayName("chuck")
                                                                  .build())
                                                  .build())
                    .meta(Meta.builder().created(LocalDateTime.now()).lastModified(LocalDateTime.now()).build())
                    .build();
    userHandler.getInMemoryMap().put(id, user);

    List<PatchRequestOperation> requestOperations = new ArrayList<>();
    String bulkIdReference = AttributeNames.RFC7643.BULK_ID + ":" + bulkId;
    Manager manager = Manager.builder().value(bulkIdReference).build();
    requestOperations.add(PatchRequestOperation.builder()
                                               .op(PatchOp.REPLACE)
                                               .path("manager[displayname eq \"chuck\"]")
                                               .values(Arrays.asList(manager.toString()))
                                               .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(requestOperations).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(UUID.randomUUID().toString())
                                       .method(HttpMethod.PATCH)
                                       .path(EndpointPaths.USERS + "/" + id)
                                       .data(patchOpRequest.toString())
                                       .build());

    User patchedUser = User.builder().userName(UUID.randomUUID().toString()).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(bulkId)
                                       .method(HttpMethod.POST)
                                       .path(EndpointPaths.USERS)
                                       .data(patchedUser.toString())
                                       .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseOperations.size());
    Assertions.assertEquals(HttpStatus.CREATED, responseOperations.get(0).getStatus(), bulkResponse.toPrettyString());
    Assertions.assertEquals(HttpStatus.OK, responseOperations.get(1).getStatus());
    Assertions.assertEquals(2, userHandler.getInMemoryMap().size());
    User patched = userHandler.getInMemoryMap().get(id);
    User referenced = userHandler.getInMemoryMap()
                                 .values()
                                 .stream()
                                 .filter(u -> !u.getId().get().equals(id))
                                 .findAny()
                                 .get();
    Assertions.assertEquals(referenced.getId().get(),
                            patched.getEnterpriseUser().get().getManager().get().getValue().get());
  }

  /**
   * verifies that a dedicated error message is thrown if the patch operation created an erroneous resource. The
   * patch operation in this test is built like this:
   *
   * <pre>
   *   {
   *     "method" : "PATCH",
   *     "bulkId" : "ba2eddfe-c8fe-4c49-b536-402fa88885eb",
   *     "path" : "/Users/269ed3f9-4f1e-4b85-b19e-13b33da79baa",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *       "Operations" : [ {
   *         "path" : "manager[displayname eq \"chuck\"]",
   *         "op" : "replace",
   *         "value" : "{\"manager\":{\"value\":\"05d3d1f1-e6e3-4ea9-b674-af1422033d03\"}}"
   *       } ]
   *     }
   *   }
   * </pre>
   *
   * but the correct representation would be like this:
   *
   * <pre>
   *   {
   *     "method" : "PATCH",
   *     "bulkId" : "ba2eddfe-c8fe-4c49-b536-402fa88885eb",
   *     "path" : "/Users/269ed3f9-4f1e-4b85-b19e-13b33da79baa",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *       "Operations" : [ {
   *         "path" : "manager[displayname eq \"chuck\"]",
   *         "op" : "replace",
   *         "value" : "{\"value\":\"05d3d1f1-e6e3-4ea9-b674-af1422033d03\"}"
   *       } ]
   *     }
   *   }
   * </pre>
   */
  @Test
  public void testBulkIdReferenceOnPatchAddWithPathOnComplexAttributeWithErroneousType()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    final String id = UUID.randomUUID().toString();
    User user = User.builder()
                    .id(id)
                    .userName(username)
                    .enterpriseUser(EnterpriseUser.builder()
                                                  .manager(Manager.builder()
                                                                  .value("123456")
                                                                  .displayName("chuck")
                                                                  .build())
                                                  .build())
                    .meta(Meta.builder().created(LocalDateTime.now()).lastModified(LocalDateTime.now()).build())
                    .build();
    userHandler.getInMemoryMap().put(id, user);

    List<PatchRequestOperation> requestOperations = new ArrayList<>();
    EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                  .manager(Manager.builder()
                                                                  .value(UUID.randomUUID().toString())
                                                                  .build())
                                                  .build();
    requestOperations.add(PatchRequestOperation.builder()
                                               .op(PatchOp.REPLACE)
                                               .path("manager[displayname eq \"chuck\"]")
                                               .values(Arrays.asList(enterpriseUser.toString()))
                                               .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(requestOperations).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(UUID.randomUUID().toString())
                                       .method(HttpMethod.PATCH)
                                       .path(EndpointPaths.USERS + "/" + id)
                                       .data(patchOpRequest.toString())
                                       .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    log.warn(bulkRequest.toPrettyString());
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(1, responseOperations.size());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST,
                            responseOperations.get(0).getStatus(),
                            bulkResponse.toPrettyString());
    ErrorResponse errorResponse = responseOperations.get(0).getResponse(ErrorResponse.class).get();
    String expectedMessage = "Required 'READ_WRITE' attribute "
                             + "'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value' is missing";
    MatcherAssert.assertThat(errorResponse.getDetail().get(), Matchers.startsWith(expectedMessage));
    MatcherAssert.assertThat(errorResponse.getErrorMessages(), Matchers.empty());
    Assertions.assertEquals(1, errorResponse.getFieldErrors().size());
    List<String> managerValueErrors = errorResponse.getFieldErrors().get("manager.value");
    MatcherAssert.assertThat(managerValueErrors, Matchers.containsInAnyOrder(expectedMessage));
  }

  /**
   * will verify that the version-attribute in a
   * {@link de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation} is treated as an If-Match http
   * header
   */
  @Test
  public void testSetVersionOnBulkRequest()
  {
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(Integer.MAX_VALUE);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    serviceProvider.getETagConfig().setSupported(true);
    ResourceType userResourceType = bulkEndpoint.getResourceTypeFactory()
                                                .getResourceTypeByName(ResourceTypeNames.USER)
                                                .get();
    userResourceType.getFeatures().getETagFeature().setEnabled(true);

    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).version("123456").build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    User newUser = JsonHelper.copyResourceToObject(user, User.class);
    newUser.setUserType("workaholic");
    BulkRequestOperation operation = BulkRequestOperation.builder()
                                                         .method(HttpMethod.PUT)
                                                         .path(EndpointPaths.USERS + "/" + id)
                                                         .data(newUser.toString())
                                                         .version(ETag.builder().tag("unknown").build())
                                                         .build();
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(Collections.singletonList(operation)).build();
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    List<BulkResponseOperation> responses = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(1, responses.size());
    BulkResponseOperation responseOperation = responses.get(0);
    Assertions.assertTrue(responseOperation.getResponse().isPresent(), bulkResponse.toPrettyString());
    ErrorResponse errorResponse = responseOperation.getResponse(ErrorResponse.class).get();
    Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, errorResponse.getHttpStatus());
    Assertions.assertEquals("eTag status of resource has changed. Current value is: W/\"123456\"",
                            errorResponse.getDetail().get());
  }

  /**
   * this test will verify that the request is rejected if several bulk request operations do have the same
   * bulkId
   */
  @Test
  public void testRejectDuplicateBulkIds()
  {
    final int maxOperations = 3;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);

    final String bulkId = UUID.randomUUID().toString();

    List<BulkRequestOperation> operations = new ArrayList<>();
    final String username = UUID.randomUUID().toString();
    User user = User.builder().userName(username).build();
    operations.add(BulkRequestOperation.builder()
                                       .bulkId(bulkId)
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
    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    Assertions.assertEquals(2, responseOperations.size());
    Assertions.assertEquals(HttpStatus.CREATED, responseOperations.get(0).getStatus());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseOperations.get(1).getStatus());
    ErrorResponse errorResponse = responseOperations.get(1).getResponse(ErrorResponse.class).get();
    Assertions.assertEquals(String.format("Found duplicate %s '%s' in bulk request operations",
                                          AttributeNames.RFC7643.BULK_ID,
                                          bulkId),
                            errorResponse.getDetail().get());
  }

  /**
   * this test will verify that even a bad created bulk-request where the bulk operations need to be checked
   * several times will be successfully resolved. Here is an example of such a bulk request:
   *
   * <pre>
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:BulkRequest"
   *     ],
   *     "Operations": [
   *         {
   *             "method": "POST",
   *             "bulkId": "1",
   *             "path": "/Users",
   *             "data": {
   *                 "schemas": [
   *                     "urn:ietf:params:scim:schemas:core:2.0:User",
   *                     "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
   *                 ],
   *                 "userName": "goldfish",
   *                 "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *                     "manager": {
   *                         "value": "bulkId:3"
   *                     }
   *                 }
   *             }
   *         },
   *         {
   *             "method": "POST",
   *             "bulkId": "2",
   *             "path": "/Groups",
   *             "data": {
   *                 "schemas": [
   *                     "urn:ietf:params:scim:schemas:core:2.0:Group"
   *                 ],
   *                 "displayName": "admin",
   *                 "members": [
   *                     {
   *                         "value": "bulkId:1",
   *                         "type": "Group"
   *                     },
   *                     {
   *                         "value": "bulkId:3",
   *                         "type": "User"
   *                     },
   *                     {
   *                         "value": "bulkId:4",
   *                         "type": "Group"
   *                     }
   *                 ]
   *             }
   *         },
   *         {
   *             "method": "POST",
   *             "bulkId": "3",
   *             "path": "/Users",
   *             "data": {
   *                 "schemas": [
   *                     "urn:ietf:params:scim:schemas:core:2.0:User"
   *                 ],
   *                 "userName": "chuck"
   *             }
   *         },
   *         {
   *             "method": "POST",
   *             "bulkId": "4",
   *             "path": "/Groups",
   *             "data": {
   *                 "schemas": [
   *                     "urn:ietf:params:scim:schemas:core:2.0:Group"
   *                 ],
   *                 "displayName": "manager",
   *                 "members": [
   *                     {
   *                         "value": "bulkId:1",
   *                         "type": "User"
   *                     },
   *                     {
   *                         "value": "bulkId:3",
   *                         "type": "Group"
   *                     }
   *                 ]
   *             }
   *         }
   *     ]
   * }
   * </pre>
   *
   * what will happen?
   * <ol>
   * <li>bulkId:1 cannot be resolved for reference to bulkId:3 and will be pushed to the end of line</li>
   * <li>bulkId:2 cannot be resolved for reference to bulkId:(1,3,4) and will be pushed to the end of line</li>
   * <li>bulkId:3 will be successfully resolved</li>
   * <li>bulkId:4 will be only partially resolved with bulkId:3 and reference to bulkId:1 cannot be resolved so
   * it will be pushed to the end of line</li>
   * <li>bulkId:1 is successfully resolved</li>
   * <li>bulkId:2 cannot be resolved for reference to bulkId:4 and will be pushed to the end of line</li>
   * <li>bulkId:4 is successfully resolved</li>
   * <li>bulkId:2 is successfully resolved</li>
   * </ol>
   * the operation with bulkId:2 is going 3 times through the execution in order to be completely resolved and
   * this test makes sure that even such circumstances will be resolved successfully
   */
  @Test
  public void testResolveWorstPossibleBulkIdReferenceSetup()
  {
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(10);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);

    String worstPossibleBulkRequestString = readResourceFile(WORST_POSSIBLE_BULK_REQUEST);
    BulkRequest bulkRequest = JsonHelper.readJsonDocument(worstPossibleBulkRequestString, BulkRequest.class);

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertEquals(HttpStatus.CREATED, bulkResponseOperation.getStatus(), bulkResponse.toPrettyString());
    }
  }

  /**
   * this test will use a large bulkId ensemble on a lot of different types that must be successfully resolved
   */
  @Test
  public void testResolveLargeComplexBulkIdEnsemble()
  {
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(20);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    serviceProvider.getBulkConfig().setReturnResourcesEnabled(true);

    String worstPossibleBulkRequestString = readResourceFile(BULK_ID_REFERENCE_RESOURCE_ENSEMBLE);
    BulkRequest bulkRequest = JsonHelper.readJsonDocument(worstPossibleBulkRequestString, BulkRequest.class);

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertEquals(HttpStatus.CREATED, bulkResponseOperation.getStatus(), bulkResponse.toPrettyString());
    }
    BulkResponseOperation responseOperation = bulkResponse.getByBulkId("1").get();
    BulkIdReferences bulkIdReferences = responseOperation.getResponse(BulkIdReferences.class).get();

    Set<String> resolvedIds = new HashSet<>();
    resolvedIds.add(bulkIdReferences.getUserId().get());
    resolvedIds.addAll(bulkIdReferences.getUserIdList());
    resolvedIds.add(bulkIdReferences.getMember().get().getUserId().get());
    resolvedIds.addAll(bulkIdReferences.getMember().get().getUserIdList());

    for ( BulkIdReferences.MemberList memberList : bulkIdReferences.getMemberList() )
    {
      resolvedIds.add(memberList.getGroupId().get());
      resolvedIds.addAll(memberList.getGroupIdList());
    }
    Assertions.assertEquals(bulkRequest.getBulkRequestOperations().size() - 1, resolvedIds.size());
    Assertions.assertTrue(resolvedIds.stream().noneMatch(id -> {
      return id.startsWith(String.format("%s:", AttributeNames.RFC7643.BULK_ID));
    }));
    Assertions.assertTrue(resolvedIds.stream().allMatch(this::isUuid));
  }

  /**
   * verifies that simple bulkId references are also correctly resolved within patch requests without a path.
   */
  @Test
  public void testResolveSimpleBulkIdsInPatchRequestWithoutAPath()
  {
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(20);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    serviceProvider.getBulkConfig().setReturnResourcesEnabled(true);

    BulkRequest bulkRequest = JsonHelper.loadJsonDocument(BULK_ID_REFERENCE_PATCH_NO_PATH_ENSEMBLE, BulkRequest.class);

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());

    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      if ("3".equals(bulkResponseOperation.getBulkId().get()))
      {
        Assertions.assertEquals(HttpStatus.OK, bulkResponseOperation.getStatus(), bulkResponse.toPrettyString());
      }
      else
      {
        Assertions.assertEquals(HttpStatus.CREATED, bulkResponseOperation.getStatus(), bulkResponse.toPrettyString());
      }
    }
    BulkResponseOperation responseOperation = bulkResponse.getByBulkId("3").get();
    BulkIdReferences bulkIdReferences = responseOperation.getResponse(BulkIdReferences.class).get();

    Set<String> resolvedIds = new HashSet<>();
    resolvedIds.add(bulkIdReferences.getUserId().get());
    resolvedIds.addAll(bulkIdReferences.getUserIdList());
    resolvedIds.add(bulkIdReferences.getMember().get().getUserId().get());
    resolvedIds.addAll(bulkIdReferences.getMember().get().getUserIdList());

    for ( BulkIdReferences.MemberList memberList : bulkIdReferences.getMemberList() )
    {
      resolvedIds.add(memberList.getGroupId().get());
      resolvedIds.addAll(memberList.getGroupIdList());
    }
    Assertions.assertEquals(12, resolvedIds.size());
    Assertions.assertTrue(resolvedIds.stream().noneMatch(id -> {
      return id.startsWith(String.format("%s:", AttributeNames.RFC7643.BULK_ID));
    }));
    Assertions.assertTrue(resolvedIds.stream().allMatch(this::isUuid));
  }

  /**
   * verifies that no error occurs if the schema has a bulkId reference but does not use it and sets the ID
   * directly instead
   */
  @Test
  public void testSimpleBulkIdReferenceWithSeveralReferences()
  {
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(20);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    serviceProvider.getBulkConfig().setReturnResourcesEnabled(true);

    BulkRequest bulkRequest = JsonHelper.loadJsonDocument(BULK_ID_REFERENCE_RESOURCE_ENSEMBLE, BulkRequest.class);
    {
      // set a direct value and not a bulkId-reference into the userId field this must execute successfully
      ObjectNode objectNode = JsonHelper.readJsonDocument(bulkRequest.getBulkRequestOperations().get(0).getData().get(),
                                                          ObjectNode.class);
      objectNode.set("userId", new TextNode(UUID.randomUUID().toString()));
      bulkRequest.getBulkRequestOperations().get(0).setData(objectNode.toString());
    }

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());

    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertEquals(HttpStatus.CREATED, bulkResponseOperation.getStatus(), bulkResponse.toPrettyString());
    }
    BulkResponseOperation responseOperation = bulkResponse.getByBulkId("1").get();
    BulkIdReferences bulkIdReferences = responseOperation.getResponse(BulkIdReferences.class).get();

    Set<String> resolvedIds = new HashSet<>();
    resolvedIds.addAll(bulkIdReferences.getUserIdList());
    resolvedIds.add(bulkIdReferences.getMember().get().getUserId().get());
    resolvedIds.addAll(bulkIdReferences.getMember().get().getUserIdList());

    for ( BulkIdReferences.MemberList memberList : bulkIdReferences.getMemberList() )
    {
      resolvedIds.add(memberList.getGroupId().get());
      resolvedIds.addAll(memberList.getGroupIdList());
    }
    Assertions.assertEquals(11, resolvedIds.size());
    Assertions.assertTrue(resolvedIds.stream().noneMatch(id -> {
      return id.startsWith(String.format("%s:", AttributeNames.RFC7643.BULK_ID));
    }));
    Assertions.assertTrue(resolvedIds.stream().allMatch(this::isUuid));
  }

  /**
   * verifies that simple bulkId references are also correctly resolved within patch requests with path.
   */
  @Test
  public void testResolveSimpleBulkIdsInPatchRequestWithPath()
  {
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(20);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    serviceProvider.getBulkConfig().setReturnResourcesEnabled(true);

    BulkRequest bulkRequest = JsonHelper.loadJsonDocument(BULK_ID_REFERENCE_PATCH_WITH_PATH_ENSEMBLE,
                                                          BulkRequest.class);

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());

    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      if ("3".equals(bulkResponseOperation.getBulkId().get()))
      {
        Assertions.assertEquals(HttpStatus.OK, bulkResponseOperation.getStatus(), bulkResponse.toPrettyString());
      }
      else
      {
        Assertions.assertEquals(HttpStatus.CREATED, bulkResponseOperation.getStatus(), bulkResponse.toPrettyString());
      }
    }
    BulkResponseOperation responseOperation = bulkResponse.getByBulkId("3").get();
    BulkIdReferences bulkIdReferences = responseOperation.getResponse(BulkIdReferences.class).get();

    Set<String> resolvedIds = new HashSet<>();
    resolvedIds.add(bulkIdReferences.getUserId().get());
    resolvedIds.addAll(bulkIdReferences.getUserIdList());
    resolvedIds.add(bulkIdReferences.getMember().get().getUserId().get());
    resolvedIds.addAll(bulkIdReferences.getMember().get().getUserIdList());

    for ( BulkIdReferences.MemberList memberList : bulkIdReferences.getMemberList() )
    {
      resolvedIds.add(memberList.getGroupId().get());
      resolvedIds.addAll(memberList.getGroupIdList());
    }
    Assertions.assertEquals(12, resolvedIds.size());
    Assertions.assertTrue(resolvedIds.stream().noneMatch(id -> {
      return id.startsWith(String.format("%s:", AttributeNames.RFC7643.BULK_ID));
    }));
    Assertions.assertTrue(resolvedIds.stream().allMatch(this::isUuid));
  }

  /**
   * verifies that the noContent http-status is also accepted as valid in bulk-requests
   */
  @DisplayName("204 is accepted for PATCH in BulkResponses")
  @Test
  public void test204IsAcceptedInPatchBulkResponses()
  {
    serviceProvider.getPatchConfig().setSupported(true);
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(20);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    serviceProvider.getBulkConfig().setReturnResourcesEnabled(true);

    // make sure that status-code 204 is returned by returning null from getUpdatedResource
    {
      PatchOperationHandler patchOperationHandler = //
        Mockito.spy(bulkIdReferencesResourceHandler.getPatchOpResourceHandler(Mockito.any(), Mockito.any()));
      Mockito.doReturn(patchOperationHandler)
             .when(bulkIdReferencesResourceHandler)
             .getPatchOpResourceHandler(Mockito.any(), Mockito.any());
      Mockito.doReturn(null)
             .when(patchOperationHandler)
             .getUpdatedResource(Mockito.any(),
                                 Mockito.any(),
                                 Mockito.anyBoolean(),
                                 Mockito.any(),
                                 Mockito.any(),
                                 Mockito.any());
    }

    BulkRequest bulkRequest = JsonHelper.loadJsonDocument(BULK_ID_REFERENCE_PATCH_WITH_PATH_ENSEMBLE,
                                                          BulkRequest.class);

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());

    log.warn(bulkResponse.toPrettyString());

    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      if ("3".equals(bulkResponseOperation.getBulkId().get()))
      {
        Assertions.assertEquals(HttpStatus.NO_CONTENT,
                                bulkResponseOperation.getStatus(),
                                bulkResponse.toPrettyString());
      }
      else
      {
        Assertions.assertEquals(HttpStatus.CREATED, bulkResponseOperation.getStatus(), bulkResponse.toPrettyString());
      }
    }
  }

  /**
   * verifies that the bulk-get feature will act the same if the resource level is set to 1 or below 1. 1 is the
   * lowest possible value to set
   */
  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  public void testUseBulkGetWithResourceLevel(int maxResourceLevel)
  {
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(20);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    serviceProvider.getBulkConfig().setSupportBulkGet(true);

    User user1 = User.builder().id("1").userName("user1").build();
    User user2 = User.builder().id("2").userName("user2").build();
    User user3 = User.builder()
                     .id("3")
                     .userName("user3")
                     .enterpriseUser(EnterpriseUser.builder()
                                                   .manager(Manager.builder()
                                                                   .value(user2.getId().get())
                                                                   .ref(String.format("%s%s/%s",
                                                                                      BASE_URI,
                                                                                      EndpointPaths.USERS,
                                                                                      user2.getId().get()))
                                                                   .build())
                                                   .build())
                     .build();
    Group group1 = Group.builder().id("1").displayName("group1").build();
    Group group2 = Group.builder()
                        .id("2")
                        .displayName("group2")
                        .members(Arrays.asList(Member.builder()
                                                     .value(user3.getId().get())
                                                     .type(ResourceTypeNames.USER)
                                                     .build()))
                        .build();

    groupHandler.getInMemoryMap().put(group1.getId().get(), group1);
    groupHandler.getInMemoryMap().put(group2.getId().get(), group2);
    userHandler.getInMemoryMap().put(user1.getId().get(), user1);
    userHandler.getInMemoryMap().put(user2.getId().get(), user2);
    userHandler.getInMemoryMap().put(user3.getId().get(), user3);

    List<Member> members = Arrays.asList(Member.builder()
                                               .value(group1.getId().get())
                                               .type(ResourceTypeNames.GROUPS)
                                               .build(),
                                         Member.builder()
                                               .value(group2.getId().get())
                                               .ref(String.format("%s%s/%s",
                                                                  BASE_URI,
                                                                  EndpointPaths.GROUPS,
                                                                  group2.getId().get()))
                                               .build(),
                                         Member.builder()
                                               .value(user1.getId().get())
                                               .type(ResourceTypeNames.USER)
                                               .build(),
                                         Member.builder()
                                               .value(user2.getId().get())
                                               .ref(String.format("%s%s/%s",
                                                                  BASE_URI,
                                                                  EndpointPaths.USERS,
                                                                  user2.getId().get()))
                                               .build());
    Group adminGroup = Group.builder().id("3").displayName("admin").members(members).build();

    groupHandler.getInMemoryMap().put(adminGroup.getId().get(), adminGroup);

    final String resourcePath = String.format("%s/%s", EndpointPaths.GROUPS, adminGroup.getId().get());
    List<BulkRequestOperation> bulkRequestOperations = Arrays.asList(BulkRequestOperation.builder()
                                                                                         .method(HttpMethod.GET)
                                                                                         .path(resourcePath)
                                                                                         .maxResourceLevel(maxResourceLevel)
                                                                                         .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(bulkRequestOperations).failOnErrors(0).build();

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());

    Assertions.assertEquals(1, bulkResponse.getBulkResponseOperations().size());
    BulkResponseOperation responseOperation = bulkResponse.getBulkResponseOperations().get(0);
    BulkResponseGetOperation responseGetOperation = responseOperation.getResponse(BulkResponseGetOperation.class).get();
    Assertions.assertEquals(HttpStatus.OK, responseGetOperation.getStatus());

    Assertions.assertEquals(adminGroup.getId().get(), responseGetOperation.getResourceId());
    Assertions.assertEquals(adminGroup.getId().get(), responseGetOperation.getResource(Group.class).getId().get());
    Assertions.assertEquals(4, responseGetOperation.getChildren().size());

    List<BulkResponseGetOperation> groupResponses = responseGetOperation.getChildren().stream().filter(op -> {
      return op.getResourceType().equals(ResourceTypeNames.GROUPS);
    }).collect(Collectors.toList());

    Assertions.assertEquals(2, groupResponses.size());
    Assertions.assertEquals(group1.getId().get(), groupResponses.get(0).getResourceId());
    Assertions.assertEquals(group1.getId().get(), groupResponses.get(0).getResource(Group.class).getId().get());
    Assertions.assertEquals(0, groupResponses.get(0).getChildren().size());

    Assertions.assertEquals(group2.getId().get(), groupResponses.get(1).getResourceId());
    Assertions.assertEquals(group2.getId().get(), groupResponses.get(1).getResource(Group.class).getId().get());
    Assertions.assertEquals(0, groupResponses.get(1).getChildren().size());

    List<BulkResponseGetOperation> userResponses = responseGetOperation.getChildren().stream().filter(op -> {
      return op.getResourceType().equals(ResourceTypeNames.USER);
    }).collect(Collectors.toList());
    Assertions.assertEquals(2, userResponses.size());
    Assertions.assertEquals(user1.getId().get(), userResponses.get(0).getResourceId());
    Assertions.assertEquals(user1.getId().get(), userResponses.get(0).getResource(User.class).getId().get());
    Assertions.assertEquals(0, userResponses.get(0).getChildren().size());

    Assertions.assertEquals(user2.getId().get(), userResponses.get(1).getResourceId());
    Assertions.assertEquals(user2.getId().get(), userResponses.get(1).getResource(User.class).getId().get());
    Assertions.assertEquals(0, userResponses.get(1).getChildren().size());
  }

  /**
   * tests the bulk-get feature with a group referencing two groups of which one will address a user that has
   * also a manager and this user will also be addressed by the parent group
   */
  @Test
  public void testUseBulkGetWithResourceLevel_2()
  {
    final int maxResourceLevel = 2;

    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(20);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    serviceProvider.getBulkConfig().setSupportBulkGet(true);

    User user1 = User.builder().id("1").userName("user1").build();
    User user2 = User.builder().id("2").userName("user2").build();
    User user3 = User.builder()
                     .id("3")
                     .userName("user3")
                     .enterpriseUser(EnterpriseUser.builder()
                                                   .manager(Manager.builder()
                                                                   .value(user2.getId().get())
                                                                   .ref(String.format("%s%s/%s",
                                                                                      BASE_URI,
                                                                                      EndpointPaths.USERS,
                                                                                      user2.getId().get()))
                                                                   .build())
                                                   .build())
                     .build();
    Group group1 = Group.builder().id("1").displayName("group1").build();
    Group group2 = Group.builder()
                        .id("2")
                        .displayName("group2")
                        .members(Arrays.asList(Member.builder()
                                                     .value(user3.getId().get())
                                                     .type(ResourceTypeNames.USER)
                                                     .build()))
                        .build();

    groupHandler.getInMemoryMap().put(group1.getId().get(), group1);
    groupHandler.getInMemoryMap().put(group2.getId().get(), group2);
    userHandler.getInMemoryMap().put(user1.getId().get(), user1);
    userHandler.getInMemoryMap().put(user2.getId().get(), user2);
    userHandler.getInMemoryMap().put(user3.getId().get(), user3);

    List<Member> members = Arrays.asList(Member.builder()
                                               .value(group1.getId().get())
                                               .type(ResourceTypeNames.GROUPS)
                                               .build(),
                                         Member.builder()
                                               .value(group2.getId().get())
                                               .ref(String.format("%s%s/%s",
                                                                  BASE_URI,
                                                                  EndpointPaths.GROUPS,
                                                                  group2.getId().get()))
                                               .build(),
                                         Member.builder()
                                               .value(user1.getId().get())
                                               .type(ResourceTypeNames.USER)
                                               .build(),
                                         Member.builder()
                                               .value(user2.getId().get())
                                               .ref(String.format("%s%s/%s",
                                                                  BASE_URI,
                                                                  EndpointPaths.USERS,
                                                                  user2.getId().get()))
                                               .build());
    Group adminGroup = Group.builder().id("3").displayName("admin").members(members).build();

    groupHandler.getInMemoryMap().put(adminGroup.getId().get(), adminGroup);

    final String resourcePath = String.format("%s/%s", EndpointPaths.GROUPS, adminGroup.getId().get());
    List<BulkRequestOperation> bulkRequestOperations = Arrays.asList(BulkRequestOperation.builder()
                                                                                         .method(HttpMethod.GET)
                                                                                         .path(resourcePath)
                                                                                         .maxResourceLevel(maxResourceLevel)
                                                                                         .build());
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(bulkRequestOperations).failOnErrors(0).build();

    BulkResponse bulkResponse = bulkEndpoint.bulk(BASE_URI, bulkRequest.toString(), null);
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());

    Assertions.assertEquals(1, bulkResponse.getBulkResponseOperations().size());
    BulkResponseOperation responseOperation = bulkResponse.getBulkResponseOperations().get(0);
    BulkResponseGetOperation responseGetOperation = responseOperation.getResponse(BulkResponseGetOperation.class).get();
    Assertions.assertEquals(HttpStatus.OK, responseGetOperation.getStatus());

    Assertions.assertEquals(adminGroup.getId().get(), responseGetOperation.getResourceId());
    Assertions.assertEquals(adminGroup.getId().get(), responseGetOperation.getResource(Group.class).getId().get());
    Assertions.assertEquals(4, responseGetOperation.getChildren().size());

    List<BulkResponseGetOperation> groupResponses = responseGetOperation.getChildren().stream().filter(op -> {
      return op.getResourceType().equals(ResourceTypeNames.GROUPS);
    }).collect(Collectors.toList());

    Assertions.assertEquals(2, groupResponses.size());
    Assertions.assertEquals(group1.getId().get(), groupResponses.get(0).getResourceId());
    Assertions.assertEquals(group1.getId().get(), groupResponses.get(0).getResource(Group.class).getId().get());
    Assertions.assertEquals(ResourceTypeNames.GROUPS, groupResponses.get(0).getResourceType());
    Assertions.assertEquals(0, groupResponses.get(0).getChildren().size());

    Assertions.assertEquals(group2.getId().get(), groupResponses.get(1).getResourceId());
    Assertions.assertEquals(group2.getId().get(), groupResponses.get(1).getResource(Group.class).getId().get());
    Assertions.assertEquals(ResourceTypeNames.GROUPS, groupResponses.get(1).getResourceType());
    Assertions.assertEquals(1, groupResponses.get(1).getChildren().size());
    Assertions.assertEquals(0, groupResponses.get(1).getChildren().get(0).getChildren().size());
    Assertions.assertEquals(ResourceTypeNames.USER, groupResponses.get(1).getChildren().get(0).getResourceType());

    List<BulkResponseGetOperation> userResponses = responseGetOperation.getChildren().stream().filter(op -> {
      return op.getResourceType().equals(ResourceTypeNames.USER);
    }).collect(Collectors.toList());
    Assertions.assertEquals(2, userResponses.size());
    Assertions.assertEquals(user1.getId().get(), userResponses.get(0).getResourceId());
    Assertions.assertEquals(user1.getId().get(), userResponses.get(0).getResource(User.class).getId().get());
    Assertions.assertEquals(0, userResponses.get(0).getChildren().size());

    Assertions.assertEquals(user2.getId().get(), userResponses.get(1).getResourceId());
    Assertions.assertEquals(user2.getId().get(), userResponses.get(1).getResource(User.class).getId().get());
    Assertions.assertEquals(0, userResponses.get(1).getChildren().size());
  }

  private boolean isUuid(String id)
  {
    try
    {
      UUID.fromString(id);
      return true;
    }
    catch (Exception ex)
    {
      return false;
    }
  }
}
