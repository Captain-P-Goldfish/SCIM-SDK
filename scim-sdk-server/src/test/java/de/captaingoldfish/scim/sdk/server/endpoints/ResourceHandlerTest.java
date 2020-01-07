package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;


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
    public User createResource(User resource, Authorization authorization)
    {
      return null;
    }

    @Override
    public User getResource(String id, Authorization authorization)
    {
      return null;
    }

    @Override
    public PartialListResponse listResources(long startIndex,
                                             int count,
                                             FilterNode filter,
                                             SchemaAttribute sortBy,
                                             SortOrder sortOrder,
                                             List<SchemaAttribute> attributes,
                                             List<SchemaAttribute> excludedAttributes,
                                             Authorization authorization)
    {
      return null;
    }

    @Override
    public User updateResource(User resource, Authorization authorization)
    {
      return null;
    }

    @Override
    public void deleteResource(String id, Authorization authorization)
    {

    }
  }

  /**
   * a resource handler impl without a set generic type
   */
  private static class NonParametrizedResourceHandlerImpl extends ResourceHandler
  {

    @Override
    public ResourceNode createResource(ResourceNode resource, Authorization authorization)
    {
      return null;
    }

    @Override
    public ResourceNode getResource(String id, Authorization authorization)
    {
      return null;
    }

    @Override
    public PartialListResponse<ResourceNode> listResources(long startIndex,
                                                           int count,
                                                           FilterNode filter,
                                                           SchemaAttribute sortBy,
                                                           SortOrder sortOrder,
                                                           List list,
                                                           List excludedAttributes,
                                                           Authorization authorization)
    {
      return null;
    }


    @Override
    public ResourceNode updateResource(ResourceNode resource, Authorization authorization)
    {
      return null;
    }

    @Override
    public void deleteResource(String id, Authorization authorization)
    {

    }
  }
}
