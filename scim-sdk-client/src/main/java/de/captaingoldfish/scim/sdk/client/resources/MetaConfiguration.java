package de.captaingoldfish.scim.sdk.client.resources;

import java.util.List;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import lombok.Builder;


/**
 * @author Pascal Knueppel
 * @since 14.10.2023
 */
public class MetaConfiguration extends ScimObjectNode
{

  @Builder
  public MetaConfiguration(ServiceProvider serviceProvider, List<ResourceType> resourceTypes, List<Schema> schemas)
  {
    setServiceProvider(serviceProvider);
    setResourceTypes(resourceTypes);
    setSchemas(schemas);
  }

  /**
   * the ServiceProvider config of a SCIM provider
   */
  public ServiceProvider getServiceProvider()
  {
    return getObjectAttribute(AttributeNames.Custom.SERVICE_PROVIDER, ServiceProvider.class).orElse(null);
  }

  /**
   * the ServiceProvider config of a SCIM provider
   */
  public void setServiceProvider(ServiceProvider serviceProvider)
  {
    setAttribute(AttributeNames.Custom.SERVICE_PROVIDER, serviceProvider);
  }

  /**
   * The resourceTypes of a SCIM provider that define the supported endpoints at the provider
   */
  public List<ResourceType> getResourceTypes()
  {
    return getArrayAttribute(AttributeNames.Custom.RESOURCE_TYPES, ResourceType.class);
  }

  /**
   * The resourceTypes of a SCIM provider that define the supported endpoints at the provider
   */
  public void setResourceTypes(List<ResourceType> resourceTypes)
  {
    setAttribute(AttributeNames.Custom.RESOURCE_TYPES, resourceTypes);
  }

  /**
   * The schemas of a SCIM provider that define the supported resources at the provider
   */
  public List<Schema> getSchemas()
  {
    return getArrayAttribute(AttributeNames.RFC7643.SCHEMAS, Schema.class);
  }

  /**
   * The schemas of a SCIM provider that define the supported schemas at the provider
   */
  public void setSchemas(List<Schema> schemas)
  {
    setAttribute(AttributeNames.RFC7643.SCHEMAS, schemas);
  }

}
