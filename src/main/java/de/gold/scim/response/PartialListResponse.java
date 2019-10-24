package de.gold.scim.response;

import java.util.List;

import de.gold.scim.constants.enums.SortOrder;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.resources.ResourceNode;
import de.gold.scim.schemas.SchemaAttribute;
import lombok.Builder;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 17:50 <br>
 * <br>
 * this type must be returned by
 * {@link de.gold.scim.endpoints.ResourceHandler#listResources(long, int, FilterNode, SchemaAttribute, SortOrder)}
 * methods
 */
@Getter
@Builder
public class PartialListResponse<T extends ResourceNode>
{

  /**
   * the resources that have extracted at the listResources method
   */
  private List<T> resources;

  /**
   * the total number of results the given query request has found. This value should be known by the client
   * since the found number of results might exceed the maximum number of results so the client will be able to
   * use paging mechanisms
   */
  private long totalResults;
}
