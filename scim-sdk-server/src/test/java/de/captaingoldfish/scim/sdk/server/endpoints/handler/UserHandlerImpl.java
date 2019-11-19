package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;

import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 00:05 <br>
 * <br>
 * a simple user resource handler for testing
 */
@Slf4j
public class UserHandlerImpl extends ResourceHandler<User>
{

  @Getter
  private Map<String, User> inMemoryMap = new HashMap<>();

  @Override
  public User createResource(User resource)
  {
    Assertions.assertTrue(resource.getMeta().isPresent());
    Meta meta = resource.getMeta().get();
    Assertions.assertFalse(meta.getLocation().isPresent());
    Assertions.assertTrue(meta.getResourceType().isPresent());
    Assertions.assertEquals(ResourceTypeNames.USER, meta.getResourceType().get());
    final String userId = UUID.randomUUID().toString();
    if (inMemoryMap.containsKey(userId))
    {
      throw new ConflictException("resource with id '" + userId + "' does already exist");
    }
    resource.setId(userId);
    inMemoryMap.put(userId, resource);
    meta.setCreated(Instant.now());
    meta.setLastModified(Instant.now());
    return resource;
  }

  @Override
  public User getResource(String id)
  {
    return inMemoryMap.get(id);
  }

  @Override
  public PartialListResponse<User> listResources(long startIndex,
                                                 int count,
                                                 FilterNode filter,
                                                 SchemaAttribute sortBy,
                                                 SortOrder sortOrder,
                                                 List<SchemaAttribute> attributes,
                                                 List<SchemaAttribute> excludedAttributes)
  {
    List<User> resourceNodes = new ArrayList<>(inMemoryMap.values());
    return PartialListResponse.<User> builder().resources(resourceNodes).totalResults(resourceNodes.size()).build();
  }

  @Override
  public User updateResource(User resource)
  {
    Assertions.assertTrue(resource.getMeta().isPresent());
    Meta meta = resource.getMeta().get();
    Assertions.assertTrue(meta.getLocation().isPresent());
    Assertions.assertTrue(meta.getResourceType().isPresent());
    Assertions.assertEquals(ResourceTypeNames.USER, meta.getResourceType().get());
    String userId = resource.getId().get();
    User oldUser = inMemoryMap.get(userId);
    if (oldUser == null)
    {
      throw new ResourceNotFoundException("resource with id '" + userId + "' does not exist", null, null);
    }
    inMemoryMap.put(userId, resource);
    meta.setCreated(oldUser.getMeta().get().getCreated().get());
    meta.setLastModified(Instant.now());
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
