package de.captaingoldfish.scim.sdk.server.endpoints;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResponseException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.request.SearchRequest;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.SortConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;
import de.captaingoldfish.scim.sdk.common.response.DeleteResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.GetResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.response.UpdateResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.features.EndpointType;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 26.10.2019 - 00:32 <br>
 * <br>
 */
@Slf4j
public class ResourceEndpointTest extends AbstractBulkTest implements FileReferences
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
   * the http header map that is validated on a request
   */
  private Map<String, String> httpHeaders = new HashMap<>();

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    serviceProvider = ServiceProvider.builder().build();
    userHandler = Mockito.spy(new UserHandlerImpl(true));
    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, user.toString(), httpHeaders);
    Assertions.assertEquals(1, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
    CreateResponse createResponse = (CreateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.CREATED, createResponse.getHttpStatus());
    User createdUser = JsonHelper.copyResourceToObject(createResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), createdUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    Assertions.assertEquals(BASE_URI + EndpointPaths.USERS + "/" + createdUser.getId().get(),
                            createdUser.getMeta().get().getLocation().get());
  }

  /**
   * this test will verify that a creation request is processed successfully if parameters are correctly set and
   * a resource type consumer is present
   */
  @Test
  public void testCreateResourceWithResourceTypeConsumer()
  {
    final String userName = "chuck_norris";
    final User user = User.builder().userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS;
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<ResourceType> resourceTypeConsumer = resourceType -> {
      wasCalled.set(true);
      Assertions.assertEquals(EndpointPaths.USERS, resourceType.getEndpoint());
    };
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               user.toString(),
                                                               httpHeaders,
                                                               resourceTypeConsumer);
    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(1, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
    CreateResponse createResponse = (CreateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.CREATED, createResponse.getHttpStatus());
    User createdUser = JsonHelper.copyResourceToObject(createResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), createdUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    Assertions.assertEquals(BASE_URI + EndpointPaths.USERS + "/" + createdUser.getId().get(),
                            createdUser.getMeta().get().getLocation().get());
  }

  /**
   * this test will verify that if a developer forgot to add an extension uri within the schemas attribute that
   * this schema will be added automatically by the scim-server framework
   */
  @Test
  public void testCreateResourceWithMissingExtensionUriInSchemas()
  {
    final String userName = "chuck_norris";
    final EnterpriseUser enterpriseUser = EnterpriseUser.builder().costCenter("cost center").build();
    final User user = User.builder()
                          .id(UUID.randomUUID().toString())
                          .userName(userName)
                          .enterpriseUser(enterpriseUser)
                          .meta(Meta.builder()
                                    .created(Instant.now())
                                    .lastModified(Instant.now())
                                    .resourceType(ResourceTypeNames.USER)
                                    .build())
                          .build();
    final String userRequest = user.toString();
    final String url = BASE_URI + EndpointPaths.USERS;

    user.removeSchema(SchemaUris.ENTERPRISE_USER_URI);
    Mockito.doReturn(user).when(userHandler).createResource(Mockito.any(), Mockito.isNull());

    MatcherAssert.assertThat(new ArrayList<>(user.getSchemas()),
                             Matchers.not(Matchers.hasItem(SchemaUris.ENTERPRISE_USER_URI)));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, userRequest, httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
    CreateResponse createResponse = (CreateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.CREATED, createResponse.getHttpStatus());
    User createdUser = JsonHelper.copyResourceToObject(createResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), createdUser.getUserName().get());
    MatcherAssert.assertThat(new ArrayList<>(createdUser.getSchemas()),
                             Matchers.hasItem(SchemaUris.ENTERPRISE_USER_URI));
    Assertions.assertTrue(createdUser.getEnterpriseUser().isPresent());
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, user.toString(), httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, getResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(getResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    Assertions.assertEquals(BASE_URI + EndpointPaths.USERS + "/" + returnedUser.getId().get(),
                            returnedUser.getMeta().get().getLocation().get());
  }

  /**
   * this test will verify that a get request is processed successfully if parameters are correctly set and a
   * resource type consumer is set
   */
  @Test
  public void testGetResourceWithResourceTypeConsumer()
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

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<ResourceType> resourceTypeConsumer = resourceType -> {
      wasCalled.set(true);
      Assertions.assertEquals(EndpointPaths.USERS, resourceType.getEndpoint());
    };
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               user.toString(),
                                                               httpHeaders,
                                                               resourceTypeConsumer);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, getResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(getResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PUT,
                                                               changedUser.toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(updateResponse, User.class);
    Assertions.assertNotEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Assertions.assertEquals(changedUser.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    Assertions.assertEquals(BASE_URI + EndpointPaths.USERS + "/" + returnedUser.getId().get(),
                            returnedUser.getMeta().get().getLocation().get());
  }

  /**
   * this test will verify that an update request is processed successfully if parameters are correctly set and
   * if a resource type consumer is set
   */
  @Test
  public void testUpdateResourceWithResourceTypeConsumer()
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

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<ResourceType> resourceTypeConsumer = resourceType -> {
      wasCalled.set(true);
      Assertions.assertEquals(EndpointPaths.USERS, resourceType.getEndpoint());
    };
    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PUT,
                                                               changedUser.toString(),
                                                               httpHeaders,
                                                               resourceTypeConsumer);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(updateResponse, User.class);
    Assertions.assertNotEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Assertions.assertEquals(changedUser.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.DELETE, user.toString(), httpHeaders);
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
    DeleteResponse deleteResponse = (DeleteResponse)scimResponse;
    Assertions.assertTrue(deleteResponse.isEmpty());
    Assertions.assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
  }

  /**
   * this test will verify that a delete request is processed successfully if parameters are correctly set and
   * if a resource type consumer is set
   */
  @Test
  public void testDeleteResourceWithResourceTypeConsumer()
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

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<ResourceType> resourceTypeConsumer = resourceType -> {
      wasCalled.set(true);
      Assertions.assertEquals(EndpointPaths.USERS, resourceType.getEndpoint());
    };
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.DELETE,
                                                               user.toString(),
                                                               httpHeaders,
                                                               resourceTypeConsumer);
    Assertions.assertTrue(wasCalled.get());

    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
    DeleteResponse deleteResponse = (DeleteResponse)scimResponse;
    Assertions.assertTrue(deleteResponse.isEmpty());
    Assertions.assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
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
                    .setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).build());
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(maxUsers),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    log.warn(listResponse.getListedResources()
                         .stream()
                         .map(userNode -> userNode.get(AttributeNames.RFC7643.ID).textValue())
                         .collect(Collectors.joining("\n")));
    log.warn(listResponse.toPrettyString());
  }

  /**
   * this test will create 5 users and will then send a filter request with get that verifies that the request
   * is correctly processed and a resource type consumer is set
   */
  @Test
  public void testQueryResourcesWithGetWithResourceTypeConsumer()
  {
    int maxUsers = 5;
    serviceProvider.getFilterConfig().setSupported(true);
    serviceProvider.getFilterConfig().setMaxResults(maxUsers);
    resourceEndpoint.getResourceTypeFactory()
                    .getResourceType(EndpointPaths.USERS)
                    .setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).build());
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

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<ResourceType> resourceTypeConsumer = resourceType -> {
      wasCalled.set(true);
      Assertions.assertEquals(EndpointPaths.USERS, resourceType.getEndpoint());
    };
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               resourceTypeConsumer);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(maxUsers),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
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
                    .setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).build());
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               searchRequest.toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(maxUsers),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    log.warn(listResponse.getListedResources()
                         .stream()
                         .map(userNode -> userNode.get(AttributeNames.RFC7643.ID).textValue())
                         .collect(Collectors.joining("\n")));
    log.warn(listResponse.toPrettyString());
  }

  /**
   * this test will create 5 users and will then send a filter request with post that verifies that the request
   * is correctly processed if a resource type consumer is set
   */
  @Test
  public void testQueryResourcesWithPostWithResourceTypeConsumerSet()
  {
    int maxUsers = 5;
    serviceProvider.getFilterConfig().setSupported(true);
    serviceProvider.getFilterConfig().setMaxResults(maxUsers);
    resourceEndpoint.getResourceTypeFactory()
                    .getResourceType(EndpointPaths.USERS)
                    .setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).build());
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

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<ResourceType> resourceTypeConsumer = resourceType -> {
      wasCalled.set(true);
      Assertions.assertEquals(EndpointPaths.USERS, resourceType.getEndpoint());
    };
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               searchRequest.toString(),
                                                               httpHeaders,
                                                               resourceTypeConsumer);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).getResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(maxUsers),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders);
    Assertions.assertEquals(maxOperations, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).createResource(Mockito.any(), Mockito.isNull());

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
    scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, bulkRequest.toString(), httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).updateResource(Mockito.any(), Mockito.isNull());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).deleteResource(Mockito.any(), Mockito.isNull());

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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               createOperations.toString(),
                                                               httpHeaders);
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders);
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
        throw new IllegalStateException("not yet supported");
    }

    operations.forEach(operation -> operation.setBulkId(null));
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders);
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
           .createResource(Mockito.any(), Mockito.isNull());
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder()
                                         .failOnErrors(failOnErrors)
                                         .bulkRequestOperation(createOperations)
                                         .build();
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders);
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders);
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders);
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders);
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders);
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
           .createResource(Mockito.any(), Mockito.isNull());
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders);
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, httpMethod, bulkRequest.toString(), httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    MatcherAssert.assertThat(errorResponse.getDetail().get(),
                             Matchers.equalTo("Bulk endpoint can only be reached with a HTTP-POST request"));
  }

  /**
   * Verifies that a resource can successfully be patched
   */
  @Test
  public void testPatchResource()
  {
    serviceProvider.getPatchConfig().setSupported(true);

    final Supplier<String> baseUrl = () -> "https://localhost/scim/v2";
    final String path = "name";
    Name name = Name.builder().givenName("goldfish").familyName("captain").build();
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .valueNode(name)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").nickName("captain").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);


    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    User copiedUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    copiedUser.setName(name);
    Assertions.assertEquals(copiedUser, updateResponse);

    GetResponse getResponse = (GetResponse)resourceEndpoint.getResource(EndpointPaths.USERS, id, null, baseUrl);
    Assertions.assertEquals(updateResponse, getResponse);
    Assertions.assertEquals(copiedUser, getResponse);
  }

  /**
   * Verifies that a resource can successfully be patched if a resource type consumer is set
   */
  @Test
  public void testPatchResourceWithResourceTypeConsumer()
  {
    serviceProvider.getPatchConfig().setSupported(true);

    final Supplier<String> baseUrl = () -> "https://localhost/scim/v2";
    final String path = "name";
    Name name = Name.builder().givenName("goldfish").familyName("captain").build();
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .valueNode(name)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").nickName("captain").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);


    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<ResourceType> resourceTypeConsumer = resourceType -> {
      wasCalled.set(true);
      Assertions.assertEquals(EndpointPaths.USERS, resourceType.getEndpoint());
    };
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders,
                                                               resourceTypeConsumer);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    User copiedUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    copiedUser.setName(name);
    Assertions.assertEquals(copiedUser, updateResponse);

    GetResponse getResponse = (GetResponse)resourceEndpoint.getResource(EndpointPaths.USERS, id, null, baseUrl);
    Assertions.assertEquals(updateResponse, getResponse);
    Assertions.assertEquals(copiedUser, getResponse);
  }

  /**
   * Verifies that an {@link ErrorResponse} is returned if patch is not supported
   */
  @Test
  public void testPatchResourceWithoutPatchSupport()
  {
    serviceProvider.getPatchConfig().setSupported(false);

    final String path = "name";
    Name name = Name.builder().givenName("goldfish").familyName("captain").build();
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .valueNode(name)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").nickName("captain").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);


    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, errorResponse.getHttpStatus());
  }

  /**
   * Verifies that only the minimal set plus the attribute is returned after the patch operation
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testPatchResourceWithAttributesParameter(PatchOp patchOp)
  {
    serviceProvider.getPatchConfig().setSupported(true);

    final String path = "nickName";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(Arrays.asList("captain"))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").active(true).meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);


    final String url = BASE_URI + EndpointPaths.USERS + "/" + id + "?attributes=userName";
    final List<String> minimalAttributeSet = Arrays.asList("schemas", "id", "userName", "nickName");

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());

    Assertions.assertEquals(minimalAttributeSet.size(), updateResponse.size(), updateResponse.toPrettyString());
    for ( String attributeName : minimalAttributeSet )
    {
      Assertions.assertNotNull(updateResponse.get(attributeName), updateResponse.toPrettyString());
    }
  }

  /**
   * Verifies that only the minimal set plus the attribute is returned after the patch operation. In this case
   * the resource itself is used as value
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testPatchResourceWithAttributesParameterUseResourceAsValue(PatchOp patchOp)
  {
    serviceProvider.getPatchConfig().setSupported(true);

    User value = User.builder().nickName("captain").build();
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .values(Arrays.asList(value.toString()))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").active(true).meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);


    final String url = BASE_URI + EndpointPaths.USERS + "/" + id + "?attributes=userName";
    final List<String> minimalAttributeSet = Arrays.asList("schemas", "id", "userName", "nickName");

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());

    Assertions.assertEquals(minimalAttributeSet.size(), updateResponse.size(), updateResponse.toPrettyString());
    for ( String attributeName : minimalAttributeSet )
    {
      Assertions.assertNotNull(updateResponse.get(attributeName), updateResponse.toPrettyString());
    }
  }

  /**
   * Verifies that only the minimal set plus the attribute is returned after the patch operation. In this case
   * the resource itself is used as value. This test will add just the schema extension
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testPatchResourceWithAttributesParameterUseResourceAsValueWithExtension(PatchOp patchOp)
  {
    serviceProvider.getPatchConfig().setSupported(true);

    User value = User.builder().enterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build()).build();
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .values(Arrays.asList(value.toString()))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").active(true).meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);


    final String url = BASE_URI + EndpointPaths.USERS + "/" + id + "?attributes=userName";
    final List<String> minimalAttributeSet = Arrays.asList("schemas", "id", "userName", SchemaUris.ENTERPRISE_USER_URI);

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());

    Assertions.assertEquals(minimalAttributeSet.size(), updateResponse.size(), updateResponse.toPrettyString());
    for ( String attributeName : minimalAttributeSet )
    {
      Assertions.assertNotNull(updateResponse.get(attributeName), updateResponse.toPrettyString());
    }
    JsonNode enterpriseUser = updateResponse.get(SchemaUris.ENTERPRISE_USER_URI);
    Assertions.assertNotNull(enterpriseUser.get(AttributeNames.RFC7643.COST_CENTER));
  }

  /**
   * this test verifies that already existing attributes in extensions that have not been modified and were not
   * in the attributes parameter will not be returned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testPatchResourceWithAttributesParameterUseResourceAsValueWithExtension2(PatchOp patchOp)
  {
    serviceProvider.getPatchConfig().setSupported(true);

    User value = User.builder().enterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build()).build();
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .values(Arrays.asList(value.toString()))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder()
                    .id(id)
                    .userName("goldfish")
                    .active(true)
                    .enterpriseUser(EnterpriseUser.builder().department("department").build())
                    .meta(meta)
                    .build();
    userHandler.getInMemoryMap().put(id, user);


    final String url = BASE_URI + EndpointPaths.USERS + "/" + id + "?attributes=userName";
    final List<String> minimalAttributeSet = Arrays.asList("schemas", "id", "userName", SchemaUris.ENTERPRISE_USER_URI);

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());

    Assertions.assertEquals(minimalAttributeSet.size(), updateResponse.size(), updateResponse.toPrettyString());
    for ( String attributeName : minimalAttributeSet )
    {
      Assertions.assertNotNull(updateResponse.get(attributeName), updateResponse.toPrettyString());
    }
    JsonNode enterpriseUser = updateResponse.get(SchemaUris.ENTERPRISE_USER_URI);
    Assertions.assertNotNull(enterpriseUser.get(AttributeNames.RFC7643.COST_CENTER));
    Assertions.assertNull(enterpriseUser.get(AttributeNames.RFC7643.DEPARTMENT));
  }

  /**
   * verifies that a {@link BadRequestException} is returned if the client tries to access the create endpoint
   * with the wrong content type
   */
  @Test
  public void testCreateFailsWithWrongContentType()
  {
    final String url = BASE_URI + EndpointPaths.USERS;
    httpHeaders.clear();
    final String contentType = "application/json";
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, contentType);
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               User.builder().build().toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatus());
    Assertions.assertEquals("Invalid content type. Was '" + contentType + "' but should be "
                            + HttpHeader.SCIM_CONTENT_TYPE,
                            errorResponse.getDetail().get());
  }

  /**
   * verifies that a {@link BadRequestException} is returned if the client tries to access the list-users
   * endpoint with the wrong content type
   */
  @Test
  public void testListUsersWithPostFailsWithWrongContentType()
  {
    final String url = BASE_URI + EndpointPaths.BULK;
    httpHeaders.clear();
    final String contentType = "application/json";
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, contentType);
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               SearchRequest.builder().build().toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatus());
    Assertions.assertEquals("Invalid content type. Was '" + contentType + "' but should be "
                            + HttpHeader.SCIM_CONTENT_TYPE,
                            errorResponse.getDetail().get());
  }

  /**
   * verifies that a {@link BadRequestException} is returned if the client tries to access the bulk endpoint
   * with the wrong content type
   */
  @Test
  public void testBulkFailsWithWrongContentType()
  {
    final String url = BASE_URI + EndpointPaths.BULK;
    httpHeaders.clear();
    final String contentType = "application/json";
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, contentType);
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               BulkRequest.builder().build().toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatus());
    Assertions.assertEquals("Invalid content type. Was '" + contentType + "' but should be "
                            + HttpHeader.SCIM_CONTENT_TYPE,
                            errorResponse.getDetail().get());
  }

  /**
   * verifies that a {@link BadRequestException} is returned if the client tries to access the update users
   * endpoint with the wrong content type
   */
  @Test
  public void testPutFailsWithWrongContentType()
  {
    final String url = BASE_URI + EndpointPaths.USERS + "/123456";
    httpHeaders.clear();
    final String contentType = "application/json";
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, contentType);
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PUT,
                                                               User.builder().build().toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatus());
    Assertions.assertEquals("Invalid content type. Was '" + contentType + "' but should be "
                            + HttpHeader.SCIM_CONTENT_TYPE,
                            errorResponse.getDetail().get());
  }

  /**
   * verifies that a {@link BadRequestException} is returned if the client tries to access the patch users
   * endpoint with the wrong content type
   */
  @Test
  public void testPatchFailsWithWrongContentType()
  {
    final String url = BASE_URI + EndpointPaths.USERS + "/123456";
    httpHeaders.clear();
    final String contentType = "application/json";
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, contentType);
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               PatchOpRequest.builder().build().toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatus());
    Assertions.assertEquals("Invalid content type. Was '" + contentType + "' but should be "
                            + HttpHeader.SCIM_CONTENT_TYPE,
                            errorResponse.getDetail().get());
  }

  /**
   * this test will verify that the service provider configuration can be accessed without any errors
   */
  @Test
  public void testGetServiceProviderConfig()
  {
    final String url = BASE_URI + EndpointPaths.SERVICE_PROVIDER_CONFIG;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
  }

  /**
   * this test will verify that the resource types can be accessed without any errors
   */
  @Test
  public void testGetResourceTypes()
  {
    final String url = BASE_URI + EndpointPaths.RESOURCE_TYPES;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
  }

  /**
   * this test will verify that the schemas can be accessed without any errors
   */
  @Test
  public void testGetSchemas()
  {
    final String url = BASE_URI + EndpointPaths.SCHEMAS;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
  }

  /**
   * this test will verify that the different endpoint can simply be disabled by using the resource type feature
   */
  @TestFactory
  public List<DynamicTest> testDisabledEndpoints()
  {
    final String url = BASE_URI + EndpointPaths.RESOURCE_TYPES;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);

    ResourceType resourceType = resourceEndpoint.getResourceTypeFactory().getResourceType(EndpointPaths.RESOURCE_TYPES);
    EndpointControlFeature endpointControlFeature = resourceType.getFeatures().getEndpointControlFeature();

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check create is disabled", () -> {
      endpointControlFeature.setCreateDisabled(true);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("create is not supported for resource type '" + resourceType.getName() + "'",
                              ex.getDetail());
      endpointControlFeature.setCreateDisabled(false);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check list is disabled", () -> {
      endpointControlFeature.setGetDisabled(true);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.GET, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("get is not supported for resource type '" + resourceType.getName() + "'",
                              ex.getDetail());
      endpointControlFeature.setGetDisabled(false);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check list is disabled", () -> {
      endpointControlFeature.setListDisabled(true);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("list is not supported for resource type '" + resourceType.getName() + "'",
                              ex.getDetail());
      endpointControlFeature.setListDisabled(false);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check update is disabled", () -> {
      endpointControlFeature.setUpdateDisabled(true);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.PUT, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("update is not supported for resource type '" + resourceType.getName() + "'",
                              ex.getDetail());
      endpointControlFeature.setUpdateDisabled(false);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check patch is disabled", () -> {
      endpointControlFeature.setUpdateDisabled(true);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.PATCH, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("update is not supported for resource type '" + resourceType.getName() + "'",
                              ex.getDetail());
      endpointControlFeature.setUpdateDisabled(false);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check delete is disabled", () -> {
      endpointControlFeature.setDeleteDisabled(true);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.DELETE, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("delete is not supported for resource type '" + resourceType.getName() + "'",
                              ex.getDetail());
      endpointControlFeature.setDeleteDisabled(false);
    }));
    /* ************************************************************************************************************/
    return dynamicTests;
  }

  /**
   * will verify that all methods are inaccessible if the resource type itself has been disabled
   */
  @TestFactory
  public List<DynamicTest> testResourceTypeDisabled()
  {
    final String url = BASE_URI + EndpointPaths.RESOURCE_TYPES;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);

    ResourceType resourceType = resourceEndpoint.getResourceTypeFactory().getResourceType(EndpointPaths.RESOURCE_TYPES);
    resourceType.setDisabled(true);

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check create is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check list is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check update is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.PUT, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check patch is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.PATCH, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check delete is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.DELETE, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    return dynamicTests;
  }

  /**
   * will verify that all methods are inaccessible if the resource type has been disabled on the feature method
   */
  @TestFactory
  public List<DynamicTest> testResourceTypeFeatureIsDisabled()
  {
    final String url = BASE_URI + EndpointPaths.SERVICE_PROVIDER_CONFIG;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);

    ResourceType resourceType = resourceEndpoint.getResourceTypeFactory()
                                                .getResourceType(EndpointPaths.SERVICE_PROVIDER_CONFIG);
    resourceType.getFeatures().setResourceTypeDisabled(true);

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check create is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check list is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check update is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.PUT, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check patch is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.PATCH, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check delete is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.DELETE, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    return dynamicTests;
  }

  /**
   * will verify that all methods are inaccessible if all single methods have been disabled
   */
  @TestFactory
  public List<DynamicTest> testResourceTypeAllSingleEndpointAreDisabled()
  {
    final String url = BASE_URI + EndpointPaths.SERVICE_PROVIDER_CONFIG;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);

    ResourceType resourceType = resourceEndpoint.getResourceTypeFactory()
                                                .getResourceType(EndpointPaths.SERVICE_PROVIDER_CONFIG);
    resourceType.getFeatures().getEndpointControlFeature().setCreateDisabled(true);
    resourceType.getFeatures().getEndpointControlFeature().setGetDisabled(true);
    resourceType.getFeatures().getEndpointControlFeature().setListDisabled(true);
    resourceType.getFeatures().getEndpointControlFeature().setUpdateDisabled(true);
    resourceType.getFeatures().getEndpointControlFeature().setDeleteDisabled(true);

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check create is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check list is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check update is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.PUT, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check patch is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.PATCH, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check delete is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456", HttpMethod.DELETE, null, httpHeaders);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    return dynamicTests;
  }

  /**
   * builds a fully configured service provider configuration
   */
  private ServiceProvider getServiceProvider()
  {
    AuthenticationScheme authScheme = AuthenticationScheme.builder()
                                                          .name("OAuth Bearer Token")
                                                          .description("Authentication scheme using the OAuth "
                                                                       + "Bearer Token Standard")
                                                          .specUri("http://www.rfc-editor.org/info/rfc6750")
                                                          .type("oauthbearertoken")
                                                          .build();
    return ServiceProvider.builder()
                          .filterConfig(FilterConfig.builder().supported(true).maxResults(50).build())
                          .sortConfig(SortConfig.builder().supported(true).build())
                          .changePasswordConfig(ChangePasswordConfig.builder().supported(true).build())
                          .bulkConfig(BulkConfig.builder().supported(true).maxOperations(10).build())
                          .patchConfig(PatchConfig.builder().supported(true).build())
                          .authenticationSchemes(Collections.singletonList(authScheme))
                          .eTagConfig(ETagConfig.builder().supported(true).build())
                          .build();
  }

  /**
   * this test will verify that a client cannot access protected endpoints without the proper roles
   */
  @TestFactory
  public List<DynamicTest> testUnauthorized()
  {
    final String url = BASE_URI + EndpointPaths.USERS;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    EndpointDefinition endpointDefinition = new UserEndpointDefinition(new UserHandlerImpl(true));
    endpointDefinition.setResourceType(JsonHelper.loadJsonDocument(USER_AUTHORIZED_RESOURCE_TYPE));
    endpointDefinition.setResourceSchemaExtensions(Collections.emptyList());
    ResourceType resourceType = resourceEndpoint.registerEndpoint(endpointDefinition);
    final ClientAuthorization clientAuthorization = new ClientAuthorization("goldfish", "user");

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("create unauthorized",
                                         url,
                                         resourceType,
                                         HttpMethod.POST,
                                         null,
                                         EndpointType.CREATE));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("create with roles unauthorized",
                                         url,
                                         resourceType,
                                         HttpMethod.POST,
                                         clientAuthorization,
                                         EndpointType.CREATE));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("get unauthorized",
                                         url + "/123456",
                                         resourceType,
                                         HttpMethod.GET,
                                         null,
                                         EndpointType.GET));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("get with roles unauthorized",
                                         url + "/123456",
                                         resourceType,
                                         HttpMethod.GET,
                                         clientAuthorization,
                                         EndpointType.GET));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("list unauthorized",
                                         url,
                                         resourceType,
                                         HttpMethod.GET,
                                         null,
                                         EndpointType.LIST));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("list with roles unauthorized",
                                         url,
                                         resourceType,
                                         HttpMethod.GET,
                                         clientAuthorization,
                                         EndpointType.LIST));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("list-post unauthorized",
                                         url + "/.search",
                                         resourceType,
                                         HttpMethod.POST,
                                         null,
                                         EndpointType.LIST));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("list-post with roles unauthorized",
                                         url + "/.search",
                                         resourceType,
                                         HttpMethod.POST,
                                         clientAuthorization,
                                         EndpointType.LIST));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("update unauthorized",
                                         url + "/123456",
                                         resourceType,
                                         HttpMethod.PUT,
                                         null,
                                         EndpointType.UPDATE));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("update with roles unauthorized",
                                         url + "/123456",
                                         resourceType,
                                         HttpMethod.PUT,
                                         clientAuthorization,
                                         EndpointType.UPDATE));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("patch unauthorized",
                                         url + "/123456",
                                         resourceType,
                                         HttpMethod.PATCH,
                                         null,
                                         EndpointType.UPDATE));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("patch with roles unauthorized",
                                         url + "/123456",
                                         resourceType,
                                         HttpMethod.PATCH,
                                         clientAuthorization,
                                         EndpointType.UPDATE));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("delete unauthorized",
                                         url + "/123456",
                                         resourceType,
                                         HttpMethod.DELETE,
                                         null,
                                         EndpointType.DELETE));
    /* ************************************************************************************************************/
    dynamicTests.add(getUnauthorizedTest("delete with roles unauthorized",
                                         url + "/123456",
                                         resourceType,
                                         HttpMethod.DELETE,
                                         clientAuthorization,
                                         EndpointType.DELETE));
    /* ************************************************************************************************************/
    return dynamicTests;
  }

  /**
   * this test will verify that a client can access protected endpoints if the proper roles are granted
   */
  @TestFactory
  public List<DynamicTest> testAuthorized()
  {
    final String url = BASE_URI + EndpointPaths.USERS;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    EndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    endpointDefinition.setResourceType(JsonHelper.loadJsonDocument(USER_AUTHORIZED_RESOURCE_TYPE));
    endpointDefinition.setResourceSchemaExtensions(Collections.emptyList());
    resourceEndpoint.registerEndpoint(endpointDefinition);
    final ClientAuthorization clientAuthorization = new ClientAuthorization("goldfish", "create", "get", "update",
                                                                            "delete");
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("test").nickName("test").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("create authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.POST,
                                                                 User.builder().userName("test").build().toString(),
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("get authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).getResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list-post authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/.search",
                                                                 HttpMethod.POST,
                                                                 null,
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("update authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PUT,
                                                                 User.builder()
                                                                     .userName("test")
                                                                     .nickName("test")
                                                                     .build()
                                                                     .toString(),
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("patch authorized", () -> {
      PatchRequestOperation operation = PatchRequestOperation.builder().path("nickname").op(PatchOp.REMOVE).build();
      List<PatchRequestOperation> operations = Arrays.asList(operation);
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PATCH,
                                                                 patchOpRequest.toString(),
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).getResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("delete authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.DELETE,
                                                                 null,
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any(), Mockito.eq(clientAuthorization));
    }));
    /* ************************************************************************************************************/
    return dynamicTests;
  }

  /**
   * this test will verify that a client can access protected endpoints if the proper roles are granted and the
   * necessary roles are on the "roles" attribute in the resource type
   */
  @TestFactory
  public List<DynamicTest> testAuthorizedWithAuthorizationOnResourceTypeLevel()
  {
    final String url = BASE_URI + EndpointPaths.USERS;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    EndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType resourceType = resourceEndpoint.registerEndpoint(endpointDefinition);
    final String requiredRole = "admin";
    resourceType.getFeatures().getAuthorization().setRoles(requiredRole);
    ResourceTypeAuthorization resourceTypeAuthorization = resourceType.getFeatures().getAuthorization();
    Assertions.assertEquals(requiredRole, resourceTypeAuthorization.getRolesCreate().iterator().next());
    Assertions.assertEquals(requiredRole, resourceTypeAuthorization.getRolesGet().iterator().next());
    Assertions.assertEquals(requiredRole, resourceTypeAuthorization.getRolesUpdate().iterator().next());
    Assertions.assertEquals(requiredRole, resourceTypeAuthorization.getRolesDelete().iterator().next());
    final ClientAuthorization clientAuthorization = new ClientAuthorization("goldfish", requiredRole);
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("test").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("create authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.POST,
                                                                 User.builder().userName("test").build().toString(),
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("get authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).getResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list-post authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/.search",
                                                                 HttpMethod.POST,
                                                                 null,
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("update authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PUT,
                                                                 User.builder()
                                                                     .userName("test")
                                                                     .nickName("test")
                                                                     .build()
                                                                     .toString(),
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("patch authorized", () -> {
      PatchRequestOperation operation = PatchRequestOperation.builder().path("nickname").op(PatchOp.REMOVE).build();
      List<PatchRequestOperation> operations = Arrays.asList(operation);
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PATCH,
                                                                 patchOpRequest.toString(),
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).getResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(clientAuthorization));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("delete authorized", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.DELETE,
                                                                 null,
                                                                 httpHeaders,
                                                                 clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any(), Mockito.eq(clientAuthorization));
    }));
    /* ************************************************************************************************************/
    return dynamicTests;
  }

  /**
   * tests if a resource type endpoint protected with authorization roles is inaccessible if the necessary roles
   * are not present
   */
  private DynamicTest getUnauthorizedTest(String testName,
                                          String url,
                                          ResourceType resourceType,
                                          HttpMethod method,
                                          ClientAuthorization clientAuthorization,
                                          EndpointType endpointType)
  {
    return DynamicTest.dynamicTest(testName, () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, method, null, httpHeaders, clientAuthorization);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.FORBIDDEN, errorResponse.getHttpStatus());
      Assertions.assertEquals("you are not authorized to access the '" + endpointType + "' endpoint on resource type '"
                              + resourceType.getName() + "'",
                              errorResponse.getDetail().get());
    });
  }

  /**
   * a simple class that is used to to represent and test client authorization
   */
  public static class ClientAuthorization implements Authorization
  {

    /**
     * the client identification
     */
    private String clientId;

    /**
     * the roles the client have been granted
     */
    private Set<String> roles;

    public ClientAuthorization(String clientId, String... roles)
    {
      this.clientId = clientId;
      this.roles = new HashSet<>();
      if (roles != null)
      {
        this.roles.addAll(Arrays.asList(roles));
      }
    }

    @Override
    public String getClientId()
    {
      return clientId;
    }

    @Override
    public Set<String> getClientRoles()
    {
      return roles;
    }

  }
}
