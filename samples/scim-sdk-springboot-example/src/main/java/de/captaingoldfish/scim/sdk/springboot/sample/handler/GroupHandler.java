package de.captaingoldfish.scim.sdk.springboot.sample.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 03.11.2019 - 00:14 <br>
 * <br>
 * a simple handler implementation for managing groups
 */
public class GroupHandler extends ResourceHandler<Group>
{

  /**
   * an in memory map that holds our 5000 groups
   */
  @Getter
  private Map<String, Group> inMemoryMap = Collections.synchronizedMap(new HashMap<>());

  /**
   * an in memory map that holds our 5000 groups
   */
  @Getter
  private Map<String, Group> groupnameMap = Collections.synchronizedMap(new HashMap<>());

  /**
   * adds approximately 5000 groups into the in memory map
   */
  public GroupHandler(boolean addTestGroups)
  {
    if (!addTestGroups)
    {
      return;
    }
    try (InputStream inputStream = getClass().getResourceAsStream("/groupnames.txt");
      InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
      BufferedReader reader = new BufferedReader(inputStreamReader))
    {

      String name;
      while ((name = reader.readLine()) != null)
      {
        String id = UUID.randomUUID().toString();
        Meta meta = Meta.builder().created(LocalDateTime.now()).lastModified(LocalDateTime.now()).build();
        inMemoryMap.put(id, Group.builder().id(id).displayName(name).meta(meta).build());
      }
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group createResource(Group resource, Context context)
  {
    final String groupId = UUID.randomUUID().toString();
    if (inMemoryMap.containsKey(groupId))
    {
      throw new ConflictException("resource with id '" + groupId + "' does already exist");
    }
    if (groupnameMap.get(resource.getDisplayName().get()) != null)
    {
      throw new ConflictException("resource with groupname '" + resource.getDisplayName().get()
                                  + "' does already exist");
    }
    resource.setId(groupId);
    inMemoryMap.put(groupId, resource);
    groupnameMap.put(resource.getDisplayName().get(), resource);
    resource.getMeta().ifPresent(meta -> {
      meta.setCreated(Instant.now());
      meta.setLastModified(Instant.now());
    });
    return resource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group getResource(String id,
                           List<SchemaAttribute> attributes,
                           List<SchemaAttribute> excludedAttributes,
                           Context context)
  {
    return inMemoryMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PartialListResponse<Group> listResources(long startIndex,
                                                  int count,
                                                  FilterNode filter,
                                                  SchemaAttribute sortBy,
                                                  SortOrder sortOrder,
                                                  List<SchemaAttribute> attributes,
                                                  List<SchemaAttribute> excludedAttributes,
                                                  Context context)
  {
    List<Group> resourceNodes = new ArrayList<>(inMemoryMap.values());
    return PartialListResponse.<Group> builder().resources(resourceNodes).totalResults(resourceNodes.size()).build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group updateResource(Group resource, Context context)
  {
    String groupId = resource.getId().get();
    Group oldGroup = inMemoryMap.get(groupId);
    if (oldGroup == null)
    {
      throw new ResourceNotFoundException("resource with id '" + groupId + "' does not exist", null, null);
    }
    inMemoryMap.put(groupId, resource);
    groupnameMap.remove(oldGroup.getDisplayName().get(), resource);
    groupnameMap.put(resource.getDisplayName().get(), resource);
    resource.getMeta().ifPresent(meta -> {
      meta.setCreated(oldGroup.getMeta().get().getCreated().get());
      meta.setLastModified(Instant.now());
    });
    return resource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(String id, Context context)
  {
    Group group = inMemoryMap.get(id);
    if (group != null)
    {
      inMemoryMap.remove(id);
      groupnameMap.remove(group.getDisplayName().get());
    }
    else
    {
      throw new ResourceNotFoundException("resource with id '" + id + "' does not exist", null, null);
    }
  }
}
