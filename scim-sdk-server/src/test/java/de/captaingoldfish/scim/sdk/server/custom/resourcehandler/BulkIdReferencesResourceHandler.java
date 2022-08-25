package de.captaingoldfish.scim.sdk.server.custom.resourcehandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.custom.resources.BulkIdReferences;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;


/**
 *
 */
public class BulkIdReferencesResourceHandler extends ResourceHandler<BulkIdReferences>
{

  private static final Map<String, BulkIdReferences> BULK_ID_REFERENCES_MAP = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public BulkIdReferences createResource(BulkIdReferences resource, Context context)
  {
    resource.setId(UUID.randomUUID().toString());
    BULK_ID_REFERENCES_MAP.put(resource.getId().get(), resource);
    resource.getMeta().ifPresent(meta -> {
      meta.setCreated(Instant.now());
      meta.setLastModified(meta.getCreated().get());
    });
    return resource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BulkIdReferences getResource(String id,
                                      List<SchemaAttribute> attributes,
                                      List<SchemaAttribute> excludedAttributes,
                                      Context context)
  {
    return BULK_ID_REFERENCES_MAP.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PartialListResponse<BulkIdReferences> listResources(long startIndex,
                                                             int count,
                                                             FilterNode filter,
                                                             SchemaAttribute sortBy,
                                                             SortOrder sortOrder,
                                                             List<SchemaAttribute> attributes,
                                                             List<SchemaAttribute> excludedAttributes,
                                                             Context context)
  {
    List<BulkIdReferences> references = new ArrayList<>(BULK_ID_REFERENCES_MAP.values());
    return PartialListResponse.<BulkIdReferences> builder()
                              .resources(references)
                              .totalResults(references.size())
                              .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BulkIdReferences updateResource(BulkIdReferences resource, Context context)
  {
    BULK_ID_REFERENCES_MAP.put(resource.getId().get(), resource);
    resource.getMeta().ifPresent(meta -> meta.setLastModified(Instant.now()));
    return resource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(String id, Context context)
  {
    BULK_ID_REFERENCES_MAP.remove(id);
  }
}
