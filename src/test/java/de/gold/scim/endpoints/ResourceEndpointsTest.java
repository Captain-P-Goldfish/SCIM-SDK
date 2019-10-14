package de.gold.scim.endpoints;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpHeader;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.endpoints.base.UserResourceType;
import de.gold.scim.exceptions.ConflictException;
import de.gold.scim.exceptions.ResourceNotFoundException;
import de.gold.scim.resources.User;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.response.CreateResponse;
import de.gold.scim.response.DeleteResponse;
import de.gold.scim.response.ErrorResponse;
import de.gold.scim.response.GetResponse;
import de.gold.scim.response.ScimResponse;
import de.gold.scim.response.UpdateResponse;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 23:54 <br>
 * <br>
 */
@Slf4j
public class ResourceEndpointsTest implements FileReferences
{

  /**
   * the resource endpoints implementation that will handle any request
   */
  private ResourceEndpoints resourceEndpoints;

  /**
   * a mockito mock to verify that the methods are called correctly by the {@link ResourceEndpoints}
   * implementation
   */
  private TestUserHandlerImpl userHandler;

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    userHandler = Mockito.spy(new TestUserHandlerImpl());
    resourceEndpoints = new ResourceEndpoints(new UserResourceType(userHandler));
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
    Assertions.assertEquals(createdUser, readUser);

    User updatedUser = updateUser(endpoint, readUser);
    Assertions.assertEquals(userId, updatedUser.getId().get());
    ScimResponse deleteResponse = resourceEndpoints.deleteResource(endpoint, userId);
    MatcherAssert.assertThat(deleteResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
    Mockito.verify(userHandler, Mockito.times(1)).deleteResource(userId);
    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> resourceEndpoints.getResource(endpoint, userId));
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
    ScimResponse scimResponse = resourceEndpoints.createResource(endpoint,
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
    return user;
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
    ScimResponse scimResponse = resourceEndpoints.getResource(endpoint, userId, getBaseUrlSupplier());
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

    ScimResponse scimResponse = resourceEndpoints.updateResource(endpoint,
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
    return getBaseUrlSupplier().get() + "/" + endpoint + "/" + resourceId;
  }

  /**
   * the base uri supplier that is given to the endpoint implementations
   */
  private Supplier<String> getBaseUrlSupplier()
  {
    return () -> "https://goldfish.de/scim/v2";
  }

  /**
   * a very simple test implementation for the users endpoint
   */
  private static class TestUserHandlerImpl extends ResourceHandler<User>
  {

    private Map<String, User> inMemoryMap = new HashMap<>();

    @Override
    public User createResource(User resource)
    {
      final String userId = UUID.randomUUID().toString();
      if (inMemoryMap.containsKey(userId))
      {
        throw new ConflictException("resource with id '" + userId + "' does already exist");
      }
      JsonHelper.addAttribute(resource, AttributeNames.ID, new TextNode(userId));
      inMemoryMap.put(userId, resource);
      return resource;
    }

    @Override
    public User getResource(String id)
    {
      return inMemoryMap.get(id);
    }

    @Override
    public User listResources()
    {
      return null;
    }

    @Override
    public User updateResource(User resource)
    {
      String userId = resource.getId().get();
      User oldUser = getResource(userId);
      if (oldUser == null)
      {
        throw new ResourceNotFoundException("resource with id '" + userId + "' does not exist", null, null);
      }
      resource.getMeta().get().setCreated(oldUser.getMeta().get().getCreated().get());
      inMemoryMap.put(userId, resource);
      return resource;
    }

    @Override
    public void deleteResource(String id)
    {
      if (inMemoryMap.containsKey(id))
      {
        inMemoryMap.remove(id);
      }
      else
      {
        throw new ResourceNotFoundException("resource with id '" + id + "' does not exist", null, null);
      }
    }
  }
}
