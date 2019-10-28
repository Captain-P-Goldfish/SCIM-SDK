package de.gold.scim.endpoints;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.ResourceTypeNames;
import de.gold.scim.constants.enums.HttpMethod;
import de.gold.scim.endpoints.base.UserEndpointDefinition;
import de.gold.scim.endpoints.handler.UserHandlerImpl;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.NotImplementedException;
import de.gold.scim.exceptions.ResponseException;
import de.gold.scim.request.BulkRequest;
import de.gold.scim.request.BulkRequestOperation;
import de.gold.scim.request.SearchRequest;
import de.gold.scim.resources.ServiceProvider;
import de.gold.scim.resources.User;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.response.BulkResponse;
import de.gold.scim.response.BulkResponseOperation;
import de.gold.scim.response.CreateResponse;
import de.gold.scim.response.DeleteResponse;
import de.gold.scim.response.ErrorResponse;
import de.gold.scim.response.GetResponse;
import de.gold.scim.response.ListResponse;
import de.gold.scim.response.ScimResponse;
import de.gold.scim.response.UpdateResponse;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 26.10.2019 - 00:32 <br>
 * <br>
 */
@Slf4j
public class ResourceEndpointTest
{

  /**
   * a simple basic uri used in these tests
   */
  private static final String BASE_URI = "https://localhost/scim/v2";

  /**
   * the resource endpoint under test
   */
  private ResourceEndpoint resourceEndpoint;

  /**
   * the service provider configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * a mockito spy to verify the class that have been made on this instance
   */
  private UserHandlerImpl userHandler;

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    serviceProvider = ServiceProvider.builder().build();
    userHandler = Mockito.spy(new UserHandlerImpl());
    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
  }

  /**
   * tests that urls are correctly resolved by the {@link ResourceEndpoint}
   */
  @ParameterizedTest
  @CsvSource({"/Users,,,POST", "/Users/.search,,,POST", "/Users,123456,,GET", "/Users,,startIndex=1&count=50,GET",
              "/Users,123456,,PUT", "/Users,123456,,DELETE", "/Users,123456,,PATCH"})
  public void testParseUri(String resourcePath, String resourceId, String query, HttpMethod httpMethod)
  {
    final String baseUrl = "https://localhost/management/Users/scim/v2";
    final String resourceUrl = baseUrl + resourcePath + (resourceId == null ? "" : "/" + resourceId)
                               + (query == null ? "" : "?" + query);
    ResourceEndpoint.UriInfos uriInfos = resourceEndpoint.getRequestUrlInfos(resourceUrl, httpMethod);
    Assertions.assertEquals(baseUrl, uriInfos.getBaseUri());
    Assertions.assertEquals(ResourceTypeNames.USER, uriInfos.getResourceType().getName());
    Assertions.assertEquals(resourcePath.replaceFirst(EndpointPaths.SEARCH, ""), uriInfos.getResourceEndpoint());
    Assertions.assertEquals(resourceId, uriInfos.getResourceId());
    Assertions.assertEquals(resourcePath.endsWith(EndpointPaths.SEARCH) && HttpMethod.POST.equals(httpMethod)
                            || HttpMethod.GET.equals(httpMethod) && query != null,
                            uriInfos.isSearchRequest());
  }

  /**
   * will verify that the query for a filter request is correctly parsed
   */
  @Test
  public void testParseQuery() throws UnsupportedEncodingException
  {
    final String startIndex = "1";
    final String count = "50";
    final String filter = "=username+eq+%5C%22chuck%5C%22";
    final String sortBy = "userName";
    final String sortOrder = "ascending";
    final String attributes = "name";
    final String excludedAttributes = "nickName";
    final String query = String.format("startIndex=%s&count=%s&filter=%s&sortBy=%s&sortOrder=%s&attributes=%s"
                                       + "&excludedAttributes=%s",
                                       startIndex,
                                       count,
                                       filter,
                                       sortBy,
                                       sortOrder,
                                       attributes,
                                       excludedAttributes);
    final String url = BASE_URI + EndpointPaths.USERS + "?" + query;
    ResourceEndpoint.UriInfos uriInfos = resourceEndpoint.getRequestUrlInfos(url, HttpMethod.GET);
    Assertions.assertEquals(BASE_URI, uriInfos.getBaseUri());
    Assertions.assertEquals(EndpointPaths.USERS, uriInfos.getResourceEndpoint());
    Assertions.assertNull(uriInfos.getResourceId());
    Assertions.assertEquals(ResourceTypeNames.USER, uriInfos.getResourceType().getName());

    Map<String, String> parameter = uriInfos.getQueryParameters();
    Assertions.assertEquals(startIndex, parameter.get(AttributeNames.RFC7643.START_INDEX));
    Assertions.assertEquals(count, parameter.get(AttributeNames.RFC7643.COUNT));
    Assertions.assertEquals(URLDecoder.decode(filter, StandardCharsets.UTF_8.name()),
                            parameter.get(AttributeNames.RFC7643.FILTER));
    Assertions.assertEquals(sortBy, parameter.get(AttributeNames.RFC7643.SORT_BY));
    Assertions.assertEquals(sortOrder, parameter.get(AttributeNames.RFC7643.SORT_ORDER));
    Assertions.assertEquals(attributes, parameter.get(AttributeNames.RFC7643.ATTRIBUTES));
    Assertions.assertEquals(excludedAttributes, parameter.get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES));
  }

  /**
   * will verify that a {@link de.gold.scim.exceptions.BadRequestException} is thrown if the resource endpoint
   * is unknown
   */
  @Test
  public void testUnknownResourceType()
  {
    final String url = BASE_URI + "/Unknown";
    Assertions.assertThrows(BadRequestException.class, () -> resourceEndpoint.getRequestUrlInfos(url, HttpMethod.GET));
  }

  /**
   * will verify that invalid parameter combinations in the request will lead to {@link BadRequestException}s
   */
  @ParameterizedTest
  @CsvSource({"POST,123456", "PUT,", "PATCH,", "DELETE,"})
  public void testInvalidRequestParameter(HttpMethod httpMethod, String id)
  {
    final String url = BASE_URI + EndpointPaths.USERS + (id == null ? "" : "/" + id);
    Assertions.assertThrows(BadRequestException.class, () -> resourceEndpoint.getRequestUrlInfos(url, httpMethod));
  }

  /**
   * verifies that no exceptions are thrown if the endpoint path points to bulk endpoint
   */
  @Test
  public void testParseBulkRequestAsUriInfo()
  {
    final String url = BASE_URI + EndpointPaths.BULK;
    Assertions.assertDoesNotThrow(() -> resourceEndpoint.getRequestUrlInfos(url, HttpMethod.POST));
  }

  /**
   * will verify that calling the bulk endpoint with another {@link HttpMethod} than POST causes
   * {@link BadRequestException}s
   */
  @ParameterizedTest
  @ValueSource(strings = {"GET", "PUT", "DELETE", "PATCH"})
  public void testParseBulkRequestWithInvalidHttpMethods(HttpMethod httpMethod)
  {
    final String url = BASE_URI + EndpointPaths.BULK;
    Assertions.assertThrows(BadRequestException.class, () -> resourceEndpoint.getRequestUrlInfos(url, httpMethod));
  }

  /**
   * this test will verify that a creation request is processed successfully if parameters are correctly set
   */
  @Test
  public void testCreateResource()
  {
    final String userName = "chuck_norris";
    final User user = User.builder().userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS;
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, user.toString());
    Assertions.assertEquals(1, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
    CreateResponse createResponse = (CreateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.SC_CREATED, createResponse.getHttpStatus());
    User createdUser = JsonHelper.copyResourceToObject(createResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), createdUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());
    Assertions.assertEquals(BASE_URI + EndpointPaths.USERS + "/" + createdUser.getId().get(),
                            createdUser.getMeta().get().getLocation().get());
  }

  /**
   * this test will verify that a get request is processed successfully if parameters are correctly set
   */
  @Test
  public void testGetResource()
  {
    final String userName = "chuck_norris";
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    final User user = User.builder().id(id).userName(userName).meta(meta).build();
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    userHandler.getInMemoryMap().put(id, user);
    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, user.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(getResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1)).getResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());
    Assertions.assertEquals(BASE_URI + EndpointPaths.USERS + "/" + returnedUser.getId().get(),
                            returnedUser.getMeta().get().getLocation().get());
  }

  /**
   * this test will verify that an update request is processed successfully if parameters are correctly set
   */
  @Test
  public void testUpdateResource()
  {
    final String userName = "chuck_norris";
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    final User user = User.builder().id(id).userName(userName).meta(meta).build();
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    userHandler.getInMemoryMap().put(id, user);
    User changedUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    changedUser.setUserName("ourNewName");
    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.PUT, changedUser.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.SC_OK, updateResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(updateResponse, User.class);
    Assertions.assertNotEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Assertions.assertEquals(changedUser.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());
    Assertions.assertEquals(BASE_URI + EndpointPaths.USERS + "/" + returnedUser.getId().get(),
                            returnedUser.getMeta().get().getLocation().get());
  }

  /**
   * this test will verify that a delete request is processed successfully if parameters are correctly set
   */
  @Test
  public void testDeleteResource()
  {
    final String userName = "chuck_norris";
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    final User user = User.builder().id(id).userName(userName).meta(meta).build();
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    userHandler.getInMemoryMap().put(id, user);
    Assertions.assertEquals(1, userHandler.getInMemoryMap().size());
    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.DELETE, user.toString());
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
    DeleteResponse deleteResponse = (DeleteResponse)scimResponse;
    Assertions.assertTrue(deleteResponse.isEmpty());
    Assertions.assertEquals(HttpStatus.SC_NO_CONTENT, deleteResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());
  }

  /**
   * this test will create 500 users and will then send a filter request with get that verifies that the request
   * is correctly processed
   */
  @Test
  public void testQueryResourcesWithGet()
  {
    int maxUsers = 500;
    serviceProvider.getFilterConfig().setSupported(true);
    serviceProvider.getFilterConfig().setMaxResults(maxUsers);
    resourceEndpoint.getResourceTypeFactory()
                    .getResourceType(EndpointPaths.USERS)
                    .setFilterExtension(new ResourceType.FilterExtension(true));
    int counter = 0;
    final String searchValue = "0";
    for ( int i = 0 ; i < maxUsers ; i++ )
    {
      final String id = UUID.randomUUID().toString();
      Meta meta = Meta.builder()
                      .resourceType(ResourceTypeNames.USER)
                      .created(LocalDateTime.now())
                      .lastModified(LocalDateTime.now())
                      .build();
      User user = User.builder().id(id).userName(id).meta(meta).build();
      if (id.startsWith(searchValue))
      {
        counter++;
      }
      userHandler.getInMemoryMap().put(id, user);
    }
    log.warn("counter: {}", counter);
    final String url = BASE_URI + EndpointPaths.USERS
                       + String.format("?startIndex=1&count=%d&filter=%s",
                                       maxUsers,
                                       "userName sw \"" + searchValue + "\"");
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.SC_OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L), Mockito.eq(maxUsers), Mockito.any(), Mockito.any(), Mockito.any());
    log.warn(listResponse.getListedResources()
                         .stream()
                         .map(userNode -> userNode.get(AttributeNames.RFC7643.ID).textValue())
                         .collect(Collectors.joining("\n")));
    log.warn(listResponse.toPrettyString());
  }

  /**
   * this test will create 500 users and will then send a filter request with post that verifies that the
   * request is correctly processed
   */
  @Test
  public void testQueryResourcesWithPost()
  {
    int maxUsers = 500;
    serviceProvider.getFilterConfig().setSupported(true);
    serviceProvider.getFilterConfig().setMaxResults(maxUsers);
    resourceEndpoint.getResourceTypeFactory()
                    .getResourceType(EndpointPaths.USERS)
                    .setFilterExtension(new ResourceType.FilterExtension(true));
    int counter = 0;
    final String searchValue = "0";
    for ( int i = 0 ; i < maxUsers ; i++ )
    {
      final String id = UUID.randomUUID().toString();
      Meta meta = Meta.builder()
                      .resourceType(ResourceTypeNames.USER)
                      .created(LocalDateTime.now())
                      .lastModified(LocalDateTime.now())
                      .build();
      User user = User.builder().id(id).userName(id).meta(meta).build();
      if (id.startsWith(searchValue))
      {
        counter++;
      }
      userHandler.getInMemoryMap().put(id, user);
    }
    log.warn("counter: {}", counter);
    final String url = BASE_URI + EndpointPaths.USERS + EndpointPaths.SEARCH;
    SearchRequest searchRequest = SearchRequest.builder()
                                               .startIndex(1L)
                                               .count(maxUsers)
                                               .filter("userName sw \"" + searchValue + "\"")
                                               .build();
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, searchRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.SC_OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L), Mockito.eq(maxUsers), Mockito.any(), Mockito.any(), Mockito.any());
    log.warn(listResponse.getListedResources()
                         .stream()
                         .map(userNode -> userNode.get(AttributeNames.RFC7643.ID).textValue())
                         .collect(Collectors.joining("\n")));
    log.warn(listResponse.toPrettyString());
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
    final String url = BASE_URI + EndpointPaths.BULK;
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    Assertions.assertEquals(maxOperations, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.SC_OK, bulkResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).createResource(Mockito.any());

    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      Assertions.assertEquals(HttpMethod.POST, bulkResponseOperation.getMethod());
      Assertions.assertEquals(HttpStatus.SC_CREATED, bulkResponseOperation.getStatus());
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
    scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.SC_OK, bulkResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).updateResource(Mockito.any());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).deleteResource(Mockito.any());

    List<BulkResponseOperation> responseOperations = bulkResponse.getBulkResponseOperations();
    for ( BulkResponseOperation bulkResponseOperation : responseOperations.subList(0, maxOperations - 1) )
    {
      Assertions.assertEquals(HttpMethod.PUT, bulkResponseOperation.getMethod());
      Assertions.assertEquals(HttpStatus.SC_OK, bulkResponseOperation.getStatus());
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
      Assertions.assertEquals(HttpStatus.SC_NO_CONTENT, bulkResponseOperation.getStatus());
      Assertions.assertFalse(bulkResponseOperation.getResponse().isPresent());
      Assertions.assertTrue(bulkResponseOperation.getBulkId().isPresent());
      Assertions.assertTrue(bulkResponseOperation.getLocation().isPresent());
    }
  }

  /**
   * creates delete requests for the create operations
   *
   * @param createUsers the create operations to access the ids
   */
  private List<BulkRequestOperation> getDeleteUserBulkOperations(Collection<User> createUsers)
  {
    return getDeleteUserBulkOperations(createUsers, HttpMethod.DELETE);
  }

  /**
   * creates delete requests for the create operations
   *
   * @param createUsers the create operations to access the ids
   */
  private List<BulkRequestOperation> getDeleteUserBulkOperations(Collection<User> createUsers, HttpMethod httpMethod)
  {
    List<BulkRequestOperation> operations = new ArrayList<>();
    for ( User createdUser : createUsers )
    {
      final String id = createdUser.getId().get();
      operations.add(BulkRequestOperation.builder()
                                         .bulkId(UUID.randomUUID().toString())
                                         .method(httpMethod)
                                         .path(EndpointPaths.USERS + "/" + id)
                                         .build());
    }
    return operations;
  }

  /**
   * creates update requests for the create operations
   *
   * @param createdUsers the create operations to access the ids
   */
  private List<BulkRequestOperation> getUpdateUserBulkOperations(Collection<User> createdUsers)
  {
    return getUpdateUserBulkOperations(createdUsers, HttpMethod.PUT);
  }

  /**
   * creates update requests for the create operations
   *
   * @param createdUsers the create operations to access the ids
   */
  private List<BulkRequestOperation> getUpdateUserBulkOperations(Collection<User> createdUsers, HttpMethod httpMethod)
  {
    List<BulkRequestOperation> operations = new ArrayList<>();
    for ( User createdUser : createdUsers )
    {
      final String id = createdUser.getId().get();
      final String newUserName = UUID.randomUUID().toString();
      final User user = User.builder().userName(newUserName).nickName(newUserName).build();
      operations.add(BulkRequestOperation.builder()
                                         .method(httpMethod)
                                         .path(EndpointPaths.USERS + "/" + id)
                                         .data(user.toString())
                                         .build());
    }
    return operations;
  }

  /**
   * creates several create operations for a bulk operations
   *
   * @param numberOfOperations number of operations to create
   */
  protected List<BulkRequestOperation> getCreateUserBulkOperations(int numberOfOperations)
  {
    List<BulkRequestOperation> operations = new ArrayList<>();
    for ( int i = 0 ; i < numberOfOperations ; i++ )
    {
      final String username = UUID.randomUUID().toString();
      final User user = User.builder().userName(username).build();
      operations.add(BulkRequestOperation.builder()
                                         .bulkId(UUID.randomUUID().toString())
                                         .method(HttpMethod.POST)
                                         .path(EndpointPaths.USERS)
                                         .data(user.toString())
                                         .build());
    }
    return operations;
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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, createOperations.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    MatcherAssert.assertThat(errorResponse.getDetail().get(),
                             Matchers.containsString("document does not have a 'schemas'-attribute"));
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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    MatcherAssert.assertThat(errorResponse.getDetail().get(),
                             Matchers.equalTo("missing 'bulkId' on BULK-POST request"));
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
        throw new IllegalStateException("not yet supported");
    }

    operations.forEach(operation -> operation.setBulkId(null));
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.SC_OK, bulkResponse.getHttpStatus());
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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(failOnErrors, bulkResponse.getBulkResponseOperations().size());

    bulkResponse.getBulkResponseOperations().forEach(operation -> {
      Assertions.assertTrue(operation.getResponse().isPresent());
      ErrorResponse errorResponse = operation.getResponse().get();
      MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                               Matchers.typeCompatibleWith(ResponseException.class));
      Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getHttpStatus());
      Assertions.assertEquals("something bad", errorResponse.getDetail().get());
    });

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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(NotImplementedException.class));
    MatcherAssert.assertThat(errorResponse.getDetail().get(),
                             Matchers.equalTo("bulk is not supported by this service provider"));
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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    Assertions.assertEquals("too many operations maximum number of operations is '" + maxOperations + "'",
                            errorResponse.getDetail().get());
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

    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    Assertions.assertEquals("request body too large with '" + (maxPayloadSize + 1) + "'-bytes "
                            + "maximum payload size is '" + maxPayloadSize + "'",
                            errorResponse.getDetail().get());
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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    Assertions.assertEquals("the attribute 'urn:ietf:params:scim:api:messages:2.0:BulkRequest:Operations' "
                            + "is required \n\tmutability: 'WRITE_ONLY'\n\treturned: 'NEVER'",
                            errorResponse.getDetail().get());
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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(maxOperations, bulkResponse.getBulkResponseOperations().size());
    Assertions.assertFalse(bulkResponse.getBulkResponseOperations().get(0).getLocation().isPresent());
  }

  /**
   * verifies that the bulk operation fails and a {@link ErrorResponse} is returned if the bulk operation is
   * questioned with the wrong http method
   */
  @ParameterizedTest
  @ValueSource(strings = {"GET", "PUT", "PATCH", "DELETE"})
  public void testAccessBulkEndpointWithOtherHttpMethodThanPost(HttpMethod httpMethod)
  {
    final int maxOperations = 1;
    serviceProvider.getBulkConfig().setSupported(true);
    serviceProvider.getBulkConfig().setMaxOperations(maxOperations);
    serviceProvider.getBulkConfig().setMaxPayloadSize(Long.MAX_VALUE);
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, httpMethod, bulkRequest.toString());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    MatcherAssert.assertThat(errorResponse.getDetail().get(),
                             Matchers.equalTo("Bulk endpoint can only be reached with a HTTP-POST request"));
  }
}
