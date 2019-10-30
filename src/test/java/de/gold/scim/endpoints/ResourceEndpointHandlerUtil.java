package de.gold.scim.endpoints;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.endpoints.base.GroupEndpointDefinition;
import de.gold.scim.endpoints.base.MeEndpointDefinition;
import de.gold.scim.endpoints.base.ResourceTypeEndpointDefinition;
import de.gold.scim.endpoints.base.SchemaEndpointDefinition;
import de.gold.scim.endpoints.base.ServiceProviderEndpointDefinition;
import de.gold.scim.endpoints.base.UserEndpointDefinition;
import de.gold.scim.endpoints.handler.GroupHandlerImpl;
import de.gold.scim.endpoints.handler.UserHandlerImpl;
import de.gold.scim.resources.ServiceProvider;
import de.gold.scim.schemas.ResourceTypeFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 00:01 <br>
 * <br>
 * a helper class to create a {@link ResourceEndpointHandler} with a custom resource type factory for any unit
 * test
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceEndpointHandlerUtil
{

  /**
   * registers all endpoints within the given resourceType
   */
  public static void registerAllEndpoints(ResourceTypeFactory resourceTypeFactory)
  {
    ServiceProvider serviceProvider = ServiceProvider.builder().build();
    registerAllEndpoints(resourceTypeFactory, serviceProvider);
  }

  /**
   * registers all endpoints within the given resourceType
   */
  public static void registerAllEndpoints(ResourceTypeFactory resourceTypeFactory, ServiceProvider serviceProvider)
  {
    UserEndpointDefinition userEndpoint = new UserEndpointDefinition(new UserHandlerImpl());
    GroupEndpointDefinition groupEndpoint = new GroupEndpointDefinition(new GroupHandlerImpl());
    MeEndpointDefinition meEndpoint = new MeEndpointDefinition(new UserHandlerImpl());
    ResourceTypeEndpointDefinition resourceTypeEndpoint = new ResourceTypeEndpointDefinition(resourceTypeFactory);
    SchemaEndpointDefinition schemaEndpoint = new SchemaEndpointDefinition(resourceTypeFactory);
    ServiceProviderEndpointDefinition serviceProviderEndpoint = new ServiceProviderEndpointDefinition(serviceProvider);
    List<EndpointDefinition> endpointDefinitionList = Arrays.asList(userEndpoint,
                                                                    groupEndpoint,
                                                                    meEndpoint,
                                                                    resourceTypeEndpoint,
                                                                    serviceProviderEndpoint,
                                                                    schemaEndpoint);
    for ( EndpointDefinition endpointDefinition : endpointDefinitionList )
    {
      resourceTypeFactory.registerResourceType(endpointDefinition.getResourceHandler(),
                                               endpointDefinition.getResourceType(),
                                               endpointDefinition.getResourceSchema(),
                                               endpointDefinition.getResourceSchemaExtensions()
                                                                 .toArray(new JsonNode[0]));
    }
  }
}
