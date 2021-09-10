package de.captaingoldfish.scim.sdk.server.endpoints.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.http.HttpHeaders;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 26.04.2021
 */
@Slf4j
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

    RequestValidator<User> requestValidator = Mockito.spy(new RequestValidator<User>()
    {

      @Override
      public void validateCreate(User resource, ValidationContext validationContext, Context requestContext)
      {
        Assertions.assertNotNull(validationContext);
        Assertions.assertNotNull(validationContext.getResourceType());
        Assertions.assertNotNull(validationContext.getErrors());
        Assertions.assertNotNull(validationContext.getFieldErrors());
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext,
                                 Context requestContext)
      {
        // is not used in this test
      }
    });
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    resourceEndpoint.handleRequest(url, HttpMethod.POST, user.toString(), httpHeaders);
    User storedUser = userHandler.getInMemoryMap().get(userHandler.getInMemoryMap().keySet().iterator().next());
    Mockito.verify(requestValidator, Mockito.times(1))
           .validateCreate(Mockito.eq(storedUser), Mockito.notNull(), Mockito.notNull());
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
      public void validateCreate(User resource, ValidationContext validationContext, Context requestContext)
      {
        // is not used in this test
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext,
                                 Context requestContext)
      {
        Assertions.assertNotNull(validationContext);
        Assertions.assertNotNull(validationContext.getResourceType());
        Assertions.assertNotNull(validationContext.getErrors());
        Assertions.assertNotNull(validationContext.getFieldErrors());
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
           .validateUpdate(Mockito.any(), Mockito.eq(updateUser), Mockito.notNull(), Mockito.notNull());
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
      public void validateCreate(User resource, ValidationContext validationContext, Context requestContext)
      {
        // is not used in this test
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext,
                                 Context requestContext)
      {
        Assertions.assertNotNull(validationContext);
        Assertions.assertNotNull(validationContext.getResourceType());
        Assertions.assertNotNull(validationContext.getErrors());
        Assertions.assertNotNull(validationContext.getFieldErrors());
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

    PatchOpRequest patchOpRequest = new PatchOpRequest();
    PatchRequestOperation operation = PatchRequestOperation.builder().op(PatchOp.ADD).valueNode(updateUser).build();
    patchOpRequest.setOperations(Collections.singletonList(operation));

    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    resourceEndpoint.handleRequest(url, HttpMethod.PATCH, patchOpRequest.toString(), httpHeaders);

    Mockito.verify(requestValidator, Mockito.times(1))
           .validateUpdate(Mockito.any(), Mockito.eq(updateUser), Mockito.notNull(), Mockito.notNull());
  }

  /**
   * This test will show that the errors from the schema validation are passed through to the
   * {@link RequestValidator} on a POST request and that additionally added errors are also returned in the
   * error response
   */
  @Test
  public void testSchemaValidationErrorsArePassedThroughToValidateCreateOnPost()
  {
    final String nickname = "captain";
    final User user = User.builder().id(UUID.randomUUID().toString()).nickName(nickname).build();
    final String url = BASE_URI + EndpointPaths.USERS;

    final String displayName = AttributeNames.RFC7643.DISPLAY_NAME;
    final String someErrorMessage = "this is some error";
    RequestValidator<User> requestValidator = Mockito.spy(new RequestValidator<User>()
    {

      @Override
      public void validateCreate(User resource, ValidationContext validationContext, Context requestContext)
      {
        Assertions.assertTrue(validationContext.hasErrors());
        Assertions.assertEquals(0, validationContext.getErrors().size());
        Assertions.assertEquals(1, validationContext.getFieldErrors().size());
        Assertions.assertEquals(1, validationContext.getFieldErrors().get(AttributeNames.RFC7643.USER_NAME).size());
        Assertions.assertEquals("Required 'READ_WRITE' attribute "
                                + "'urn:ietf:params:scim:schemas:core:2.0:User:userName' is missing",
                                validationContext.getFieldErrors().get(AttributeNames.RFC7643.USER_NAME).get(0));
        validationContext.addError("");
        validationContext.addError(someErrorMessage);
        validationContext.addError(displayName, someErrorMessage);
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext,
                                 Context requestContext)
      {
        // is not used in this test
      }
    });
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    userHandler.getInMemoryMap().put(user.getId().get(), user);

    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, user.toString(), httpHeaders);
    Mockito.verify(requestValidator, Mockito.times(1))
           .validateCreate(Mockito.any(), Mockito.notNull(), Mockito.notNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));

    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    List<String> errors = errorResponse.getErrorMessages();
    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(someErrorMessage, errors.get(0));

    Map<String, List<String>> fieldErrors = errorResponse.getFieldErrors();
    Assertions.assertEquals(2, fieldErrors.size());
    List<String> userNameErrors = fieldErrors.get(AttributeNames.RFC7643.USER_NAME);
    Assertions.assertEquals(1, userNameErrors.size());
    Assertions.assertEquals("Required 'READ_WRITE' attribute "
                            + "'urn:ietf:params:scim:schemas:core:2.0:User:userName' is missing",
                            userNameErrors.get(0));
    List<String> displayNameErrors = fieldErrors.get(displayName);
    Assertions.assertEquals(1, displayNameErrors.size());
    Assertions.assertEquals(someErrorMessage, displayNameErrors.get(0));
  }

  /**
   * This test will show that the errors from the schema validation are passed through to the
   * {@link RequestValidator} on a PUT request and that additionally added errors are also returned in the error
   * response
   */
  @Test
  public void testSchemaValidationErrorsArePassedThroughToValidateCreateOnPut()
  {
    final String userName = "goldfish";
    final String nickname = "captain";
    final User user = User.builder().id(UUID.randomUUID().toString()).userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS + "/" + user.getId().get();

    final String displayName = AttributeNames.RFC7643.DISPLAY_NAME;
    final String someErrorMessage = "this is some error";
    RequestValidator<User> requestValidator = Mockito.spy(new RequestValidator<User>()
    {

      @Override
      public void validateCreate(User resource, ValidationContext validationContext, Context requestContext)
      {
        // is not used in this test
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext,
                                 Context requestContext)
      {
        Assertions.assertTrue(validationContext.hasErrors());
        Assertions.assertEquals(0, validationContext.getErrors().size());
        Assertions.assertEquals(1, validationContext.getFieldErrors().size());
        Assertions.assertEquals(1, validationContext.getFieldErrors().get(AttributeNames.RFC7643.USER_NAME).size());
        Assertions.assertEquals("Required 'READ_WRITE' attribute "
                                + "'urn:ietf:params:scim:schemas:core:2.0:User:userName' is missing",
                                validationContext.getFieldErrors().get(AttributeNames.RFC7643.USER_NAME).get(0));
        validationContext.addError("");
        validationContext.addError(someErrorMessage);
        validationContext.addError(displayName, someErrorMessage);
      }
    });
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    userHandler.getInMemoryMap().put(user.getId().get(), user);

    User updateUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    updateUser.setUserName(null);
    updateUser.setNickName(nickname);

    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.PUT, updateUser.toString(), httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));

    Mockito.verify(requestValidator, Mockito.times(1))
           .validateUpdate(Mockito.any(), Mockito.any(), Mockito.notNull(), Mockito.notNull());

    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    List<String> errors = errorResponse.getErrorMessages();
    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(someErrorMessage, errors.get(0));

    Map<String, List<String>> fieldErrors = errorResponse.getFieldErrors();
    Assertions.assertEquals(2, fieldErrors.size());
    List<String> userNameErrors = fieldErrors.get(AttributeNames.RFC7643.USER_NAME);
    Assertions.assertEquals(1, userNameErrors.size());
    Assertions.assertEquals("Required 'READ_WRITE' attribute "
                            + "'urn:ietf:params:scim:schemas:core:2.0:User:userName' is missing",
                            userNameErrors.get(0));
    List<String> displayNameErrors = fieldErrors.get(displayName);
    Assertions.assertEquals(1, displayNameErrors.size());
    Assertions.assertEquals(someErrorMessage, displayNameErrors.get(0));
  }

  /**
   * This test will show that the errors from the schema validation are passed through to the
   * {@link RequestValidator} on a PATCH request and that additionally added errors are also returned in the
   * error response
   */
  @Test
  public void testSchemaValidationErrorsArePassedThroughToValidateCreateOnPatch()
  {
    serviceProvider.getPatchConfig().setSupported(true);

    final String userName = "goldfish";
    final String nickname = "captain";
    final User user = User.builder().id(UUID.randomUUID().toString()).userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS + "/" + user.getId().get();

    final String displayName = AttributeNames.RFC7643.DISPLAY_NAME;
    final String someErrorMessage = "this is some error";
    RequestValidator<User> requestValidator = Mockito.spy(new RequestValidator<User>()
    {

      @Override
      public void validateCreate(User resource, ValidationContext validationContext, Context requestContext)
      {
        // is not used in this test
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext,
                                 Context requestContext)
      {
        Assertions.assertTrue(validationContext.hasErrors());
        Assertions.assertEquals(0, validationContext.getErrors().size());
        Assertions.assertEquals(1, validationContext.getFieldErrors().size());
        Assertions.assertEquals(1, validationContext.getFieldErrors().get(AttributeNames.RFC7643.USER_NAME).size());
        Assertions.assertEquals("Required 'READ_WRITE' attribute "
                                + "'urn:ietf:params:scim:schemas:core:2.0:User:userName' is missing",
                                validationContext.getFieldErrors().get(AttributeNames.RFC7643.USER_NAME).get(0));
        validationContext.addError("");
        validationContext.addError(someErrorMessage);
        validationContext.addError(displayName, someErrorMessage);
      }
    });
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    userHandler.getInMemoryMap().put(user.getId().get(), user);

    User updateUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    updateUser.setUserName(null);
    updateUser.setNickName(nickname);

    PatchOpRequest patchOpRequest = new PatchOpRequest();
    PatchRequestOperation operation1 = PatchRequestOperation.builder()
                                                            .op(PatchOp.REMOVE)
                                                            .path(AttributeNames.RFC7643.USER_NAME)
                                                            .build();
    PatchRequestOperation operation2 = PatchRequestOperation.builder().op(PatchOp.ADD).valueNode(updateUser).build();
    patchOpRequest.setOperations(Arrays.asList(operation1, operation2));

    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url,
                                                               HttpMethod.PATCH,
                                                               patchOpRequest.toString(),
                                                               httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));

    Mockito.verify(requestValidator, Mockito.times(1))
           .validateUpdate(Mockito.any(), Mockito.any(), Mockito.notNull(), Mockito.notNull());

    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    List<String> errors = errorResponse.getErrorMessages();
    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(someErrorMessage, errors.get(0));

    Map<String, List<String>> fieldErrors = errorResponse.getFieldErrors();
    Assertions.assertEquals(2, fieldErrors.size());
    List<String> userNameErrors = fieldErrors.get(AttributeNames.RFC7643.USER_NAME);
    Assertions.assertEquals(1, userNameErrors.size());
    Assertions.assertEquals("Required 'READ_WRITE' attribute "
                            + "'urn:ietf:params:scim:schemas:core:2.0:User:userName' is missing",
                            userNameErrors.get(0));
    List<String> displayNameErrors = fieldErrors.get(displayName);
    Assertions.assertEquals(1, displayNameErrors.size());
    Assertions.assertEquals(someErrorMessage, displayNameErrors.get(0));
  }

  /**
   * shows that an internal server error is thrown if the developer adds a field name that does not exist on the
   * resource
   */
  @Test
  public void testValidateFieldNameMustExistOnResourceType()
  {
    final String userName = "goldfish";
    final User user = User.builder().userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS;

    RequestValidator<User> requestValidator = Mockito.spy(new RequestValidator<User>()
    {

      @Override
      public void validateCreate(User resource, ValidationContext validationContext, Context requestContext)
      {
        validationContext.addError("unknown-attribute", "blubb");
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext,
                                 Context requestContext)
      {
        // is not used in this test
      }
    });
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, user.toString(), httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    Mockito.verify(requestValidator, Mockito.times(1))
           .validateCreate(Mockito.any(), Mockito.notNull(), Mockito.notNull());
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getStatus());
    Assertions.assertEquals("An internal error has occurred.", errorResponse.getDetail().get());
  }

  /**
   * shows that the configured http response status is returned that was set by the user
   */
  @Test
  public void testCorrectHttpResponseStatusIsReturned()
  {
    final String userName = "goldfish";
    final User user = User.builder().userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS;

    RequestValidator<User> requestValidator = Mockito.spy(new RequestValidator<User>()
    {

      @Override
      public void validateCreate(User resource, ValidationContext validationContext, Context requestContext)
      {
        validationContext.setHttpResponseStatus(HttpStatus.CONFLICT);
        validationContext.addError("blubb");
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext,
                                 Context requestContext)
      {
        // is not used in this test
      }
    });
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, user.toString(), httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    Mockito.verify(requestValidator, Mockito.times(1))
           .validateCreate(Mockito.any(), Mockito.notNull(), Mockito.notNull());
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.CONFLICT, errorResponse.getStatus());
    Assertions.assertEquals("blubb", errorResponse.getDetail().get());
  }

  /**
   * shows that the configured http response headers are returned if set in the validation context
   */
  @Test
  public void testCorrectHttpHeadersAreReturned()
  {
    final String userName = "goldfish";
    final User user = User.builder().userName(userName).build();
    final String url = BASE_URI + EndpointPaths.USERS;

    RequestValidator<User> requestValidator = Mockito.spy(new RequestValidator<User>()
    {

      @Override
      public void validateCreate(User resource, ValidationContext validationContext, Context requestContext)
      {
        validationContext.getResponseHttpHeaders().put(HttpHeaders.CACHE_CONTROL, "no-cache");
        validationContext.getResponseHttpHeaders().put(HttpHeaders.CONTENT_LENGTH, "5");
        validationContext.addError("blubb");
      }

      @Override
      public void validateUpdate(Supplier<User> oldResourceSupplier,
                                 User newResource,
                                 ValidationContext validationContext,
                                 Context requestContext)
      {
        // is not used in this test
      }
    });
    UserHandlerImpl userHandler = Mockito.spy(new UserHandlerImpl(true, requestValidator));
    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(userHandler));
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.POST, user.toString(), httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    Mockito.verify(requestValidator, Mockito.times(1))
           .validateCreate(Mockito.any(), Mockito.notNull(), Mockito.notNull());
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatus());
    Assertions.assertEquals("blubb", errorResponse.getDetail().get());
    Assertions.assertTrue(errorResponse.getHttpHeaders().containsKey(HttpHeaders.CACHE_CONTROL));
    Assertions.assertEquals("no-cache", errorResponse.getHttpHeaders().get(HttpHeaders.CACHE_CONTROL));
    Assertions.assertTrue(errorResponse.getHttpHeaders().containsKey(HttpHeaders.CONTENT_LENGTH));
    Assertions.assertEquals("5", errorResponse.getHttpHeaders().get(HttpHeaders.CONTENT_LENGTH));
  }
}
