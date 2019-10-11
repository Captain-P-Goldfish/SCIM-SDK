package de.gold.scim.endpoints;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  @BeforeEach
  public void initialize()
  {
    resourceEndpoints = new ResourceEndpoints(new UserResourceType(new TestUserHandlerImpl()));
  }

  @Test
  public void testCreateResource()
  {
    ScimResponse scimResponse = resourceEndpoints.createResource("/Users", readResourceFile(USER_RESOURCE));
    Assertions.fail("validate response");
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
    public User updateResource(User resource, String id)
    {
      return resource;
    }

    @Override
    public User deleteResource(String id)
    {
      return null;
    }
  }
}
