package de.gold.scim.endpoints;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.resources.ResourceNode;
import de.gold.scim.resources.User;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 17:01 <br>
 * <br>
 */
public class ResourceHandlerTest
{

  /**
   * checks that the instance creation does not fail if the generic parameter has been set correctly
   */
  @Test
  public void testGetParametrizedImpl()
  {
    Assertions.assertDoesNotThrow(ParametrizedResourceHandlerImpl::new);
  }

  /**
   * checks that the instance creation does not fail if no generic parameter was set
   */
  @Test
  public void testGetNonParametrizedImpl()
  {
    Assertions.assertDoesNotThrow(NonParametrizedResourceHandlerImpl::new);
  }

  /**
   * a resource handler impl with a set generic type
   */
  private static class ParametrizedResourceHandlerImpl extends ResourceHandler<User>
  {

    @Override
    public User createResource(User resource)
    {
      return null;
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
      return null;
    }

    @Override
    public User deleteResource(String id)
    {
      return null;
    }
  }

  /**
   * a resource handler impl without a set generic type
   */
  private static class NonParametrizedResourceHandlerImpl extends ResourceHandler
  {

    @Override
    public ResourceNode createResource(ResourceNode resource)
    {
      return null;
    }

    @Override
    public ResourceNode readResource(String id)
    {
      return null;
    }

    @Override
    public ResourceNode listResources()
    {
      return null;
    }

    @Override
    public ResourceNode updateResource(ResourceNode resource, String id)
    {
      return null;
    }

    @Override
    public ResourceNode deleteResource(String id)
    {
      return null;
    }
  }
}
