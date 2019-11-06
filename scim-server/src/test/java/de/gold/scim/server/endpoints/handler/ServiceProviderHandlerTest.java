package de.gold.scim.server.endpoints.handler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.gold.scim.common.constants.EndpointPaths;
import de.gold.scim.common.exceptions.NotImplementedException;
import de.gold.scim.common.resources.ServiceProvider;
import de.gold.scim.common.resources.complex.FilterConfig;
import de.gold.scim.common.resources.complex.SortConfig;
import de.gold.scim.server.endpoints.ResourceEndpointHandlerUtil;
import de.gold.scim.server.schemas.ResourceType;
import de.gold.scim.server.schemas.ResourceTypeFactory;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 19.10.2019 - 13:20 <br>
 * <br>
 */
@Slf4j
public class ServiceProviderHandlerTest
{


  /**
   * the resource type factory in which the resources will be registered
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource type handler implementation that was registered
   */
  private ServiceProviderHandler serviceProviderHandler;

  /**
   * the service provider that should be returned from the service provider endpoint
   */
  private ServiceProvider serviceProvider;

  /**
   * initializes the resource endpoint implementation
   */
  @BeforeEach
  public void initialize()
  {
    resourceTypeFactory = new ResourceTypeFactory();
    this.serviceProvider = Mockito.spy(buildServiceProvider());
    // this line is simply used to register the endpoints on the resourceTypeFactory
    ResourceEndpointHandlerUtil.registerAllEndpoints(resourceTypeFactory, serviceProvider);
    final String endpointPath = EndpointPaths.SERVICE_PROVIDER_CONFIG;
    this.serviceProviderHandler = (ServiceProviderHandler)resourceTypeFactory.getResourceType(endpointPath)
                                                                             .getResourceHandlerImpl();
  }

  /**
   * builds a simple {@link ServiceProvider} instance that holds the configuration
   */
  private ServiceProvider buildServiceProvider()
  {
    return ServiceProvider.builder()
                          .filterConfig(FilterConfig.builder().supported(true).maxResults(10).build())
                          .sortConfig(SortConfig.builder().supported(true).build())
                          .build();
  }

  /**
   * verifies that the service provider can successfully be extracted from the {@link ServiceProviderHandler}
   */
  @Test
  public void testGetServiceProvider()
  {
    Assertions.assertEquals(serviceProviderHandler.getResource(null), serviceProvider);
  }

  /**
   * verifies that the feature values can successful be read and changed
   */
  @Test
  public void testValidateDefaultFeatureValues()
  {
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.SERVICE_PROVIDER_CONFIG);
    Assertions.assertFalse(resourceType.getFeatures().isAutoFiltering());
    Assertions.assertTrue(resourceType.getFeatures().isSingletonEndpoint());

    resourceType.getFeatures().setAutoFiltering(true);
    resourceType.getFeatures().setSingletonEndpoint(false);
    Assertions.assertTrue(resourceType.getFeatures().isAutoFiltering());
    Assertions.assertFalse(resourceType.getFeatures().isSingletonEndpoint());
  }

  /**
   * verfies that a {@link NotImplementedException} is thrown if the listResources method is called
   */
  @Test
  public void testListServiceProviders()
  {
    Assertions.assertThrows(NotImplementedException.class,
                            () -> serviceProviderHandler.listResources(0, 0, null, null, null, null, null));
  }

  /**
   * verfies that a {@link NotImplementedException} is thrown if the createResource method is called
   */
  @Test
  public void testCreateProviders()
  {
    Assertions.assertThrows(NotImplementedException.class, () -> serviceProviderHandler.createResource(null));
  }

  /**
   * verfies that a {@link NotImplementedException} is thrown if the updateResource method is called
   */
  @Test
  public void testUpdateProviders()
  {
    Assertions.assertThrows(NotImplementedException.class, () -> serviceProviderHandler.updateResource(null));
  }

  /**
   * verfies that a {@link NotImplementedException} is thrown if the deleteResource method is called
   */
  @Test
  public void testDeleteProviders()
  {
    Assertions.assertThrows(NotImplementedException.class, () -> serviceProviderHandler.deleteResource(null));
  }
}
