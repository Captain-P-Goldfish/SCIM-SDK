package de.gold.scim.endpoints;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
import de.gold.scim.request.SearchRequest;
import de.gold.scim.resources.ServiceProvider;
import de.gold.scim.resources.User;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.response.CreateResponse;
import de.gold.scim.response.DeleteResponse;
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
}
