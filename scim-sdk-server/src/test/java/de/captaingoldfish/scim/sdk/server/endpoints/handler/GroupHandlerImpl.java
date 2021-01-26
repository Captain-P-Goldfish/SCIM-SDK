package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 00:05 <br>
 * <br>
 * a simple group resource handler for testing
 */
public class GroupHandlerImpl extends ResourceHandler<Group>
{

  @Getter
  private Map<String, Group> inMemoryMap = new HashMap<>();

  @Override
  public Group createResource(Group resource, Authorization authorization)
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
  public Group getResource(String id,
                           Authorization authorization,
                           List<SchemaAttribute> attributes,
                           List<SchemaAttribute> excludedAttributes)
  {
    return inMemoryMap.get(id);
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
    return PartialListResponse.builder()
                              .resources(new ArrayList<>(inMemoryMap.values()))
                              .totalResults(inMemoryMap.size())
                              .build();
  }

  @Override
  public Group updateResource(Group resource, Authorization authorization)
  {
    String groupId = resource.getId().get();
    Group oldGroup = getResource(groupId, null, null, null);
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
  public void deleteResource(String id, Authorization authorization)
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
