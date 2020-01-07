package de.captaingoldfish.scim.sdk.server.schemas.custom;

import java.util.Optional;

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
  public ResourceTypeFeatures(boolean autoFiltering,
                              boolean autoSorting,
                              boolean singletonEndpoint,
                              EndpointControlFeature endpointControlFeature)
  {
    super(null);
    setAutoFiltering(autoFiltering);
    setAutoSorting(autoSorting);
    setSingletonEndpoint(singletonEndpoint);
    setEndpointControlFeature(endpointControlFeature);
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

  /**
   * if the current resource type is disabled
   */
  public boolean isResourceTypeDisabled()
  {
    return getBooleanAttribute(AttributeNames.Custom.RESOURCE_TYPE_DISABLED).orElse(false)
           || getEndpointControlFeature().isResourceTypeDisabled();
  }

  /**
   * disables or enables the current resource type
   */
  public void setResourceTypeDisabled(Boolean disabled)
  {
    setAttribute(AttributeNames.Custom.RESOURCE_TYPE_DISABLED, disabled);
  }

  /**
   * @return the current values of the endpoint control feature
   */
  public EndpointControlFeature getEndpointControlFeature()
  {
    Optional<EndpointControlFeature> endpointControl = getObjectAttribute(AttributeNames.Custom.ENDPOINT_CONTROL,
                                                                          EndpointControlFeature.class);
    if (endpointControl.isPresent())
    {
      return endpointControl.get();
    }
    else
    {
      EndpointControlFeature endpointControlFeature = EndpointControlFeature.builder().build();
      setEndpointControlFeature(endpointControlFeature);
      return endpointControlFeature;
    }
  }

  /**
   * replaces the whole endpoint control feature node
   */
  public void setEndpointControlFeature(EndpointControlFeature endpointControlFeature)
  {
    setAttribute(AttributeNames.Custom.ENDPOINT_CONTROL, endpointControlFeature);
  }


  /**
   * a complex attribute that tells us the which roles the client must have to access the given endpoin
   */
  public ResourceTypeAuthorization getAuthorization()
  {
    Optional<ResourceTypeAuthorization> authorization = getObjectAttribute(AttributeNames.Custom.AUTHORIZATION,
                                                                           ResourceTypeAuthorization.class);
    if (authorization.isPresent())
    {
      return authorization.get();
    }
    else
    {
      ResourceTypeAuthorization resourceTypeAuthorization = ResourceTypeAuthorization.builder().build();
      setAuthorization(resourceTypeAuthorization);
      return resourceTypeAuthorization;
    }
  }

  /**
   * a complex attribute that tells us the which roles the client must have to access the given endpoin
   */
  public void setAuthorization(ResourceTypeAuthorization resourceTypeAuthorization)
  {
    setAttribute(AttributeNames.Custom.AUTHORIZATION, resourceTypeAuthorization);
  }


}
