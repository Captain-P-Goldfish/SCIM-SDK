package de.gold.scim.endpoints;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.constants.HttpHeader;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.constants.ScimType;
import de.gold.scim.constants.enums.SortOrder;
import de.gold.scim.endpoints.base.GroupEndpointDefinition;
import de.gold.scim.endpoints.base.ResourceTypeEndpointDefinition;
import de.gold.scim.endpoints.base.UserEndpointDefinition;
import de.gold.scim.endpoints.handler.GroupHandlerImpl;
import de.gold.scim.endpoints.handler.ResourceTypeHandler;
import de.gold.scim.endpoints.handler.UserHandlerImpl;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.ConflictException;
import de.gold.scim.exceptions.InternalServerException;
import de.gold.scim.exceptions.NotImplementedException;
import de.gold.scim.exceptions.ResourceNotFoundException;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.request.SearchRequest;
import de.gold.scim.resources.ResourceNode;
import de.gold.scim.resources.ServiceProvider;
import de.gold.scim.resources.ServiceProviderUrlExtension;
import de.gold.scim.resources.User;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.response.CreateResponse;
import de.gold.scim.response.DeleteResponse;
import de.gold.scim.response.ErrorResponse;
import de.gold.scim.response.GetResponse;
import de.gold.scim.response.ListResponse;
import de.gold.scim.response.PartialListResponse;
import de.gold.scim.response.ScimResponse;
import de.gold.scim.response.UpdateResponse;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.schemas.Schema;
import de.gold.scim.schemas.SchemaAttribute;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;
import de.gold.scim.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 23:54 <br>
 * <br>
 */
@Slf4j
public class ResourceEndpointHandlerTest implements FileReferences
{

  /**
   * defines the base url for the meta-attribute
   */
  private static final String BASE_URL = "https://localhost/scim/v2";

  /**
   * the resource type factory in which the resources will be registered
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource endpoints implementation that will handle any request
   */
  private ResourceEndpointHandler resourceEndpointHandler;

  /**
   * a mockito spy to verify that the methods are called correctly by the {@link ResourceEndpointHandler}
   * implementation
   */
  private UserHandlerImpl userHandler;

  /**
   * a mockito spy to verify that the methods are called correctly by the {@link ResourceEndpointHandler}
   * implementation
   */
  private GroupHandlerImpl groupHandler;

  /**
   * a mockito spy to verify that the methods are called correctly by the {@link ResourceEndpointHandler}
   * implementation
   */
  private ResourceTypeHandler resourceTypeHandler;


  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    userHandler = Mockito.spy(new UserHandlerImpl());
    groupHandler = Mockito.spy(new GroupHandlerImpl());
    UserEndpointDefinition userEndpoint = new UserEndpointDefinition(userHandler);
    GroupEndpointDefinition groupEndpoint = new GroupEndpointDefinition(groupHandler);

    ServiceProviderUrlExtension urlExtension = ServiceProviderUrlExtension.builder().baseUrl(BASE_URL).build();
    ServiceProvider serviceProvider = ServiceProvider.builder().serviceProviderUrlExtension(urlExtension).build();
    this.resourceEndpointHandler = new ResourceEndpointHandler(serviceProvider, userEndpoint, groupEndpoint);
    this.resourceTypeFactory = resourceEndpointHandler.getResourceTypeFactory();

    resourceTypeHandler = new ResourceTypeHandler(resourceTypeFactory);
    resourceTypeHandler = Mockito.spy(resourceTypeHandler);
    EndpointDefinition endpointDefinition = new ResourceTypeEndpointDefinition(resourceTypeHandler);
    resourceEndpointHandler.registerEndpoint(endpointDefinition);
  }

  /**
   * this test must fail with an exception for no resource endpoints will be defined when creating the endpoint
   * handler
   */
  @Test
  public void testCreateEndpointWithoutResourceEndpoints()
  {
    Assertions.assertThrows(InternalServerException.class,
                            () -> new ResourceEndpointHandler(ServiceProvider.builder().build()));
  }

  /**
   * this test will create, read, update, read, delete and read a user instance
   */
  @Test
  public void testLifeOfResource()
  {
    final String endpoint = "/Users";
    User createdUser = createUser(endpoint);
    String userId = createdUser.getId().orElse(null);
    User readUser = getUser(endpoint, userId);
    Assertions.assertEquals(createdUser.getId().get(), readUser.getId().get());
    Assertions.assertEquals(createdUser.getUserName(), readUser.getUserName());

    User updatedUser = updateUser(endpoint, readUser);
    Assertions.assertEquals(userId, updatedUser.getId().get());
    ScimResponse deleteResponse = resourceEndpointHandler.deleteResource(endpoint, userId);
    MatcherAssert.assertThat(deleteResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
    Mockito.verify(userHandler, Mockito.times(1)).deleteResource(userId);
    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> resourceEndpointHandler.getResource(endpoint,
                                                                                                        userId));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(ResourceNotFoundException.class));
    Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * creates a simple user and returns the created resource
   *
   * @param endpoint the resource endpoint that should be used
   * @return the created user
   */
  private User createUser(String endpoint)
  {
    ScimResponse scimResponse = resourceEndpointHandler.createResource(endpoint,
                                                                       readResourceFile(USER_RESOURCE),
                                                                       getBaseUrlSupplier());
    Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
    Assertions.assertEquals(HttpStatus.SC_CREATED, scimResponse.getHttpStatus());
    String createResponse = scimResponse.toJsonDocument();
    Assertions.assertNotNull(createResponse);
    User user = JsonHelper.readJsonDocument(createResponse, User.class);
    String userId = user.getId().orElse(null);
    Assertions.assertNotNull(userId);
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            scimResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertNotNull(scimResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Meta meta = user.getMeta().get();
    Assertions.assertEquals("User", meta.getResourceType().get());
    Assertions.assertEquals(getLocation(endpoint, userId), meta.getLocation().get());
    Assertions.assertTrue(meta.getCreated().isPresent());
    Assertions.assertTrue(meta.getLastModified().isPresent());
    // TODO check that the last modified value is correct
    return user;
  }

  /**
   * this test will throw an exception during object creation and check that the error response is correctly
   * returned
   */
  @Test
  public void testCreateUserWithScimException()
  {
    ConflictException exception = new ConflictException("blubb");
    Mockito.doThrow(exception).when(userHandler).createResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.createResource("/Users",
                                                                       readResourceFile(USER_RESOURCE),
                                                                       getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(exception, errorResponse.getScimException());
    Assertions.assertEquals(HttpStatus.SC_CONFLICT, errorResponse.getHttpStatus());
  }

  /**
   * this test will throw an exception during object creation and check that the error response is correctly
   * returned
   */
  @Test
  public void testCreateUserWithRuntimeException()
  {
    RuntimeException exception = new RuntimeException("blubb");
    Mockito.doThrow(exception).when(userHandler).createResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.createResource("/Users",
                                                                       readResourceFile(USER_RESOURCE),
                                                                       getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(exception, errorResponse.getScimException().getCause());
    Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * this test will try to get a resource with an empty id which should cause a
   * {@link ResourceNotFoundException}
   */
  @Test
  public void testGetResourceWithoutId()
  {
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users", "", getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * if the returned resource by the getResource endpoint has no id an {@link InternalServerException} must be
   * thrown
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "123456"})
  public void testGetResourceWithReturnedResourceHasDifferentId(String id)
  {
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    user.setId(null);
    Mockito.doReturn(user).when(userHandler).getResource(Mockito.eq(id));
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users", id, getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link de.gold.scim.exceptions.ScimException} is correctly handled by the
   * {@link ResourceEndpointHandler#getResource(String, String)} method
   */
  @Test
  public void testThrowScimExceptionOnGetResource()
  {
    ResourceNotFoundException exception = new ResourceNotFoundException("blubb", null, null);
    Mockito.doThrow(exception).when(userHandler).getResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users", "123456", getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link RuntimeException} is correctly handled by the
   * {@link ResourceEndpointHandler#getResource(String, String)} method
   */
  @Test
  public void testThrowRuntimeExceptionOnGetResource()
  {
    RuntimeException exception = new RuntimeException("blubb");
    Mockito.doThrow(exception).when(userHandler).getResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users", "123456", getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * if no resource is returned after update a {@link ResourceNotFoundException} should be thrown
   */
  @Test
  public void testSendUnusableResourceToUpdate()
  {
    User user = User.builder().id(UUID.randomUUID().toString()).build();
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       UUID.randomUUID().toString(),
                                                                       user.toString(),
                                                                       getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(BadRequestException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getHttpStatus());
    Assertions.assertEquals(ScimType.Custom.UNPARSEABLE_REQUEST, errorResponse.getScimException().getScimType());
  }

  /**
   * if no resource is returned after update a {@link ResourceNotFoundException} should be thrown
   */
  @Test
  public void testDoNotReturnResourceAfterUpdate()
  {
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    Mockito.doReturn(null).when(userHandler).updateResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       UUID.randomUUID().toString(),
                                                                       user.toString(),
                                                                       getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * if the returned resource by the getResource endpoint has no id an {@link InternalServerException} must be
   * thrown
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "123456"})
  public void testUpdateResourceWithReturnedResourceHasDifferentId(String id)
  {
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    Mockito.doReturn(JsonHelper.copyResourceToObject(user.deepCopy(), User.class))
           .when(userHandler)
           .updateResource(Mockito.any());
    user.setId(null);
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       id,
                                                                       user.toString(),
                                                                       getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link de.gold.scim.exceptions.ScimException} is correctly handled by the
   * {@link ResourceEndpointHandler#updateResource(String, String, String)} method
   */
  @Test
  public void testThrowScimExceptionOnUpdateResource()
  {
    ResourceNotFoundException exception = new ResourceNotFoundException("blubb", null, null);
    Mockito.doThrow(exception).when(userHandler).updateResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       "123456",
                                                                       readResourceFile(USER_RESOURCE));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link RuntimeException} is correctly handled by the
   * {@link ResourceEndpointHandler#updateResource(String, String, String)} method
   */
  @Test
  public void testThrowRuntimeExceptionOnUpdateResource()
  {
    RuntimeException exception = new RuntimeException("blubb");
    Mockito.doThrow(exception).when(userHandler).updateResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       "123456",
                                                                       readResourceFile(USER_RESOURCE));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link de.gold.scim.exceptions.ScimException} is correctly handled by the
   * {@link ResourceEndpointHandler#deleteResource(String, String)} method
   */
  @Test
  public void testThrowScimExceptionOnDeleteResource()
  {
    ResourceNotFoundException exception = new ResourceNotFoundException("blubb", null, null);
    Mockito.doThrow(exception).when(userHandler).deleteResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.deleteResource("/Users", "123456");
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link BadRequestException} is thrown if the parameters attributes and excludedAttributes
   * are set at the same time on creation request
   */
  @Test
  public void testThrowBadRequestIfAttributeAndExcludedAttribtesAreSetOnCreate()
  {
    ScimResponse scimResponse = resourceEndpointHandler.createResource("/Users",
                                                                       readResourceFile(USER_RESOURCE),
                                                                       "userName",
                                                                       "name",
                                                                       getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(BadRequestException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getHttpStatus());
    Assertions.assertEquals(ScimType.Custom.INVALID_PARAMETERS, errorResponse.getScimException().getScimType());
  }

  /**
   * will show that a {@link BadRequestException} is thrown if the parameters attributes and excludedAttributes
   * are set at the same time on get request
   */
  @Test
  public void testThrowBadRequestIfAttributeAndExcludedAttribtesAreSetOnGet()
  {
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users",
                                                                    "123456",
                                                                    "userName",
                                                                    "name",
                                                                    getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(BadRequestException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getHttpStatus());
    Assertions.assertEquals(ScimType.Custom.INVALID_PARAMETERS, errorResponse.getScimException().getScimType());
  }

  /**
   * will show that a {@link BadRequestException} is thrown if the parameters attributes and excludedAttributes
   * are set at the same time on update request
   */
  @Test
  public void testThrowBadRequestIfAttributeAndExcludedAttribtesAreSetOnUpdate()
  {
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       "123456",
                                                                       readResourceFile(USER_RESOURCE),
                                                                       "userName",
                                                                       "name",
                                                                       getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(BadRequestException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getHttpStatus());
    Assertions.assertEquals(ScimType.Custom.INVALID_PARAMETERS, errorResponse.getScimException().getScimType());
  }

  /**
   * will show that a {@link RuntimeException} is correctly handled by the
   * {@link ResourceEndpointHandler#deleteResource(String, String)} method
   */
  @Test
  public void testThrowRuntimeExceptionOnDeleteResource()
  {
    RuntimeException exception = new RuntimeException("blubb");
    Mockito.doThrow(exception).when(userHandler).deleteResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.deleteResource("/Users", "123456");
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * tests that if count is set to 1 that only a single entry is returned no matter which startIndex is used.
   * This test depends on at least 4 registered resource types otherwise this test will get
   * {@link BadRequestException}s
   */
  @ParameterizedTest
  @ValueSource(longs = {-1, 0, 1, 2, 3, 4})
  public void testListResourceTypesWithStartIndex(long startIndex)
  {
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                      SearchRequest.builder()
                                                                                   .startIndex(startIndex)
                                                                                   .count(1)
                                                                                   .build(),
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.SC_OK, listResponse.getHttpStatus());
    Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getTotalResults());
    Assertions.assertEquals(1, listResponse.getListedResources().size());
    Assertions.assertEquals(1, listResponse.getItemsPerPage());
  }

  /**
   * verifies that no results will be returned if the startIndex exceeds the number of results that are
   * available
   */
  @Test
  public void testListResourceTypesWithStartIndexOutOfRange()
  {
    final long startIndex = resourceTypeFactory.getAllResourceTypes().size() + 1;
    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> {
      return resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                   SearchRequest.builder().startIndex(startIndex).count(1).build(),
                                                   null);
    });
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(0, listResponse.getListedResources().size());
    Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getTotalResults());
  }

  /**
   * this test will check that the implementation is reducing the number of returned entries to the desired
   * number of entries if the developer returned too many
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  public void testReturnTooManyEntries(int count)
  {
    final long totalResults = count * 2;
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setMaxResults((int)totalResults);
    List<User> userList = createUsers(totalResults);
    PartialListResponse<User> partialListResponse = PartialListResponse.<User> builder()
                                                                       .totalResults(totalResults)
                                                                       .resources(userList)
                                                                       .build();
    Mockito.doReturn(partialListResponse)
           .when(userHandler)
           .listResources(Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());

    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      count,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(totalResults, listResponse.getTotalResults());
    Assertions.assertEquals(count, listResponse.getItemsPerPage());
    Assertions.assertEquals(1, listResponse.getStartIndex());
    Assertions.assertEquals(count, listResponse.getListedResources().size());
  }

  /**
   * this test will verify that no the sortBy value is always null if the sorting feature is disabled
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "userName", SchemaUris.USER_URI + ":" + "userName", "unknownAttributeName"})
  public void testSortByIfSortDisabled(String sortBy)
  {
    createUsers(1);
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(false);
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L), Mockito.eq(0), Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
  }

  /**
   * this test will verify that no the sortBy value is always null if the sorting feature is disabled
   */
  @ParameterizedTest
  @ValueSource(strings = {"userName", SchemaUris.USER_URI + ":" + "userName"})
  public void testSortByIfSortEnabled(String sortBy)
  {
    createUsers(1);
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(true);
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.USERS);
    SchemaAttribute sortByAttribute = RequestUtils.getSchemaAttributeForSortBy(resourceType, sortBy);
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(0),
                          Mockito.isNull(),
                          Mockito.eq(sortByAttribute),
                          Mockito.eq(SortOrder.ASCENDING));
  }

  /**
   * this test will verify that an exception is thrown if the sortBy value is unknown by the resource type
   */
  @Test
  public void testSortByIfSortIsEnabledWithUnknownAttribute()
  {
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(true);
    String sortBy = "unknownAttributeName";
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(BadRequestException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(ScimType.Custom.INVALID_PARAMETERS, errorResponse.getScimException().getScimType());
    MatcherAssert.assertThat(errorResponse.getScimException().getDetail(), Matchers.containsString(sortBy));
  }

  /**
   * this test will verify that the sortOrder value is correctly handled if sorting is disabled. Meaning the
   * result must always be null
   */
  @ParameterizedTest
  @CsvSource({",", "userName,", ",ASCENDING", ",DESCENDING", "userName,ASCENDING", "userName,DESCENDING",})
  public void testSortOrderIfSortDisabled(String sortBy, SortOrder sortOrder)
  {
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(false);
    final String sortOrderString = sortOrder == null ? null : sortOrder.name().toLowerCase();
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      sortOrderString,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L), Mockito.eq(0), Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
  }

  /**
   * this test will verify that the sortOrder value is correctly handled if sorting is enabled.
   */
  @ParameterizedTest
  @CsvSource({",", "userName,", ",ASCENDING", ",DESCENDING", "userName,ASCENDING", "userName,DESCENDING",})
  public void testSortOrderIfSortEnabled(String sortBy, SortOrder sortOrder)
  {
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(true);
    final String sortOrderString = sortOrder == null ? null : sortOrder.name().toLowerCase();
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      sortOrderString,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.USERS);
    SchemaAttribute sortByAttribute = RequestUtils.getSchemaAttributeForSortBy(resourceType, sortBy);
    SortOrder actualSortOrder = sortOrder == null && sortBy != null ? SortOrder.ASCENDING : sortOrder;
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(0),
                          Mockito.isNull(),
                          Mockito.eq(sortByAttribute),
                          Mockito.eq(actualSortOrder));
  }

  /**
   * this test will verify that the filter is not parsed if filtering is disabled
   */
  @Test
  public void testFilterIfFilteringDisabled()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(false);
    String filter = "userName eq \"chuck\"";
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      filter,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L), Mockito.eq(0), Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
  }

  /**
   * this test will verify that no exception is thrown if filtering is disabled and a filter with an unknown
   * attribute is sent
   */
  @Test
  public void testFilterIfFilteringDisabledWithUnknownAttribute()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(false);
    String filter = "unknownAttributeName eq \"chuck\"";
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      filter,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.not(Matchers.typeCompatibleWith(ErrorResponse.class)));
  }

  /**
   * this test will verify that the filter is parsed if filtering is enabled
   */
  @ParameterizedTest
  @ValueSource(strings = {"userName co \"chu\"", "userName eq \"chuck\"", "userName eq \"5.5\"",
                          "userName eq \"chu\" or name.givenName eq \"Carlos\"",
                          "userName eq \"chu\" and name.givenName eq \"null\"",
                          "userName eq \"chu\" and not( name.givenName eq null )",
                          "((userName eq \"5.5\") and not( name.givenName eq \"Carlos\" OR nickName eq \"blubb\"))",
                          "((userName eQ \"chu\") and not( name.givenName eq \"-6\" or nickName eq \"true\"))",
                          "((userName eq \"false\") and not( name.givenName eq \"6\" or nickName eq \"true\"))",
                          "((userName pR) and not( name.givenName Pr and nickName pr))", "emails.primary eq true",
                          "((userName ne \"false\") and not( name.givenName co \"-6\" or nickName sw \"true\"))",
                          "((userName ew \"false\") and not( name.givenName gt \"6\" or nickName GE \"true\"))",
                          "((userName lt \"false\") and not( name.givenName le \"-6\" or nickName gt \"true\"))",
                          "meta.lastModified ge \"2019-10-17T01:07:00Z\"",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter eq \"chuck\"",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value eq \"chuck\"",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value eq \"5.5\""})
  public void testFilterIfFilteringEnabled(String filter)
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      filter,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.USERS);
    FilterNode filterNode = RequestUtils.parseFilter(resourceType, filter);
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L), Mockito.eq(0), Mockito.eq(filterNode), Mockito.isNull(), Mockito.isNull());
  }

  /**
   * this test verifies that the framework reacts with an {@link InternalServerException} wrapped in a
   * {@link ErrorResponse} if an exception is thrown in the developer implementation of listResources
   */
  @Test
  public void testThrowNullPointerException()
  {
    Mockito.doThrow(NullPointerException.class)
           .when(userHandler)
           .listResources(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
  }

  /**
   * verifies that a {@link de.gold.scim.exceptions.NotImplementedException} {@link ErrorResponse} is returned
   * if the developer returns null on the
   * {@link ResourceHandler#listResources(long, int, FilterNode, SchemaAttribute, SortOrder)} method
   */
  @Test
  public void testReturnNullInDeveloperImplementationOnListResources()
  {
    Mockito.doReturn(null)
           .when(userHandler)
           .listResources(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(NotImplementedException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, errorResponse.getScimException().getStatus());
  }

  /**
   * verifies that a {@link de.gold.scim.exceptions.NotImplementedException} {@link ErrorResponse} is returned
   * if the developer returns null on the {@link ResourceHandler#createResource(ResourceNode)} method
   */
  @Test
  public void testReturnNullInDeveloperImplementationOnCreateResource()
  {
    Mockito.doReturn(null).when(userHandler).createResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.createResource(EndpointPaths.USERS,
                                                                       readResourceFile(USER_RESOURCE),
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(NotImplementedException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, errorResponse.getScimException().getStatus());
  }

  /**
   * verifies that a {@link de.gold.scim.exceptions.ResourceNotFoundException} {@link ErrorResponse} is returned
   * if the developer returns null on the {@link ResourceHandler#updateResource(ResourceNode)} method
   */
  @Test
  public void testReturnNullInDeveloperImplementationOnUpdateResource()
  {
    Mockito.doReturn(null).when(userHandler).updateResource(Mockito.any());
    ScimResponse scimResponse = resourceEndpointHandler.updateResource(EndpointPaths.USERS,
                                                                       UUID.randomUUID().toString(),
                                                                       readResourceFile(USER_RESOURCE),
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, errorResponse.getScimException().getStatus());
  }

  /**
   * the modifier on the the method {@link ResourceEndpointHandler#getServiceProvider()} must be public!
   */
  @Test
  public void testAccessModifierOnServiceProviderGetter() throws NoSuchMethodException
  {
    Method method = ResourceEndpointHandler.class.getMethod("getServiceProvider");
    Assertions.assertTrue(Modifier.isPublic(method.getModifiers()));
  }

  /**
   * verifies that the listResources method will never return more entries than stated in count with count has a
   * value that enforces less than count entries in the last request
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testListResourceTypesWithStartIndexAndCount(int count)
  {
    for ( int startIndex = 0 ; startIndex < resourceTypeFactory.getAllResourceTypes().size() ; startIndex += count )
    {
      SearchRequest searchRequest = SearchRequest.builder().startIndex(startIndex + 1L).count(count).build();
      ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                        searchRequest,
                                                                        null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      ListResponse listResponse = (ListResponse)scimResponse;

      MatcherAssert.assertThat(listResponse.getListedResources().size(), Matchers.lessThanOrEqualTo(count));
      Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getTotalResults());
      log.debug("returned entries: {}", listResponse.getListedResources().size());
    }
  }

  /**
   * verifies that the listResources method will never return more entries than stated in count with count has a
   * value that enforces less than count entries in the last request
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testListResourceTypesWithStartIndexAndCountForSchemas(int count)
  {
    for ( int startIndex = 0 ; startIndex < resourceTypeFactory.getAllResourceTypes().size() ; startIndex += count )
    {
      SearchRequest searchRequest = SearchRequest.builder().startIndex(startIndex + 1L).count(count).build();
      ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.SCHEMAS, searchRequest, null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      ListResponse listResponse = (ListResponse)scimResponse;
      MatcherAssert.assertThat(listResponse.getListedResources().size(), Matchers.lessThanOrEqualTo(count));

      List<Schema> allSchemas = resourceTypeFactory.getAllResourceTypes()
                                                   .stream()
                                                   .map(ResourceType::getAllSchemas)
                                                   .flatMap(Collection::stream)
                                                   .distinct()
                                                   .collect(Collectors.toList());
      Assertions.assertEquals(allSchemas.size(), listResponse.getTotalResults());
      log.debug("returned entries: {}", listResponse.getListedResources().size());
    }
  }

  /**
   * the test will assert that the filter is not getting parsed if the filter feature is disabled
   */
  @Test
  public void testFilterWithFilteringDisabled()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(false);
    final String filter = "schemaExtensions pr";
    SearchRequest searchRequest = SearchRequest.builder().filter(filter).build();
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                      searchRequest,
                                                                      null);
    Mockito.verify(resourceTypeHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L), Mockito.anyInt(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
  }

  /**
   * this test will assert that the {@link FilterNode} is successfully passed to the {@link ResourceHandler} if
   * filtering is enabled and that the entries are returned unmodified
   */
  @Test
  public void testFilterWithFilteringOnServiceProviderEnabled()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setMaxResults(Integer.MAX_VALUE);
    final String filter = "schemaExtensions pr";
    SearchRequest searchRequest = SearchRequest.builder().filter(filter).build();
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                      searchRequest,
                                                                      null);
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES);

    Assertions.assertFalse(resourceType.getFilterExtension().isPresent());
    FilterNode filterNode = RequestUtils.parseFilter(resourceType, filter);
    Mockito.verify(resourceTypeHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L), Mockito.anyInt(), Mockito.eq(filterNode), Mockito.isNull(), Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getListedResources().size());
  }

  /**
   * this test will assure if autoFiltering is enabled that the filtering is executed successfully on the
   * returned resources
   */
  @Test
  public void testFilterWithFilteringOnServiceProviderAndResourceTypeEnabled()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setMaxResults(Integer.MAX_VALUE);
    final String filter = "schemaExtensions pr";
    SearchRequest searchRequest = SearchRequest.builder().filter(filter).build();
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES);
    resourceType.setFilterExtension(new ResourceType.FilterExtension(true));

    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                      searchRequest,
                                                                      null);

    Mockito.verify(resourceTypeHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L), Mockito.anyInt(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Collection<ResourceType> resourceTypes = resourceTypeFactory.getAllResourceTypes();

    Assertions.assertNotEquals(resourceTypes.size(), listResponse.getListedResources().size());
    Assertions.assertEquals(resourceTypes.stream().filter(rt -> rt.getSchemaExtensions().size() > 0).count(),
                            listResponse.getListedResources().size());
  }

  /**
   * the {@link ResourceEndpointHandler#registerEndpoint(EndpointDefinition)} method must be accessible for
   * developers
   */
  @Test
  public void testRegisterEndpointMustBePublic() throws NoSuchMethodException
  {
    Method method = ResourceEndpointHandler.class.getMethod("registerEndpoint", EndpointDefinition.class);
    Assertions.assertTrue(Modifier.isPublic(method.getModifiers()));
  }

  /**
   * the {@link ResourceEndpointHandler#getServiceProvider()} method must be accessible for developers
   */
  @Test
  public void testGetServiceProviderMustBePublic() throws NoSuchMethodException
  {
    Method method = ResourceEndpointHandler.class.getMethod("getServiceProvider");
    Assertions.assertTrue(Modifier.isPublic(method.getModifiers()));
  }

  /**
   * reads a user from the endpoint
   *
   * @param endpoint the resource endpoint that should be used
   * @param userId the id of the user that should be read
   * @return the returned user
   */
  private User getUser(String endpoint, String userId)
  {
    ScimResponse scimResponse = resourceEndpointHandler.getResource(endpoint, userId, getBaseUrlSupplier());
    Mockito.verify(userHandler, Mockito.times(1)).getResource(Mockito.eq(userId));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    Assertions.assertEquals(HttpStatus.SC_OK, scimResponse.getHttpStatus());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            scimResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertNotNull(scimResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    User user = JsonHelper.readJsonDocument(scimResponse.toJsonDocument(), User.class);
    Meta meta = user.getMeta().get();
    Assertions.assertEquals("User", meta.getResourceType().get());
    Assertions.assertEquals(getLocation(endpoint, userId), meta.getLocation().get());
    Assertions.assertTrue(meta.getCreated().isPresent());
    Assertions.assertTrue(meta.getLastModified().isPresent());
    // TODO check that the last modified value is correct
    return user;
  }

  /**
   * takes the given user updates some values and sends the updated user to the resource endpoint
   *
   * @param endpoint the resource endpoint that should be used
   * @param readUser the resource that will be copied and modified for the update
   * @return the updated user
   */
  private User updateUser(String endpoint, User readUser)
  {
    User updateUser = JsonHelper.copyResourceToObject(readUser.deepCopy(), User.class);
    final String usertype = "newUsertype";
    final String nickname = "newNickname";
    final String title = "newTitle";
    updateUser.setUserType(usertype);
    updateUser.setNickName(nickname);
    updateUser.setTitle(title);

    ScimResponse scimResponse = resourceEndpointHandler.updateResource(endpoint,
                                                                       readUser.getId().get(),
                                                                       updateUser.toString(),
                                                                       getBaseUrlSupplier());
    Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    Assertions.assertEquals(HttpStatus.SC_OK, scimResponse.getHttpStatus());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            scimResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertNotNull(scimResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    User updatedUser = JsonHelper.readJsonDocument(scimResponse.toJsonDocument(), User.class);
    Assertions.assertEquals(updateUser, updatedUser);
    Assertions.assertNotEquals(readUser, updatedUser);
    Assertions.assertEquals(usertype, updatedUser.getUserType().get());
    Assertions.assertEquals(nickname, updatedUser.getNickName().get());
    Assertions.assertEquals(title, updatedUser.getTitle().get());

    Meta meta = updatedUser.getMeta().get();
    Assertions.assertEquals("User", meta.getResourceType().get());
    Assertions.assertEquals(getLocation(endpoint, updatedUser.getId().get()), meta.getLocation().get());
    Assertions.assertTrue(meta.getCreated().isPresent());
    Assertions.assertTrue(meta.getLastModified().isPresent());
    // TODO check that the last modified value is correct
    return updatedUser;
  }

  /**
   * this method will get the current location uri to a resource
   *
   * @param resourceId the id of the resource
   * @return the full location URL of the endpoint
   */
  private String getLocation(String endpoint, String resourceId)
  {
    return getBaseUrlSupplier().get() + endpoint + "/" + resourceId;
  }

  /**
   * the base uri supplier that is given to the endpoint implementations
   */
  private Supplier<String> getBaseUrlSupplier()
  {
    return () -> "https://goldfish.de/scim/v2";
  }

  /**
   * creates the given number of users
   *
   * @param totalResults the number of users to create
   * @return the list of the created users
   */
  protected List<User> createUsers(long totalResults)
  {
    List<User> userList = new ArrayList<>();
    for ( int i = 0 ; i < totalResults ; i++ )
    {
      ScimResponse scimResponse = resourceEndpointHandler.createResource(EndpointPaths.USERS,
                                                                         readResourceFile(USER_RESOURCE),
                                                                         null);
      CreateResponse createResponse = (CreateResponse)scimResponse;
      User user = JsonHelper.readJsonDocument(createResponse.toJsonDocument(), User.class);
      userList.add(user);
    }
    return userList;
  }
}
