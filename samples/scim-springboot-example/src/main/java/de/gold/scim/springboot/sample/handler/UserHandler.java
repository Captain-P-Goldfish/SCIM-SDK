package de.gold.scim.springboot.sample.handler;

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

import de.gold.scim.common.constants.ResourceTypeNames;
import de.gold.scim.common.constants.enums.SortOrder;
import de.gold.scim.common.exceptions.ConflictException;
import de.gold.scim.common.exceptions.ResourceNotFoundException;
import de.gold.scim.common.resources.User;
import de.gold.scim.common.resources.complex.Meta;
import de.gold.scim.common.schemas.SchemaAttribute;
import de.gold.scim.server.endpoints.ResourceHandler;
import de.gold.scim.server.filter.FilterNode;
import de.gold.scim.server.response.PartialListResponse;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 03.11.2019 - 00:14 <br>
 * <br>
 */
public class UserHandler extends ResourceHandler<User>
{

  /**
   * an in memory map that holds our 5000 users
   */
  @Getter
  private Map<String, User> inMemoryMap = new HashMap<>();

  /**
   * adds approximately 5000 users into the in memory map
   */
  public UserHandler()
  {
    try (InputStream inputStream = getClass().getResourceAsStream("/firstnames.txt");
      InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
      BufferedReader reader = new BufferedReader(inputStreamReader))
    {

      String name;
      while ((name = reader.readLine()) != null)
      {
        String id = UUID.randomUUID().toString();
        Meta meta = Meta.builder()
                        .created(LocalDateTime.now())
                        .lastModified(LocalDateTime.now())
                        .resourceType(ResourceTypeNames.USER)
                        .build();
        inMemoryMap.put(id, User.builder().id(id).userName(name).nickName(name).meta(meta).build());
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
  public User createResource(User resource)
  {
    final String userId = UUID.randomUUID().toString();
    if (inMemoryMap.containsKey(userId))
    {
      throw new ConflictException("resource with id '" + userId + "' does already exist");
    }
    resource.setId(userId);
    inMemoryMap.put(userId, resource);
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
  public User getResource(String id)
  {
    return inMemoryMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public User updateResource(User resource)
  {
    String userId = resource.getId().get();
    User oldUser = inMemoryMap.get(userId);
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
