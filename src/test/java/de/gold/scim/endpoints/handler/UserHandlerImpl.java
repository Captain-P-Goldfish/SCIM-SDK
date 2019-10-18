package de.gold.scim.endpoints.handler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.enums.SortOrder;
import de.gold.scim.endpoints.ResourceHandler;
import de.gold.scim.exceptions.ConflictException;
import de.gold.scim.exceptions.ResourceNotFoundException;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.resources.User;
import de.gold.scim.response.PartialListResponse;
import de.gold.scim.schemas.SchemaAttribute;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 00:05 <br>
 * <br>
 * a simple user resource handler for testing
 */
public class UserHandlerImpl extends ResourceHandler<User>
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
    resource.getMeta().ifPresent(meta -> {
      meta.setCreated(Instant.now());
      meta.setLastModified(Instant.now());
    });
    return resource;
  }

  @Override
  public User getResource(String id)
  {
    return inMemoryMap.get(id);
  }

  @Override
  public PartialListResponse listResources(int startIndex,
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
    String userId = resource.getId().get();
    User oldUser = getResource(userId);
    if (oldUser == null)
    {
      throw new ResourceNotFoundException("resource with id '" + userId + "' does not exist", null, null);
    }
    resource.getMeta().get().setCreated(oldUser.getMeta().get().getCreated().get());
    inMemoryMap.put(userId, resource);
    resource.getMeta().ifPresent(meta -> {
      meta.setLastModified(Instant.now());
    });
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
