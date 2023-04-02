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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
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
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.EncodingUtils;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.features.EndpointType;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.SingletonUserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
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
    List<AuthenticationScheme> authSchemes = Arrays.asList(AuthenticationScheme.builder()
                                                                               .name("test")
                                                                               .description("test")
                                                                               .type("test")
                                                                               .specUri("test")
                                                                               .documentationUri("test")
                                                                               .display("test")
                                                                               .ref("test")
                                                                               .primary(true)
                                                                               .build());
    serviceProvider = ServiceProvider.builder().authenticationSchemes(authSchemes).build();
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               user.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(1, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
    CreateResponse createResponse = (CreateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.CREATED, createResponse.getHttpStatus());
    User createdUser = JsonHelper.copyResourceToObject(createResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), createdUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.anyList(), Mockito.anyList(), Mockito.any());
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
                                                               resourceTypeConsumer,
                                                               null);
    Assertions.assertTrue(wasCalled.get());
    Assertions.assertEquals(1, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
    CreateResponse createResponse = (CreateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.CREATED, createResponse.getHttpStatus());
    User createdUser = JsonHelper.copyResourceToObject(createResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), createdUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.anyList(), Mockito.anyList(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any());
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
    Mockito.doReturn(user).when(userHandler).createResource(Mockito.any(), Mockito.notNull());

    MatcherAssert.assertThat(new ArrayList<>(user.getSchemas()),
                             Matchers.not(Matchers.hasItem(SchemaUris.ENTERPRISE_USER_URI)));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               userRequest,
                                                               httpHeaders,
                                                               new Context(null));
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               user.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, getResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(getResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1))
           .getResource(Mockito.any(),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any());
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
                                                               resourceTypeConsumer,
                                                               null);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, getResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(getResponse, User.class);
    Assertions.assertEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1))
           .getResource(Mockito.any(),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any());
    Assertions.assertEquals(BASE_URI + EndpointPaths.USERS + "/" + returnedUser.getId().get(),
                            returnedUser.getMeta().flatMap(Meta::getLocation).get());
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
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(updateResponse, User.class);
    Assertions.assertNotEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Assertions.assertEquals(changedUser.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any());
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
                                                               resourceTypeConsumer,
                                                               null);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    User returnedUser = JsonHelper.copyResourceToObject(updateResponse, User.class);
    Assertions.assertNotEquals(user.getUserName().get(), returnedUser.getUserName().get());
    Assertions.assertEquals(changedUser.getUserName().get(), returnedUser.getUserName().get());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any());
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.DELETE,
                                                               user.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
    DeleteResponse deleteResponse = (DeleteResponse)scimResponse;
    Assertions.assertTrue(deleteResponse.isEmpty());
    Assertions.assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any(), Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any());
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
                                                               resourceTypeConsumer,
                                                               null);
    Assertions.assertTrue(wasCalled.get());

    Assertions.assertEquals(0, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
    DeleteResponse deleteResponse = (DeleteResponse)scimResponse;
    Assertions.assertTrue(deleteResponse.isEmpty());
    Assertions.assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any(), Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any());
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
    final String url = BASE_URI + EndpointPaths.USERS
                       + String.format("?startIndex=1&count=%d&filter=%s",
                                       maxUsers,
                                       "userName sw \"" + searchValue + "\"");
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(maxUsers),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.notNull());
  }

  /**
   * this test will assure that a {@link BadRequestException} with an appropriate error message if the
   * startIndex value is not a number
   */
  @Test
  public void testQueryResourcesWithInvalidStartIndex()
  {
    final String url = BASE_URI + EndpointPaths.USERS + "?startIndex=NaN";
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, scimResponse.getHttpStatus());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals("Got invalid startIndex value 'NaN'. StartIndex must be a number",
                            errorResponse.getDetail().orElseThrow(IllegalStateException::new));
  }

  /**
   * this test will assure that a {@link BadRequestException} with an appropriate error message if the cont
   * value is not a number
   */
  @Test
  public void testQueryResourcesWithInvalidCount()
  {
    final String url = BASE_URI + EndpointPaths.USERS + "?count=NaN";
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, scimResponse.getHttpStatus());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals("Got invalid count value 'NaN'. Count must be a number",
                            errorResponse.getDetail().orElseThrow(IllegalStateException::new));
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
                                                               resourceTypeConsumer,
                                                               null);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(maxUsers),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.notNull());
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
    final String url = BASE_URI + EndpointPaths.USERS + EndpointPaths.SEARCH;
    SearchRequest searchRequest = SearchRequest.builder()
                                               .startIndex(1L)
                                               .count(maxUsers)
                                               .filter("userName sw \"" + searchValue + "\"")
                                               .build();
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               searchRequest.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(maxUsers),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.notNull());
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
                                                               resourceTypeConsumer,
                                                               null);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(counter, listResponse.getListedResources().size());
    Assertions.assertEquals(counter, listResponse.getTotalResults());
    Assertions.assertEquals(HttpStatus.OK, listResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .getResource(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(maxUsers),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.notNull());
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
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(maxOperations, userHandler.getInMemoryMap().size());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(maxOperations)).createResource(Mockito.any(), Mockito.notNull());

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
    scimResponse = resourceEndpoint.handleRequest(url,
                                                  HttpMethod.POST,
                                                  bulkRequest.toString(),
                                                  httpHeaders,
                                                  new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    MatcherAssert.assertThat(errorResponse.getDetail().get(),
                             Matchers.containsString("Document does not have a 'schemas'-attribute"));
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
                                                               httpHeaders,
                                                               new Context(null));
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
        throw new IllegalStateException("not yet supported");
    }

    operations.forEach(operation -> operation.setBulkId(null));
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(operations).build();
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders,
                                                               new Context(null));
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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(createOperations.size(), bulkResponse.getBulkResponseOperations().size());

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
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders,
                                                               new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    Assertions.assertEquals("too many operations maximum number of operations is '" + maxOperations + "' but got '"
                            + createOperations.size() + "'",
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
                                                               httpHeaders,
                                                               new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    Assertions.assertEquals("Required 'WRITE_ONLY' attribute "
                            + "'urn:ietf:params:scim:api:messages:2.0:BulkRequest:Operations' is missing",
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
           .createResource(Mockito.any(), Mockito.any());
    List<BulkRequestOperation> createOperations = getCreateUserBulkOperations(maxOperations);
    BulkRequest bulkRequest = BulkRequest.builder().bulkRequestOperation(createOperations).build();
    final String url = BASE_URI + EndpointPaths.BULK;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               bulkRequest.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(BulkResponse.class));
    BulkResponse bulkResponse = (BulkResponse)scimResponse;
    Assertions.assertEquals(maxOperations, bulkResponse.getBulkResponseOperations().size());
    Assertions.assertFalse(bulkResponse.getBulkResponseOperations().get(0).getLocation().isPresent(),
                           bulkResponse.toPrettyString());
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               httpMethod,
                                                               bulkRequest.toString(),
                                                               httpHeaders,
                                                               new Context(null));
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

    User copiedUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    copiedUser.setName(name);

    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    updateResponse.remove(AttributeNames.RFC7643.META);
    copiedUser.remove(AttributeNames.RFC7643.META);
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    Assertions.assertEquals(copiedUser, updateResponse);

    GetResponse getResponse = (GetResponse)resourceEndpoint.getResource(EndpointPaths.USERS, id, null, baseUrl);
    getResponse.remove(AttributeNames.RFC7643.META);
    Assertions.assertEquals(updateResponse, getResponse);
    Assertions.assertEquals(copiedUser, getResponse);
  }

  /**
   * This test adds the name-attribute to a user and lets the name.familyName attribute not be returned by the
   * developer implementation. This test will assure that the name.familyName attribute in this case will not be
   * returned to the client if this happens. <br>
   * <br>
   * This test is reproducing issue #188
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/188
   */
  @Test
  public void testPatchResourceWithFamilyNameNotReturnedByImplementation()
  {
    serviceProvider.getPatchConfig().setSupported(true);

    // at first we create a user that does not have a name-attribute
    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").nickName("captain").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    // now create a patch-operation that will add a name-attribute to the just created user
    final String path = "name";
    Name name = Name.builder().givenName("goldfish").familyName("captain").build();
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .valueNode(name)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    // make the user handler return the the copied user that has the familyName removed upon response
    User userToReturn = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    Name copiedName = JsonHelper.copyResourceToObject(name.deepCopy(), Name.class);
    copiedName.setFamilyName(null);
    userToReturn.setName(copiedName);
    Mockito.doReturn(userToReturn).when(userHandler).updateResource(Mockito.any(), Mockito.any());

    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));

    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    updateResponse.remove(AttributeNames.RFC7643.META);
    userToReturn.remove(AttributeNames.RFC7643.META);
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    Assertions.assertEquals(userToReturn, updateResponse);
  }

  /**
   * verifies that a patch operation request with a none string type value is processed successfully:
   *
   * <pre>
   *  {
   *   "Operations": [
   *     {
   *       "op": "replace",
   *       "path": "active",
   *       "value": [true]
   *     }
   *   ],
   *   "schemas": [
   *     "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *   ]
   *  }
   * </pre>
   */
  @Test
  public void testPatchResourceWithBooleanArrayValueInPatchOpValue()
  {
    serviceProvider.getPatchConfig().setSupported(true);

    final String path = "active";
    BooleanNode value = BooleanNode.TRUE;
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .valueNode(value)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    final String patchOpString = patchOpRequest.toString();

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").nickName("captain").active(false).meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    User copiedUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);

    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpString,
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    Assertions.assertTrue(JsonHelper.copyResourceToObject(scimResponse, User.class).isActive().get());
    Assertions.assertTrue(userHandler.getInMemoryMap().get(id).isActive().get());
    Assertions.assertFalse(copiedUser.isActive().get());
  }

  /**
   * verifies that a patch operation request with a none string type value is processed successfully:
   *
   * <pre>
   *  {
   *   "Operations": [
   *     {
   *       "op": "replace",
   *       "path": "active",
   *       "value": true
   *     }
   *   ],
   *   "schemas": [
   *     "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *   ]
   *  }
   * </pre>
   */
  @Test
  public void testPatchResourceWithBooleanValueInPatchOpValue()
  {
    serviceProvider.getPatchConfig().setSupported(true);

    final String path = "active";
    BooleanNode value = BooleanNode.TRUE;
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .valueNode(value)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    final String patchOpString = patchOpRequest.toString().replace("[true]", "true");

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").nickName("captain").active(false).meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    User copiedUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);

    final String url = BASE_URI + EndpointPaths.USERS + "/" + id;

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpString,
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    Assertions.assertTrue(JsonHelper.copyResourceToObject(scimResponse, User.class).isActive().get());
    Assertions.assertTrue(userHandler.getInMemoryMap().get(id).isActive().get());
    Assertions.assertFalse(copiedUser.isActive().get());
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

    User copiedUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    copiedUser.setName(name);

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
                                                               resourceTypeConsumer,
                                                               null);
    Assertions.assertTrue(wasCalled.get());

    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    updateResponse.remove(AttributeNames.RFC7643.META);
    copiedUser.remove(AttributeNames.RFC7643.META);
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    Assertions.assertEquals(copiedUser, updateResponse);

    GetResponse getResponse = (GetResponse)resourceEndpoint.getResource(EndpointPaths.USERS, id, null, baseUrl);
    getResponse.remove(AttributeNames.RFC7643.META);
    Assertions.assertEquals(updateResponse, getResponse);
    Assertions.assertEquals(copiedUser, getResponse);
  }

  /**
   * Verifies that a patch operation can be executed on a singleton resource type
   */
  @Test
  public void testPatchResourceOnSingletonEndpoint()
  {
    serviceProvider.getPatchConfig().setSupported(true);

    SingletonUserHandlerImpl singletonUserHandler = new SingletonUserHandlerImpl();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(singletonUserHandler));
    userResourceType.getFeatures().setSingletonEndpoint(true);

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
    singletonUserHandler.setUser(user);

    final String url = BASE_URI + EndpointPaths.USERS;

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
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
                                                               httpHeaders,
                                                               new Context(null));
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
                                                                                .value("captain")
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
                                                               httpHeaders,
                                                               new Context(null));
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
   * will check that no exception is thrown if we try to access only a single attribute of the service provider.
   * <ul>
   * <li>we ask explicitly for the patch attribute of the service provider</li>
   * <li>we expect only the minimal set of [schemas, patch] to be returned in the response</li>
   * </ul>
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/393
   */
  @Test
  public void testAskForASingleAttributeWhileOthersAreStillRequired()
  {
    final String url = BASE_URI + EndpointPaths.SERVICE_PROVIDER_CONFIG + "/?attributes=patch";
    final List<String> minimalAttributeSet = Arrays.asList("schemas", "patch");

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(HttpStatus.OK, scimResponse.getHttpStatus(), scimResponse.toPrettyString());
    Assertions.assertEquals(minimalAttributeSet.size(), scimResponse.size(), scimResponse.toPrettyString());
    for ( String attributeName : minimalAttributeSet )
    {
      Assertions.assertNotNull(scimResponse.get(attributeName), scimResponse.toPrettyString());
    }
  }

  /**
   * will check that no exception is thrown if we try to access only a single attribute of the service provider.
   * <ul>
   * <li>we remove the patch config from the service provider</li>
   * <li>we ask explicitly for the patch attribute of the service provider</li>
   * <li>we expect an internal server error because the response does not contain the required attribute</li>
   * </ul>
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/393
   */
  @Test
  public void testAskForASpecificAttributeThatIsMissingInTheResponse()
  {
    // 1. set patch config to null
    resourceEndpoint.getServiceProvider().remove(AttributeNames.RFC7643.PATCH);

    // 2. ask explicitly for the patch attribute
    final String url = BASE_URI + EndpointPaths.SERVICE_PROVIDER_CONFIG + "/?attributes=patch";
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));

    // 3. we expect an internal server error since the required attribute is not provided by the server
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                            scimResponse.getHttpStatus(),
                            scimResponse.toPrettyString());
  }

  /**
   * will check that no exception is thrown if we try to access only a single attribute of the service provider.
   * <ul>
   * <li>we ask explicitly to exclude the patch attribute of the service provider</li>
   * <li>we expect the complete set of attributes except the patch attribute</li>
   * </ul>
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/393
   */
  @Test
  public void testExcludeRequiredAttribute()
  {
    // 1. ask explicitly to exclude the patch attribute
    final String url = BASE_URI + EndpointPaths.SERVICE_PROVIDER_CONFIG + "/?excludedAttributes=patch";
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));

    // 2. expect all attributes to be returned except for the patch attribute
    ResourceType resourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.SERVICE_PROVIDER_CONFIG)
                                                .orElse(null);
    final List<String> expectedAttributeSet = resourceType.getMainSchema()
                                                          .getAttributes()
                                                          .stream()
                                                          .map(SchemaAttribute::getScimNodeName)
                                                          .collect(Collectors.toList());
    expectedAttributeSet.add(AttributeNames.RFC7643.SCHEMAS);
    expectedAttributeSet.add(AttributeNames.RFC7643.META);
    expectedAttributeSet.remove(AttributeNames.RFC7643.PATCH);
    // documentation uri is not a required attribute
    expectedAttributeSet.remove(AttributeNames.RFC7643.DOCUMENTATION_URI);

    Assertions.assertEquals(expectedAttributeSet.size(), scimResponse.size(), scimResponse.toPrettyString());
    for ( String attributeName : expectedAttributeSet )
    {
      Assertions.assertNotNull(scimResponse.get(attributeName),
                               String.format("attribute '%s' should have been present in document: \n%s",
                                             attributeName,
                                             scimResponse.toPrettyString()));
    }
  }

  /**
   * will check that no exception is thrown if the client tries to exclude an attribute that is required but not
   * even set by the service provider. In cases of errors on the service provider side the client may use this
   * as a workaround
   * <ul>
   * <li>remove the patch attribute from the service provider</li>
   * <li>we ask explicitly to exclude the patch attribute of the service provider</li>
   * <li>we expect the complete set of attributes except the patch attribute</li>
   * </ul>
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/393
   */
  @Test
  public void testExcludeRequiredAttributeThatIsIllegallySetToNull()
  {
    // 1. set patch config to null
    resourceEndpoint.getServiceProvider().remove(AttributeNames.RFC7643.PATCH);

    // 2. ask explicitly to exclude the patch attribute
    final String url = BASE_URI + EndpointPaths.SERVICE_PROVIDER_CONFIG + "/?excludedAttributes=patch";
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));

    // 3. expect all attributes to be returned except for the patch attribute
    ResourceType resourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.SERVICE_PROVIDER_CONFIG)
                                                .orElse(null);
    final List<String> expectedAttributeSet = resourceType.getMainSchema()
                                                          .getAttributes()
                                                          .stream()
                                                          .map(SchemaAttribute::getScimNodeName)
                                                          .collect(Collectors.toList());
    expectedAttributeSet.add(AttributeNames.RFC7643.SCHEMAS);
    expectedAttributeSet.add(AttributeNames.RFC7643.META);
    expectedAttributeSet.remove(AttributeNames.RFC7643.PATCH);
    // documentation uri is not a required attribute
    expectedAttributeSet.remove(AttributeNames.RFC7643.DOCUMENTATION_URI);

    Assertions.assertEquals(expectedAttributeSet.size(), scimResponse.size(), scimResponse.toPrettyString());
    for ( String attributeName : expectedAttributeSet )
    {
      Assertions.assertNotNull(scimResponse.get(attributeName),
                               String.format("attribute '%s' should have been present in document: \n%s",
                                             attributeName,
                                             scimResponse.toPrettyString()));
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
                                                               httpHeaders,
                                                               new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
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
                                                               httpHeaders,
                                                               new Context(null));
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = JsonHelper.copyResourceToObject(scimResponse, ListResponse.class);
    Assertions.assertEquals(3, listResponse.getTotalResults());
    Assertions.assertEquals(3, listResponse.getListedResources().size());
    for ( ScimObjectNode listedResource : listResponse.getListedResources() )
    {
      String name = listedResource.get(AttributeNames.RFC7643.NAME).textValue();
      Meta meta = JsonHelper.copyResourceToObject(listedResource.get(AttributeNames.RFC7643.META), Meta.class);
      Assertions.assertTrue(meta.getLocation().isPresent());
      Assertions.assertEquals(String.format("%s%s/%s", BASE_URI, EndpointPaths.RESOURCE_TYPES, name),
                              meta.getLocation().get());
    }
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
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.GET,
                                                               null,
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse<ScimObjectNode> listResponse = JsonHelper.copyResourceToObject(scimResponse, ListResponse.class);
    Assertions.assertEquals(4, listResponse.getTotalResults());
    Assertions.assertEquals(4, listResponse.getListedResources().size());
    for ( ScimObjectNode listedResource : listResponse.getListedResources() )
    {
      String id = listedResource.get(AttributeNames.RFC7643.ID).textValue();
      Meta meta = JsonHelper.copyResourceToObject(listedResource.get(AttributeNames.RFC7643.META), Meta.class);
      Assertions.assertTrue(meta.getLocation().isPresent());
      Assertions.assertEquals(String.format("%s%s/%s", BASE_URI, EndpointPaths.SCHEMAS, EncodingUtils.urlEncode(id)),
                              meta.getLocation().get());
    }
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.POST,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.PUT,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.PATCH,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.DELETE,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.POST,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check list is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check update is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.PUT,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check patch is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.PATCH,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check delete is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.DELETE,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.POST,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check list is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check update is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.PUT,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check patch is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.PATCH,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check delete is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.DELETE,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.POST,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check list is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check update is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.PUT,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check patch is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.PATCH,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      ScimException ex = errorResponse.getScimException();
      Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.getStatus());
      Assertions.assertEquals("the resource type '" + resourceType.getName() + "' is disabled", ex.getDetail());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("check delete is disabled", () -> {
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/123456",
                                                                 HttpMethod.DELETE,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(null));
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
                                                          .name("Bearer")
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
    final ClientAuthorization clientAuthorization = new ClientAuthorization("goldfish", "create", "list", "get",
                                                                            "update", "delete");
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("test").nickName("test").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("create authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.POST,
                                                                 User.builder().userName("test").build().toString(),
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("get authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .getResource(Mockito.any(),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(context));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders, context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list-post authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/.search",
                                                                 HttpMethod.POST,
                                                                 null,
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("update authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PUT,
                                                                 User.builder()
                                                                     .userName("test")
                                                                     .nickName("test")
                                                                     .build()
                                                                     .toString(),
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("patch authorized", () -> {
      Context context = new Context(clientAuthorization);
      PatchRequestOperation operation = PatchRequestOperation.builder().path("nickname").op(PatchOp.REMOVE).build();
      List<PatchRequestOperation> operations = Arrays.asList(operation);
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PATCH,
                                                                 patchOpRequest.toString(),
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .getResource(Mockito.any(),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(context));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("delete authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.DELETE,
                                                                 null,
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
    }));
    /* ************************************************************************************************************/
    return dynamicTests;
  }

  /**
   * this test will verify that a client cannot access a protected endpoint if the endpoint requires several
   * roles in order to access the endpoint<br>
   * <br>
   * <ol>
   * <li>the user has only the role user but access to all endpoints require the roles "admin" and "user"</li>
   * <li>the authorization access must fail on all endpoints</li>
   * </ol>
   */
  @TestFactory
  public List<DynamicTest> testUnauthorizedBecauseMultipleRolesAreNeeded()
  {
    final String url = BASE_URI + EndpointPaths.USERS;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    EndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    endpointDefinition.setResourceType(JsonHelper.loadJsonDocument(USER_AUTHORIZED_RESOURCE_TYPE));
    endpointDefinition.setResourceSchemaExtensions(Collections.emptyList());
    ResourceType resourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    // require the roles "admin" and "user" on all endpoints
    resourceType.getFeatures().getAuthorization().setUseOrOnRoles(false);
    resourceType.getFeatures().getAuthorization().setRolesCreate("admin", "user");
    resourceType.getFeatures().getAuthorization().setRolesGet("admin", "user");
    resourceType.getFeatures().getAuthorization().setRolesList("admin", "user");
    resourceType.getFeatures().getAuthorization().setRolesUpdate("admin", "user");
    resourceType.getFeatures().getAuthorization().setRolesDelete("admin", "user");

    final ClientAuthorization clientAuthorization = new ClientAuthorization("goldfish", "user");
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("test").nickName("test").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

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
   * this test will verify that a client cannot access a protected endpoint if the endpoint requires several
   * roles in order to access the endpoint<br>
   * <br>
   * <ol>
   * <li>the user has only the role user but access to all endpoints require the roles "admin" and "user"</li>
   * <li>the authorization access must fail on all endpoints</li>
   * </ol>
   */
  @TestFactory
  public List<DynamicTest> testAuthorizedBecauseRolesAreComparedWithOrInsteadOfAndNeeded()
  {
    final String url = BASE_URI + EndpointPaths.USERS;
    serviceProvider = getServiceProvider();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    EndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    endpointDefinition.setResourceType(JsonHelper.loadJsonDocument(USER_AUTHORIZED_RESOURCE_TYPE));
    endpointDefinition.setResourceSchemaExtensions(Collections.emptyList());
    ResourceType resourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    // require the roles "admin" and "user" on all endpoints
    resourceType.getFeatures().getAuthorization().setUseOrOnRoles(true);
    resourceType.getFeatures().getAuthorization().setRolesCreate("admin", "user");
    resourceType.getFeatures().getAuthorization().setRolesGet("admin", "user");
    resourceType.getFeatures().getAuthorization().setRolesList("admin", "user");
    resourceType.getFeatures().getAuthorization().setRolesUpdate("admin", "user");
    resourceType.getFeatures().getAuthorization().setRolesDelete("admin", "user");

    final ClientAuthorization clientAuthorization = new ClientAuthorization("goldfish", "user");
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("test").nickName("test").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("create authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.POST,
                                                                 User.builder().userName("test").build().toString(),
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("get authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .getResource(Mockito.any(),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(context));
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders, context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list-post authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/.search",
                                                                 HttpMethod.POST,
                                                                 null,
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("update authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PUT,
                                                                 User.builder()
                                                                     .userName("test")
                                                                     .nickName("test")
                                                                     .build()
                                                                     .toString(),
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("patch authorized", () -> {
      Context context = new Context(clientAuthorization);
      PatchRequestOperation operation = PatchRequestOperation.builder().path("nickname").op(PatchOp.REMOVE).build();
      List<PatchRequestOperation> operations = Arrays.asList(operation);
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PATCH,
                                                                 patchOpRequest.toString(),
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .getResource(Mockito.any(),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(context));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("delete authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.DELETE,
                                                                 null,
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
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
    Assertions.assertTrue(resourceTypeAuthorization.getRolesCreate().isEmpty());
    Assertions.assertTrue(resourceTypeAuthorization.getRolesGet().isEmpty());
    Assertions.assertTrue(resourceTypeAuthorization.getRolesUpdate().isEmpty());
    Assertions.assertTrue(resourceTypeAuthorization.getRolesDelete().isEmpty());
    final ClientAuthorization clientAuthorization = new ClientAuthorization("goldfish", requiredRole);
    final String id = UUID.randomUUID().toString();
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    User user = User.builder().id(id).userName("test").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("create authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 HttpMethod.POST,
                                                                 User.builder().userName("test").build().toString(),
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("get authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.GET,
                                                                 null,
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .getResource(Mockito.any(),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders, context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("list-post authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/.search",
                                                                 HttpMethod.POST,
                                                                 null,
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .listResources(Mockito.anyLong(),
                            Mockito.anyInt(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.any(),
                            Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("update authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PUT,
                                                                 User.builder()
                                                                     .userName("test")
                                                                     .nickName("test")
                                                                     .build()
                                                                     .toString(),
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("patch authorized", () -> {
      PatchRequestOperation operation = PatchRequestOperation.builder().path("nickname").op(PatchOp.REMOVE).build();
      List<PatchRequestOperation> operations = Arrays.asList(operation);
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.PATCH,
                                                                 patchOpRequest.toString(),
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      Mockito.verify(userHandler, Mockito.times(1))
             .getResource(Mockito.any(),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(context));
      Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
      Mockito.clearInvocations(userHandler);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("delete authorized", () -> {
      Context context = new Context(clientAuthorization);
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url + "/" + id,
                                                                 HttpMethod.DELETE,
                                                                 null,
                                                                 httpHeaders,
                                                                 context);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
      Mockito.verify(userHandler, Mockito.times(1)).deleteResource(Mockito.any(), Mockito.eq(context));
      Assertions.assertEquals(clientAuthorization, context.getAuthorization());
    }));
    /* ************************************************************************************************************/
    return dynamicTests;
  }

  /**
   * verifies that a bad request is returned if the the sent body is not a json object
   */
  @Test
  public void testBadRequestOnNoneObjectRequestBody()
  {
    final String userName = "chuck_norris";
    final User user = User.builder().id(UUID.randomUUID().toString()).userName(userName).build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.add(user.toString());

    final String url = BASE_URI + EndpointPaths.USERS;

    Mockito.doReturn(user).when(userHandler).createResource(Mockito.any(), Mockito.isNull());

    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               arrayNode.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
    String errorMessage = String.format("The received resource document is not an object '%s'", arrayNode);
    Assertions.assertEquals(errorMessage, errorResponse.getDetail().get());
    MatcherAssert.assertThat(errorResponse.getFieldErrors(), Matchers.anEmptyMap());
    MatcherAssert.assertThat(errorResponse.getErrorMessages(), Matchers.hasItem(errorMessage));
  }

  /**
   * This test will verify that an attribute with a returned value of request is returned if the attribute was
   * on the request. Meaning the attribute was set during creation or modified on a PUT or PATCH request.<br>
   * from RFC7643 chapter 7
   *
   * <pre>
   *   request  The attribute is returned in response to any PUT,
   *             POST, or PATCH operations if the attribute was specified by
   *             the client (for example, the attribute was modified).  The
   *             attribute is returned in a SCIM query operation only if
   *             specified in the "attributes" parameter.
   * </pre>
   */
  @Test
  public void testRequestAttributeIsReturnedAfterPutPostOrPatchRequest()
  {
    final String attributeName = AttributeNames.RFC7643.USER_NAME;
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode enterpriseUserSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(userSchemaNode,
                                       attributeName,
                                       null,
                                       null,
                                       Returned.REQUEST,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceEndpoint.registerEndpoint(new EndpointDefinition(userResourceType, userSchemaNode,
                                                             Collections.singletonList(enterpriseUserSchemaNode),
                                                             new UserHandlerImpl(false)));

    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE);

    final String url = BASE_URI + EndpointPaths.USERS;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               user.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(HttpStatus.CREATED, scimResponse.getHttpStatus());

    Assertions.assertNotNull(scimResponse.get(attributeName));
    Assertions.assertNotNull(scimResponse.get(AttributeNames.RFC7643.ID));
  }

  /**
   * This test will verify that an attribute is returned if the attributes returned value is "request" and its
   * value was present in the request document<br>
   * from RFC7643 chapter 7
   *
   * <pre>
   *   request  The attribute is returned in response to any PUT,
   *             POST, or PATCH operations if the attribute was specified by
   *             the client (for example, the attribute was modified).  The
   *             attribute is returned in a SCIM query operation only if
   *             specified in the "attributes" parameter.
   * </pre>
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.PHONE_NUMBERS, AttributeNames.RFC7643.DISPLAY_NAME,
                          AttributeNames.RFC7643.EXTERNAL_ID, AttributeNames.RFC7643.NAME,
                          AttributeNames.RFC7643.EMAILS,
                          AttributeNames.RFC7643.EMAILS + "." + AttributeNames.RFC7643.VALUE,
                          AttributeNames.RFC7643.NAME + "." + AttributeNames.RFC7643.GIVEN_NAME,
                          AttributeNames.RFC7643.NAME + "." + AttributeNames.RFC7643.MIDDLE_NAME})
  public void testAttributeIsReturnedIfFullUriNameIsUsedOnAttributesParameter(String attributeName)
  {
    final String fullName = SchemaUris.USER_URI + ":" + attributeName;

    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode enterpriseUserSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(userSchemaNode,
                                       attributeName,
                                       null,
                                       null,
                                       Returned.REQUEST,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    ResourceType resourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(userResourceType,
                                                                                         userSchemaNode,
                                                                                         Collections.singletonList(enterpriseUserSchemaNode),
                                                                                         new UserHandlerImpl(false)));
    SchemaAttribute schemaAttribute = resourceType.getSchemaAttribute(attributeName).get();

    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);

    final String url = BASE_URI + EndpointPaths.USERS + "?attributes=" + fullName;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               user.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(HttpStatus.CREATED, scimResponse.getHttpStatus());

    String[] attributeNameParts = attributeName.split("\\.");
    if (attributeNameParts.length == 1)
    {
      Assertions.assertNotNull(scimResponse.get(attributeName));
    }
    else
    {
      JsonNode complexAttribute = scimResponse.get(attributeNameParts[0]);
      Assertions.assertNotNull(complexAttribute);
      SchemaAttribute parentAttribute = schemaAttribute.getParent();
      if (parentAttribute.isMultiValued())
      {
        ArrayNode multiValuedComplex = (ArrayNode)complexAttribute;
        for ( JsonNode valuedComplex : multiValuedComplex )
        {
          Assertions.assertNotNull(valuedComplex.get(attributeNameParts[1]));
        }
      }
      else
      {
        Assertions.assertNotNull(complexAttribute.get(attributeNameParts[1]));
      }
    }
    Assertions.assertNotNull(scimResponse.get(AttributeNames.RFC7643.ID));
  }

  /**
   * This test will verify that an attribute is also returned parameters names are case insensitive<br>
   * from RFC7643 chapter 7
   *
   * <pre>
   *   request  The attribute is returned in response to any PUT,
   *             POST, or PATCH operations if the attribute was specified by
   *             the client (for example, the attribute was modified).  The
   *             attribute is returned in a SCIM query operation only if
   *             specified in the "attributes" parameter.
   * </pre>
   */
  @ParameterizedTest
  @CsvSource({AttributeNames.RFC7643.PHONE_NUMBERS + ",phonenumbers",
              AttributeNames.RFC7643.PHONE_NUMBERS + ".value,phonenumbers.value",
              AttributeNames.RFC7643.DISPLAY_NAME + "," + "displayname",
              AttributeNames.RFC7643.EXTERNAL_ID + ",externalid",
              AttributeNames.RFC7643.NAME + "." + AttributeNames.RFC7643.GIVEN_NAME + ",name.givenname",
              AttributeNames.RFC7643.NAME + "." + AttributeNames.RFC7643.MIDDLE_NAME + ",name.middlename"})
  public void testAttributeIsReturnedIfFullUriNameIsUsedOnAttributesParameter(String attributeName,
                                                                              String caseInsensitiveName)
  {
    final String fullName = SchemaUris.USER_URI + ":" + caseInsensitiveName;

    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode enterpriseUserSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(userSchemaNode,
                                       attributeName,
                                       null,
                                       null,
                                       Returned.REQUEST,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    ResourceType resourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(userResourceType,
                                                                                         userSchemaNode,
                                                                                         Collections.singletonList(enterpriseUserSchemaNode),
                                                                                         new UserHandlerImpl(false)));

    SchemaAttribute schemaAttribute = resourceType.getSchemaAttribute(attributeName).get();

    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);

    final String url = BASE_URI + EndpointPaths.USERS + "?attributes=" + fullName;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               user.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(HttpStatus.CREATED, scimResponse.getHttpStatus());

    String[] attributeNameParts = attributeName.split("\\.");
    if (attributeNameParts.length == 1)
    {
      Assertions.assertNotNull(scimResponse.get(attributeName));
    }
    else
    {
      JsonNode complexAttribute = scimResponse.get(attributeNameParts[0]);
      Assertions.assertNotNull(complexAttribute);
      SchemaAttribute parentAttribute = schemaAttribute.getParent();
      if (parentAttribute.isMultiValued())
      {
        ArrayNode multiValuedComplex = (ArrayNode)complexAttribute;
        for ( JsonNode valuedComplex : multiValuedComplex )
        {
          Assertions.assertNotNull(valuedComplex.get(attributeNameParts[1]));
        }
      }
      else
      {
        Assertions.assertNotNull(complexAttribute.get(attributeNameParts[1]));
      }
    }
    Assertions.assertNotNull(scimResponse.get(AttributeNames.RFC7643.ID));
  }

  /**
   * This test will verify that an attribute is also returned if the short name of the attribute was used in the
   * attributes parameter<br>
   * from RFC7643 chapter 7
   *
   * <pre>
   *   request  The attribute is returned in response to any PUT,
   *             POST, or PATCH operations if the attribute was specified by
   *             the client (for example, the attribute was modified).  The
   *             attribute is returned in a SCIM query operation only if
   *             specified in the "attributes" parameter.
   * </pre>
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.PHONE_NUMBERS, AttributeNames.RFC7643.DISPLAY_NAME,
                          AttributeNames.RFC7643.EXTERNAL_ID, AttributeNames.RFC7643.NAME,
                          AttributeNames.RFC7643.EMAILS,
                          AttributeNames.RFC7643.EMAILS + "." + AttributeNames.RFC7643.VALUE,
                          AttributeNames.RFC7643.NAME + "." + AttributeNames.RFC7643.GIVEN_NAME,
                          AttributeNames.RFC7643.NAME + "." + AttributeNames.RFC7643.MIDDLE_NAME})
  public void testAttributeIsReturnedIfShortNameIsUsedOnAttributesParameter(String attributeName)
  {
    final String fullName = attributeName;

    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode enterpriseUserSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(userSchemaNode,
                                       attributeName,
                                       null,
                                       null,
                                       Returned.REQUEST,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    ResourceType resourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(userResourceType,
                                                                                         userSchemaNode,
                                                                                         Collections.singletonList(enterpriseUserSchemaNode),
                                                                                         new UserHandlerImpl(false)));
    SchemaAttribute schemaAttribute = resourceType.getSchemaAttribute(attributeName).get();

    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);

    final String url = BASE_URI + EndpointPaths.USERS + "?attributes=" + fullName;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               user.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(HttpStatus.CREATED, scimResponse.getHttpStatus());

    String[] attributeNameParts = attributeName.split("\\.");
    if (attributeNameParts.length == 1)
    {
      Assertions.assertNotNull(scimResponse.get(attributeName));
    }
    else
    {
      JsonNode complexAttribute = scimResponse.get(attributeNameParts[0]);
      Assertions.assertNotNull(complexAttribute);
      SchemaAttribute parentAttribute = schemaAttribute.getParent();
      if (parentAttribute.isMultiValued())
      {
        ArrayNode multiValuedComplex = (ArrayNode)complexAttribute;
        for ( JsonNode valuedComplex : multiValuedComplex )
        {
          Assertions.assertNotNull(valuedComplex.get(attributeNameParts[1]));
        }
      }
      else
      {
        Assertions.assertNotNull(complexAttribute.get(attributeNameParts[1]));
      }
    }
    Assertions.assertNotNull(scimResponse.get(AttributeNames.RFC7643.ID));
  }

  /**
   * This test will verify that an attribute is also returned if the URI of the resource was used in the
   * attributes parameter<br>
   * from RFC7643 chapter 7
   *
   * <pre>
   *   request  The attribute is returned in response to any PUT,
   *             POST, or PATCH operations if the attribute was specified by
   *             the client (for example, the attribute was modified).  The
   *             attribute is returned in a SCIM query operation only if
   *             specified in the "attributes" parameter.
   * </pre>
   */
  @Test
  public void testAttributeIsReturnedIfResourceUriIsUsedOnAttributesParameter()
  {
    final String attributeName = AttributeNames.RFC7643.NAME;

    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode enterpriseUserSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(userSchemaNode,
                                       attributeName,
                                       null,
                                       null,
                                       Returned.REQUEST,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceEndpoint.registerEndpoint(new EndpointDefinition(userResourceType, userSchemaNode,
                                                             Collections.singletonList(enterpriseUserSchemaNode),
                                                             new UserHandlerImpl(false)));

    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);

    final String url = BASE_URI + EndpointPaths.USERS + "?attributes=" + SchemaUris.USER_URI;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               user.toString(),
                                                               httpHeaders,
                                                               new Context(null));
    Assertions.assertEquals(HttpStatus.CREATED, scimResponse.getHttpStatus());

    Assertions.assertNotNull(scimResponse.get(attributeName));
    Assertions.assertNotNull(scimResponse.get(AttributeNames.RFC7643.ID));
  }

  /**
   * verifies that the document validation fails in case of request if the document evaluates to an empty
   * document. For example: this happens if the attributes within the document are not writable
   */
  @Test
  public void testValidateReceivedDocumentToEmptyDocumentForRequest()
  {
    JsonNode allTypesResourceTypeJson = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesValidationSchema = JsonHelper.loadJsonDocument(ALL_TYPES_VALIDATION_SCHEMA);
    JsonNode enterpriseUserValidationSchema = JsonHelper.loadJsonDocument(ENTERPRISE_USER_VALIDATION_SCHEMA);

    ResourceTypeFactory resourceTypeFactory = resourceEndpoint.getResourceTypeFactory();
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(false));
    Mockito.doReturn(null).when(userHandler).createResource(Mockito.any(), Mockito.any());
    resourceTypeFactory.registerResourceType(userHandler,
                                             allTypesResourceTypeJson,
                                             allTypesValidationSchema,
                                             enterpriseUserValidationSchema);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setMeta(Meta.builder().created(Instant.now()).lastModified(Instant.now()).build());


    final String url = BASE_URI + "/AllTypes";
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.POST,
                                                               allTypes.toString(),
                                                               httpHeaders,
                                                               new Context(null));

    Assertions.assertEquals(HttpStatus.BAD_REQUEST, scimResponse.getHttpStatus());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));

    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    String errorMessage = String.format("Request document is invalid it does not contain processable data '%s'",
                                        allTypes);
    Assertions.assertEquals(0, errorResponse.getFieldErrors().size(), errorResponse.toPrettyString());
    Assertions.assertEquals(1, errorResponse.getErrorMessages().size(), errorResponse.toPrettyString());
    Assertions.assertEquals(errorMessage, errorResponse.getErrorMessages().get(0), errorResponse.toPrettyString());
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
      ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                                 method,
                                                                 null,
                                                                 httpHeaders,
                                                                 new Context(clientAuthorization));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.FORBIDDEN, errorResponse.getHttpStatus());
      Assertions.assertEquals("You are not authorized to access the '" + endpointType + "' endpoint on resource type '"
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

    @Override
    public boolean authenticate(Map<String, String> httpHeaders, Map<String, String> queryParams)
    {
      return true;
    }

  }
}
