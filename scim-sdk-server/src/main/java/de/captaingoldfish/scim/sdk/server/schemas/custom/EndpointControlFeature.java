package de.captaingoldfish.scim.sdk.server.schemas.custom;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.server.endpoints.features.EndpointType;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 26.11.2019 - 08:50 <br>
 * <br>
 * this feature extension will allow to disable specific endpoint for specific resource types
 */
public class EndpointControlFeature extends ScimObjectNode
{

  @Builder
  public EndpointControlFeature(Boolean createDisabled,
                                Boolean getDisabled,
                                Boolean listDisabled,
                                Boolean updateDisabled,
                                Boolean deleteDisabled)
  {
    setCreateDisabled(createDisabled);
    setGetDisabled(getDisabled);
    setListDisabled(listDisabled);
    setUpdateDisabled(updateDisabled);
    setDeleteDisabled(deleteDisabled);
  }

  /**
   * if the create endpoint is disabled or not
   */
  public boolean isCreateDisabled()
  {
    return getBooleanAttribute(AttributeNames.Custom.DISABLE_CREATE).orElse(false);
  }

  /**
   * if the create endpoint is disabled or not
   */
  public void setCreateDisabled(Boolean createDisabled)
  {
    setAttribute(AttributeNames.Custom.DISABLE_CREATE, createDisabled);
    validateNotAllEndpointsAreDisabled(EndpointType.CREATE);
  }

  /**
   * if the get endpoint is disabled or not
   */
  public boolean isGetDisabled()
  {
    return getBooleanAttribute(AttributeNames.Custom.DISABLE_GET).orElse(false);
  }

  /**
   * if the get endpoint is disabled or not
   */
  public void setGetDisabled(Boolean getDisabled)
  {
    setAttribute(AttributeNames.Custom.DISABLE_GET, getDisabled);
    validateNotAllEndpointsAreDisabled(EndpointType.GET);
  }

  /**
   * if the list endpoint is disabled or not
   */
  public boolean isListDisabled()
  {
    return getBooleanAttribute(AttributeNames.Custom.DISABLE_LIST).orElse(false);
  }

  /**
   * if the list endpoint is disabled or not
   */
  public void setListDisabled(Boolean listDisabled)
  {
    setAttribute(AttributeNames.Custom.DISABLE_LIST, listDisabled);
    validateNotAllEndpointsAreDisabled(EndpointType.LIST);
  }

  /**
   * if the update endpoint is disabled or not
   */
  public boolean isUpdateDisabled()
  {
    return getBooleanAttribute(AttributeNames.Custom.DISABLE_UPDATE).orElse(false);
  }

  /**
   * if the update endpoint is disabled or not
   */
  public void setUpdateDisabled(Boolean disableUpdate)
  {
    setAttribute(AttributeNames.Custom.DISABLE_UPDATE, disableUpdate);
    validateNotAllEndpointsAreDisabled(EndpointType.UPDATE);
  }

  /**
   * if the delete endpoint is disabled or not
   */
  public boolean isDeleteDisabled()
  {
    return getBooleanAttribute(AttributeNames.Custom.DISABLE_DELETE).orElse(false);
  }

  /**
   * if the delete endpoint is disabled or not
   */
  public void setDeleteDisabled(Boolean disableDelete)
  {
    setAttribute(AttributeNames.Custom.DISABLE_DELETE, disableDelete);
    validateNotAllEndpointsAreDisabled(EndpointType.DELETE);
  }

  /**
   * this method will assert that not all endpoints are disabled
   */
  private void validateNotAllEndpointsAreDisabled(EndpointType endpointType)
  {
    if (isCreateDisabled() && isGetDisabled() && isListDisabled() && isUpdateDisabled() && isDeleteDisabled())
    {
      switch (endpointType)
      {
        case CREATE:
          setCreateDisabled(false);
          break;
        case GET:
          setGetDisabled(false);
          break;
        case LIST:
          setListDisabled(false);
          break;
        case UPDATE:
          setUpdateDisabled(false);
          break;
        case DELETE:
          setDeleteDisabled(false);
          break;
      }
      throw new InternalServerException("do not disable all endpoints. Disable the resource type itself instead", null,
                                        null);
    }
  }
}
