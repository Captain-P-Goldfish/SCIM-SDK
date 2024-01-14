package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 06.01.2024 - 19:34 <br>
 * <br>
 * a simple generic resource handler for testing
 */
@Slf4j
public class AllTypesHandlerImpl extends ResourceHandler<AllTypes>
{


  @Getter
  private Map<String, AllTypes> inMemoryMap = new HashMap<>();

  @Override
  public AllTypes createResource(AllTypes resource, Context context)
  {
    Assertions.assertTrue(resource.getMeta().isPresent());
    Meta meta = resource.getMeta().get();
    Assertions.assertTrue(meta.getResourceType().isPresent());
    Assertions.assertEquals(ResourceTypeNames.USER, meta.getResourceType().get());
    final String resourceId = UUID.randomUUID().toString();
    if (inMemoryMap.containsKey(resourceId))
    {
      throw new ConflictException("resource with id '" + resourceId + "' does already exist");
    }
    resource.setId(resourceId);
    inMemoryMap.put(resourceId, resource);
    resource.remove(AttributeNames.RFC7643.META);
    Instant created = Instant.now();
    resource.setMeta(Meta.builder().created(created).lastModified(created).build());
    return resource;
  }

  @Override
  public AllTypes getResource(String id,
                              List<SchemaAttribute> attributes,
                              List<SchemaAttribute> excludedAttributes,
                              Context context)
  {
    AllTypes resource = inMemoryMap.get(id);
    if (resource != null)
    {
      Meta meta = resource.getMeta().orElse(Meta.builder().build());
      resource.remove(AttributeNames.RFC7643.META);
      meta = Meta.builder()
                 .created(meta.getCreated().orElse(null))
                 .lastModified(meta.getLastModified().orElse(null))
                 .build();
      if (!meta.isEmpty())
      {
        resource.setMeta(meta);
      }
    }
    return Optional.ofNullable(resource)
                   .map(u -> JsonHelper.copyResourceToObject(u.deepCopy(), AllTypes.class))
                   .orElse(null);
  }

  @Override
  public PartialListResponse<AllTypes> listResources(long startIndex,
                                                     int count,
                                                     FilterNode filter,
                                                     SchemaAttribute sortBy,
                                                     SortOrder sortOrder,
                                                     List<SchemaAttribute> attributes,
                                                     List<SchemaAttribute> excludedAttributes,
                                                     Context context)
  {
    List<AllTypes> resourceNodes = new ArrayList<>(inMemoryMap.values());
    resourceNodes.forEach(user -> {
      Meta meta = user.getMeta().get();
      user.remove(AttributeNames.RFC7643.META);
      user.setMeta(Meta.builder().created(meta.getCreated().get()).lastModified(meta.getLastModified().get()).build());
    });
    return PartialListResponse.<AllTypes> builder().resources(resourceNodes).totalResults(resourceNodes.size()).build();
  }

  @Override
  public AllTypes updateResource(AllTypes resource, Context context)
  {
    Assertions.assertTrue(resource.getMeta().isPresent());
    Meta meta = resource.getMeta().get();
    Assertions.assertTrue(meta.getLocation().isPresent());
    Assertions.assertTrue(meta.getResourceType().isPresent());
    Assertions.assertEquals(ResourceTypeNames.USER, meta.getResourceType().get());
    String resourceId = resource.getId().get();
    AllTypes oldResource = inMemoryMap.get(resourceId);
    if (oldResource == null)
    {
      throw new ResourceNotFoundException("resource with id '" + resourceId + "' does not exist", null, null);
    }
    inMemoryMap.put(resourceId, resource);
    Meta oldMeta = oldResource.getMeta().get();

    oldResource.remove(AttributeNames.RFC7643.META);
    resource.remove(AttributeNames.RFC7643.META);

    Instant lastModified = null;
    if (!oldResource.equals(resource))
    {
      lastModified = Instant.now();
    }
    resource.setMeta(Meta.builder().created(oldMeta.getCreated().get()).lastModified(lastModified).build());
    return resource;
  }

  @Override
  public void deleteResource(String id, Context context)
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
