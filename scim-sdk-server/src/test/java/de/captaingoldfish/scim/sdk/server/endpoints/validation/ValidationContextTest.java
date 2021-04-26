package de.captaingoldfish.scim.sdk.server.endpoints.validation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;


/**
 * @author Pascal Knueppel
 * @since 26.04.2021
 */
public class ValidationContextTest
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
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
  }

  /**
   * shows that the {@link RequestValidator#validateCreate(ResourceNode, ValidationContext)} implementation is
   * called on a POST request if a {@link RequestValidator} implementation is returned by the
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler} implementation
   */
  @Test
  public void testValidateCustomContextOnCreate()
  {
    final String userName = "goldfish";
    final User user = User.builder().userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS;

    RequestValidator<User> requestValidator = Mockito.mock(RequestValidator.class);
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    resourceEndpoint.handleRequest(url, HttpMethod.POST, user.toString(), httpHeaders);
    User storedUser = userHandler.getInMemoryMap().get(userHandler.getInMemoryMap().keySet().iterator().next());
    Mockito.verify(requestValidator, Mockito.times(1)).validateCreate(Mockito.eq(storedUser), Mockito.notNull());
  }

  /**
   * shows that the {@link RequestValidator#validateUpdate(Supplier, ResourceNode, ValidationContext)}
   * implementation is called on a PUT request if a {@link RequestValidator} implementation is returned by the
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler} implementation
   */
  @Test
  public void testValidateCustomContextOnPut()
  {
    final String userName = "goldfish";
    final String nickname = "captain";
    final User user = User.builder().id(UUID.randomUUID().toString()).userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS + "/" + user.getId().get();
    User updateUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);

    RequestValidator<User> requestValidator = Mockito.spy(new RequestValidator<User>()
    {

      @Override
      public void validateCreate(User resource, ValidationContext validationContext)
      {
        // is not used in this test
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext)
      {
        Assertions.assertEquals(updateUser.getUserName().get(), newResource.getUserName().get());
        Assertions.assertTrue(updateUser.getNickName().isPresent());
        Assertions.assertEquals(updateUser.getNickName().get(), newResource.getNickName().get());
        User oldUser = oldResourceSupplier.get();
        Assertions.assertEquals(user, oldUser);
        Assertions.assertEquals(user.getUserName().get(), oldUser.getUserName().get());
        Assertions.assertFalse(user.getNickName().isPresent());
        Assertions.assertFalse(oldUser.getNickName().isPresent());
      }
    });
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    userHandler.getInMemoryMap().put(user.getId().get(), user);

    updateUser.setNickName(nickname);

    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    resourceEndpoint.handleRequest(url, HttpMethod.PUT, updateUser.toString(), httpHeaders);

    Mockito.verify(requestValidator, Mockito.times(1))
           .validateUpdate(Mockito.any(), Mockito.eq(updateUser), Mockito.notNull());
  }

  /**
   * shows that the {@link RequestValidator#validateUpdate(Supplier, ResourceNode, ValidationContext)}
   * implementation is called on a PATCH request if a {@link RequestValidator} implementation is returned by the
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler} implementation
   */
  @Test
  public void testValidateCustomContextOnPatch()
  {
    serviceProvider.getPatchConfig().setSupported(true);

    final String userName = "goldfish";
    final String nickname = "captain";
    final User user = User.builder().id(UUID.randomUUID().toString()).userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS + "/" + user.getId().get();
    User updateUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    updateUser.setNickName(nickname);

    RequestValidator<User> requestValidator = Mockito.spy(new RequestValidator<User>()
    {

      @Override
      public void validateCreate(User resource, ValidationContext validationContext)
      {
        // is not used in this test
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext)
      {
        // TODO
        // Assertions.assertEquals(updateUser.getUserName().get(), newResource.getUserName().get());
        // Assertions.assertTrue(updateUser.getNickName().isPresent());
        // Assertions.assertEquals(updateUser.getNickName().get(), newResource.getNickName().get());
        // User oldUser = oldResourceSupplier.get();
        // Assertions.assertEquals(user, oldUser);
        // Assertions.assertEquals(user.getUserName().get(), oldUser.getUserName().get());
        // Assertions.assertFalse(user.getNickName().isPresent());
        // Assertions.assertFalse(oldUser.getNickName().isPresent());
      }
    });
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    userHandler.getInMemoryMap().put(user.getId().get(), user);

    PatchOpRequest patchOpRequest = new PatchOpRequest();
    PatchRequestOperation operation = PatchRequestOperation.builder().op(PatchOp.ADD).valueNode(updateUser).build();
    patchOpRequest.setOperations(Collections.singletonList(operation));

    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    resourceEndpoint.handleRequest(url, HttpMethod.PATCH, patchOpRequest.toString(), httpHeaders);

    Mockito.verify(requestValidator, Mockito.times(1))
           .validateUpdate(Mockito.any(), Mockito.eq(updateUser), Mockito.notNull());
  }

}
