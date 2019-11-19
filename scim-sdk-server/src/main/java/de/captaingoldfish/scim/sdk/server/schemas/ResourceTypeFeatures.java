package de.captaingoldfish.scim.sdk.server.schemas;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
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
  public ResourceTypeFeatures(boolean autoFiltering, boolean autoSorting, boolean singletonEndpoint)
  {
    super(null);
    setAutoFiltering(autoFiltering);
    setAutoSorting(autoSorting);
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
   * enables application side filtering. The developer will no longer be able to do the filtering manually if
   * this has been enabled because the developer will be cut off of the filtering information
   */
  public boolean isAutoFiltering()
  {
    return getBooleanAttribute(AttributeNames.Custom.AUTO_FILTERING).orElse(false);
  }

  /**
   * enables application side filtering. The developer will no longer be able to do the filtering manually if
   * this has been enabled because the developer will be cut off of the filtering information
   */
  public void setAutoFiltering(Boolean autoFiltering)
  {
    setAttribute(AttributeNames.Custom.AUTO_FILTERING, autoFiltering);
  }

  /**
   * enables application side sorting. The developer will no longer be able to do the sorting manually if this
   * has been enabled because the developer will be cut off of the sorting information
   */
  public boolean isAutoSorting()
  {
    return getBooleanAttribute(AttributeNames.Custom.AUTO_SORTING).orElse(false);
  }

  /**
   * enables application side sorting. The developer will no longer be able to do the sorting manually if this
   * has been enabled because the developer will be cut off of the sorting information
   */
  public void setAutoSorting(Boolean autoSorting)
  {
    setAttribute(AttributeNames.Custom.AUTO_SORTING, autoSorting);
  }
}
