package de.captaingoldfish.scim.sdk.client.setup.scim.handler;

import java.time.Instant;
import java.util.List;

import de.captaingoldfish.scim.sdk.client.setup.scim.resources.ScimTestSingleton;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;


/**
 * @author Pascal Knueppel
 * @since 25.02.2022
 */
public class TestSingletonHandler extends ResourceHandler<ScimTestSingleton>
{

  public static final String ENDPOINT = "/TestSingleton";

  private static final String ID = "1";

  public static ScimTestSingleton scimTestSingleton;

  @Override
  public ScimTestSingleton createResource(ScimTestSingleton resource, Context context)
  {
    scimTestSingleton = resource;
    scimTestSingleton.setId(ID);
    scimTestSingleton.setMeta(Meta.builder()
                                  .created(Instant.now())
                                  .lastModified(Instant.now())
                                  .location(context.getResourceReferenceUrl(null))
                                  .build());
    return scimTestSingleton;
  }

  @Override
  public ScimTestSingleton getResource(String id,
                                       List<SchemaAttribute> attributes,
                                       List<SchemaAttribute> excludedAttributes,
                                       Context context)
  {
    return scimTestSingleton;
  }

  /**
   * disabled by resource-type definition
   */
  @Override
  public PartialListResponse<ScimTestSingleton> listResources(long startIndex,
                                                              int count,
                                                              FilterNode filter,
                                                              SchemaAttribute sortBy,
                                                              SortOrder sortOrder,
                                                              List<SchemaAttribute> attributes,
                                                              List<SchemaAttribute> excludedAttributes,
                                                              Context context)
  {
    return null;
  }

  @Override
  public ScimTestSingleton updateResource(ScimTestSingleton resourceToUpdate, Context context)
  {
    scimTestSingleton = resourceToUpdate;
    scimTestSingleton.setId(ID);
    scimTestSingleton.getMeta().get().setLastModified(Instant.now());
    return scimTestSingleton;
  }

  @Override
  public void deleteResource(String id, Context context)
  {
    scimTestSingleton = null;
  }
}
