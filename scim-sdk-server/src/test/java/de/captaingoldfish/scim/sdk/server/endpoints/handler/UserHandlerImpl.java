package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 00:05 <br>
 * <br>
 * a simple user resource handler for testing
 */
@Slf4j
@RequiredArgsConstructor
public class UserHandlerImpl extends ResourceHandler<User>
{

  private final boolean returnETags;

  @Getter
  private Map<String, User> inMemoryMap = new HashMap<>();

  @Override
  public User createResource(User resource, Authorization authorization)
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
    resource.remove(AttributeNames.RFC7643.META);
    resource.setMeta(Meta.builder().created(Instant.now()).lastModified(Instant.now()).build());
    return resource;
  }

  @Override
  public User getResource(String id, Authorization authorization)
  {
    User user = inMemoryMap.get(id);
    if (user != null)
    {
      Meta meta = user.getMeta().orElse(Meta.builder().build());
      user.remove(AttributeNames.RFC7643.META);
      user.setMeta(Meta.builder()
                       .created(meta.getCreated().orElse(null))
                       .lastModified(meta.getLastModified().orElse(null))
                       .version(returnETags ? meta.getVersion().orElse(null) : null)
                       .build());
    }
    return user;
  }

  @Override
  public PartialListResponse<User> listResources(long startIndex,
                                                 int count,
                                                 FilterNode filter,
                                                 SchemaAttribute sortBy,
                                                 SortOrder sortOrder,
                                                 List<SchemaAttribute> attributes,
                                                 List<SchemaAttribute> excludedAttributes,
                                                 Authorization authorization)
  {
    List<User> resourceNodes = new ArrayList<>(inMemoryMap.values());
    resourceNodes.forEach(user -> {
      Meta meta = user.getMeta().get();
      user.remove(AttributeNames.RFC7643.META);
      user.setMeta(Meta.builder()
                       .created(meta.getCreated().get())
                       .lastModified(meta.getLastModified().get())
                       .version(returnETags ? meta.getVersion().orElse(null) : null)
                       .build());
    });
    return PartialListResponse.<User> builder().resources(resourceNodes).totalResults(resourceNodes.size()).build();
  }

  @Override
  public User updateResource(User resource, Authorization authorization)
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
    Meta oldMeta = oldUser.getMeta().get();
    Instant lastModified = resource.equals(inMemoryMap.get(userId)) ? oldMeta.getCreated().get() : Instant.now();
    resource.remove(AttributeNames.RFC7643.META);
    resource.setMeta(Meta.builder()
                         .created(oldMeta.getCreated().get())
                         .lastModified(lastModified)
                         .version(returnETags ? oldMeta.getVersion().orElse(null) : null)
                         .build());
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
