package de.captaingoldfish.scim.sdk.server.schemas.custom;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 26.11.2019 - 08:50 <br>
 * <br>
 * this feature extension will allow to disable specific endpoint for specific resource types
 */
@NoArgsConstructor
public class EndpointControlFeature extends ScimObjectNode
{

  @Builder
  public EndpointControlFeature(Boolean createDisabled,
                                Boolean getDisabled,
                                Boolean listDisabled,
                                Boolean updateDisabled,
                                Boolean deleteDisabled)
  {
    this();
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
  }

  /**
   * check if all methods are disabled
   */
  public boolean isResourceTypeDisabled()
  {
    if (isCreateDisabled() && isGetDisabled() && isListDisabled() && isUpdateDisabled() && isDeleteDisabled())
    {
      return true;
    }
    return false;
  }
}
