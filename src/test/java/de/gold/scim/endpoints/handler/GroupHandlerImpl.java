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
import de.gold.scim.resources.Group;
import de.gold.scim.response.PartialListResponse;
import de.gold.scim.schemas.SchemaAttribute;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 00:05 <br>
 * <br>
 * a simple group resource handler for testing
 */
public class GroupHandlerImpl extends ResourceHandler<Group>
{

  private Map<String, Group> inMemoryMap = new HashMap<>();

  @Override
  public Group createResource(Group resource)
  {
    final String groupId = UUID.randomUUID().toString();
    if (inMemoryMap.containsKey(groupId))
    {
      throw new ConflictException("resource with id '" + groupId + "' does already exist");
    }
    JsonHelper.addAttribute(resource, AttributeNames.RFC7643.ID, new TextNode(groupId));
    inMemoryMap.put(groupId, resource);
    resource.getMeta().ifPresent(meta -> {
      meta.setCreated(Instant.now());
      meta.setLastModified(Instant.now());
    });
    return resource;
  }

  @Override
  public Group getResource(String id)
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
  public Group updateResource(Group resource)
  {
    String groupId = resource.getId().get();
    Group oldGroup = getResource(groupId);
    if (oldGroup == null)
    {
      throw new ResourceNotFoundException("resource with id '" + groupId + "' does not exist", null, null);
    }
    resource.getMeta().get().setCreated(oldGroup.getMeta().get().getCreated().get());
    inMemoryMap.put(groupId, resource);
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
