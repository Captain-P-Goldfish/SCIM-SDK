package de.captaingoldfish.scim.sdk.translator.utils;

import java.util.List;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;


/**
 * @author Pascal Knueppel
 * @since 28.01.2022
 */
public class BasicResourceHandler extends ResourceHandler<ResourceNode>
{

  @Override
  public ResourceNode createResource(ResourceNode resource, Context context)
  {
    return null;
  }

  @Override
  public ResourceNode getResource(String id,
                                  List<SchemaAttribute> attributes,
                                  List<SchemaAttribute> excludedAttributes,
                                  Context context)
  {
    return null;
  }

  @Override
  public PartialListResponse<ResourceNode> listResources(long startIndex,
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
  public ResourceNode updateResource(ResourceNode resourceToUpdate, Context context)
  {
    return null;
  }

  @Override
  public void deleteResource(String id, Context context)
  {

  }
}
