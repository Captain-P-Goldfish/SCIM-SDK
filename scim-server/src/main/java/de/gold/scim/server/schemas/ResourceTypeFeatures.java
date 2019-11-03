package de.gold.scim.server.schemas;

import de.gold.scim.common.constants.AttributeNames;
import de.gold.scim.common.resources.base.ScimObjectNode;


/**
 * author Pascal Knueppel <br>
 * created at: 03.11.2019 - 12:34 <br>
 * <br>
 */
public class ResourceTypeFeatures extends ScimObjectNode
{

  public ResourceTypeFeatures(boolean autoFiltering)
  {
    super(null);
    setAutoFiltering(autoFiltering);
  }

  /**
   * enabled application side filtering. The developer will no longer be able to do the filtering himself if
   * this has been enabled
   */
  public boolean isAutoFiltering()
  {
    return getBooleanAttribute(AttributeNames.Custom.AUTO_FILTERING).orElse(false);
  }

  /**
   * enabled application side filtering. The developer will no longer be able to do the filtering himself if
   * this has been enabled
   */
  public void setAutoFiltering(Boolean autoFiltering)
  {
    setAttribute(AttributeNames.Custom.AUTO_FILTERING, autoFiltering);
  }
}
