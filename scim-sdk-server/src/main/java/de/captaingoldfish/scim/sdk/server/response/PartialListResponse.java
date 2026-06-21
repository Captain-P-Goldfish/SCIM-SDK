package de.captaingoldfish.scim.sdk.server.response;

import java.util.List;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 17:50 <br>
 * <br>
 * this type must be returned by
 * {@link ResourceHandler#listResources(long, int, FilterNode, SchemaAttribute, SortOrder, List, List, Authorization)}
 * methods
 */
@Setter
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

  /**
   * Optional cursor value that the client SHOULD pass to retrieve the next page of results. When set, it will
   * be included in the {@link de.captaingoldfish.scim.sdk.common.response.ListResponse} as {@code nextCursor}.
   * MUST be {@code null} for the last page. May be returned for index-based pagination too (RFC 9865 permits
   * combining cursor and index pagination in a single response).
   *
   * @see <a href="https://www.rfc-editor.org/rfc/rfc9865.html">RFC 9865</a>
   */
  private String nextCursor;

  /**
   * Optional cursor value that the client SHOULD pass to retrieve the previous page of results. Returning
   * {@code previousCursor} is OPTIONAL. MUST be {@code null} for the first page.
   *
   * @see <a href="https://www.rfc-editor.org/rfc/rfc9865.html">RFC 9865</a>
   */
  private String previousCursor;

}
