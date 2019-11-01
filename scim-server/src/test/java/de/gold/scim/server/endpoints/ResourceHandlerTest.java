package de.gold.scim.server.endpoints;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.server.constants.enums.SortOrder;
import de.gold.scim.server.exceptions.InternalServerException;
import de.gold.scim.server.filter.FilterNode;
import de.gold.scim.server.resources.ResourceNode;
import de.gold.scim.server.resources.User;
import de.gold.scim.server.response.PartialListResponse;
import de.gold.scim.server.schemas.SchemaAttribute;


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
   * checks that the developer is informed that a resource handler implementation must be generified
   */
  @Test
  public void testGetNonParametrizedImpl()
  {
    Assertions.assertThrows(InternalServerException.class, NonParametrizedResourceHandlerImpl::new);
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
    public User getResource(String id)
    {
      return null;
    }

    @Override
    public PartialListResponse listResources(long startIndex,
                                             int count,
                                             FilterNode filter,
                                             SchemaAttribute sortBy,
                                             SortOrder sortOrder)
    {
      return null;
    }

    @Override
    public User updateResource(User resource)
    {
      return null;
    }

    @Override
    public void deleteResource(String id)
    {

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
    public ResourceNode getResource(String id)
    {
      return null;
    }

    @Override
    public PartialListResponse listResources(long startIndex,
                                             int count,
                                             FilterNode filter,
                                             SchemaAttribute sortBy,
                                             SortOrder sortOrder)
    {
      return null;
    }

    @Override
    public ResourceNode updateResource(ResourceNode resource)
    {
      return null;
    }

    @Override
    public void deleteResource(String id)
    {

    }
  }
}
