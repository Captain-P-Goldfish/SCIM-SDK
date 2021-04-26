package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import java.util.List;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.Getter;
import lombok.Setter;


/**
 * @author Pascal Knueppel
 * @since 26.04.2021
 */
public class SingletonUserHandlerImpl extends ResourceHandler<User>
{

  @Getter
  @Setter
  private User user;

  @Override
  public User createResource(User resource, Authorization authorization)
  {
    this.user = resource;
    return JsonHelper.copyResourceToObject(user, User.class);
  }

  @Override
  public User getResource(String id,
                          Authorization authorization,
                          List<SchemaAttribute> attributes,
                          List<SchemaAttribute> excludedAttributes)
  {
    return JsonHelper.copyResourceToObject(user, User.class);
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
    // not supported on singleton endpoints
    return null;
  }

  @Override
  public User updateResource(User resourceToUpdate, Authorization authorization)
  {
    this.user = resourceToUpdate;
    return JsonHelper.copyResourceToObject(user, User.class);
  }

  @Override
  public void deleteResource(String id, Authorization authorization)
  {
    this.user = null;
  }
}
