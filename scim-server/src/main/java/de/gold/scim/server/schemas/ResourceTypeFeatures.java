package de.gold.scim.server.schemas;

import de.gold.scim.common.constants.AttributeNames;
import de.gold.scim.common.resources.base.ScimObjectNode;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 03.11.2019 - 12:34 <br>
 * <br>
 */
@NoArgsConstructor
public class ResourceTypeFeatures extends ScimObjectNode
{

  @Builder
  public ResourceTypeFeatures(boolean autoFiltering, boolean singletonEndpoint)
  {
    super(null);
    setAutoFiltering(autoFiltering);
    setSingletonEndpoint(singletonEndpoint);
  }

  /**
   * if true it is not possible to access the list-resources endpoint anymore because all get-requests will be
   * delegated to the get-resource endpoint
   */
  public boolean isSingletonEndpoint()
  {
    return getBooleanAttribute(AttributeNames.Custom.SINGLETON_ENDPOINT).orElse(false);
  }

  /**
   * if true it is not possible to access the list-resources endpoint anymore because all get-requests will be
   * delegated to the get-resource endpoint
   */
  public void setSingletonEndpoint(Boolean singletonEndpoint)
  {
    setAttribute(AttributeNames.Custom.SINGLETON_ENDPOINT, singletonEndpoint);
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
