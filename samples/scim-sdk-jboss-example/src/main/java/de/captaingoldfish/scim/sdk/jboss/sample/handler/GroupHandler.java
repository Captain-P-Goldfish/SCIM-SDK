package de.captaingoldfish.scim.sdk.jboss.sample.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
  private Map<String, Group> inMemoryMap = new HashMap<>();

  /**
   * adds approximately 5000 groups into the in memory map
   */
  public GroupHandler()
  {
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
  public Group createResource(Group resource)
  {
    final String groupId = UUID.randomUUID().toString();
    if (inMemoryMap.containsKey(groupId))
    {
      throw new ConflictException("resource with id '" + groupId + "' does already exist");
    }
    resource.setId(groupId);
    inMemoryMap.put(groupId, resource);
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
  public Group getResource(String id)
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
                                                  List<SchemaAttribute> excludedAttributes)
  {
    List<Group> resourceNodes = new ArrayList<>(inMemoryMap.values());
    return PartialListResponse.<Group> builder().resources(resourceNodes).totalResults(resourceNodes.size()).build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group updateResource(Group resource)
  {
    String groupId = resource.getId().get();
    Group oldGroup = inMemoryMap.get(groupId);
    if (oldGroup == null)
    {
      throw new ResourceNotFoundException("resource with id '" + groupId + "' does not exist", null, null);
    }
    inMemoryMap.put(groupId, resource);
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
