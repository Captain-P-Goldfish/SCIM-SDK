package de.gold.scim.endpoints;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.endpoints.base.UserResourceType;
import de.gold.scim.resources.User;
import de.gold.scim.response.ScimResponse;
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

  private ResourceEndpoints resourceEndpoints;

  private TestUserHandlerImpl userHandler;

  @BeforeEach
  public void initialize()
  {
    userHandler = Mockito.spy(new TestUserHandlerImpl());
    resourceEndpoints = new ResourceEndpoints(new UserResourceType(userHandler));
  }

  @Test
  public void testCreateResource()
  {
    ScimResponse scimResponse = resourceEndpoints.createResource("/Users", readResourceFile(USER_RESOURCE));
    Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any());
  }

  @Test
  public void testReadResource()
  {
    final String id = UUID.randomUUID().toString();
    ScimResponse scimResponse = resourceEndpoints.getResource("/Users", id);
    Mockito.verify(userHandler, Mockito.times(1)).readResource(id);
  }

  @Test
  public void testUpdateResource()
  {
    final String id = UUID.randomUUID().toString();
    ScimResponse scimResponse = resourceEndpoints.updateResource("/Users", id, readResourceFile(USER_RESOURCE));
    Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any());
  }

  @Test
  public void testDeleteResource()
  {
    final String id = UUID.randomUUID().toString();
    ScimResponse scimResponse = resourceEndpoints.deleteResource("/Users", id);
    Mockito.verify(userHandler, Mockito.times(1)).deleteResource(id);
  }

  private static class TestUserHandlerImpl extends ResourceHandler<User>
  {

    @Override
    public User createResource(User resource)
    {
      JsonHelper.addAttribute(resource, AttributeNames.ID, new TextNode(UUID.randomUUID().toString()));
      return resource;
    }

    @Override
    public User readResource(String id)
    {
      return null;
    }

    @Override
    public User listResources()
    {
      return null;
    }

    @Override
    public User updateResource(User resource)
    {
      return resource;
    }

    @Override
    public void deleteResource(String id)
    {

    }
  }
}
