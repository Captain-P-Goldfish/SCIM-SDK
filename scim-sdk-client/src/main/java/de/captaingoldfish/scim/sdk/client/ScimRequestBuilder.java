package de.captaingoldfish.scim.sdk.client;

import de.captaingoldfish.scim.sdk.client.builder.BulkBuilder;
import de.captaingoldfish.scim.sdk.client.builder.CreateBuilder;
import de.captaingoldfish.scim.sdk.client.builder.DeleteBuilder;
import de.captaingoldfish.scim.sdk.client.builder.GetBuilder;
import de.captaingoldfish.scim.sdk.client.builder.ListBuilder;
import de.captaingoldfish.scim.sdk.client.builder.MetaConfigLoaderBuilder;
import de.captaingoldfish.scim.sdk.client.builder.config.MetaConfigRequestDetails;
import de.captaingoldfish.scim.sdk.client.builder.PatchBuilder;
import de.captaingoldfish.scim.sdk.client.builder.UpdateBuilder;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 07.12.2019 - 23:08 <br>
 * <br>
 * this class can be used to build any type of request for SCIM
 */
@Slf4j
public class ScimRequestBuilder implements AutoCloseable
{

  /**
   * must contain the baseUrl to the scim service
   */
  @Getter
  private final String baseUrl;

  /**
   * the configuration for the client that should be used
   */
  @Getter
  private final ScimClientConfig scimClientConfig;

  /**
   * a convenience implementation that wraps the apache http client
   */
  @Getter(AccessLevel.PROTECTED) // for unit tests
  private ScimHttpClient scimHttpClient;

  /**
   * the service provider configuration from the SCIM provider application
   */
  @Getter
  @Setter
  private ServiceProvider serviceProvider;

  public ScimRequestBuilder(String baseUrl, ScimClientConfig scimClientConfig)
  {
    this.baseUrl = baseUrl.replaceFirst("/$", "");
    this.scimClientConfig = scimClientConfig;
    this.scimHttpClient = new ScimHttpClient(scimClientConfig);
  }

  /**
   * tries to load the service provider configuration from the SCIM provider, but it will not cause any aborts
   * if loading of the configuration does fail
   */
  public ServiceProvider loadServiceProviderConfiguration()
  {
    if (serviceProvider != null)
    {
      return serviceProvider;
    }
    try
    {
      log.info("Trying to load service provider configuration from SCIM provider");
      ServerResponse<ServiceProvider> response = this.get(ServiceProvider.class, EndpointPaths.SERVICE_PROVIDER_CONFIG)
                                                     .sendRequest();
      boolean isError = !response.isSuccess();
      if (isError)
      {
        log.warn("Failed to load service provider configuration: status: '{}' responseBody: '{}'",
                 response.getHttpStatus(),
                 response.getResponseBody());
      }
      serviceProvider = response.getResource();
      return serviceProvider;
    }
    catch (Exception ex)
    {
      log.warn("Failed to load service provider configuration from SCIM provider", ex);
      return null;
    }
  }

  /**
   * builds a create builder class based on the given type
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @return a create-request builder for the given resource type
   */
  public <T extends ResourceNode> CreateBuilder<T> create(Class<T> type, String endpoint)
  {
    return new CreateBuilder<>(baseUrl, endpoint, type, scimHttpClient);
  }

  /**
   * builds a create builder class based on the given type
   *
   * @param fullyQualifiedUrl if the builder should not build the url on the baseUrl but use another fully
   *          qualified url
   * @param type the type that should be created
   * @return a create-request builder for the given resource type
   */
  public <T extends ResourceNode> CreateBuilder<T> create(String fullyQualifiedUrl, Class<T> type)
  {
    return new CreateBuilder<>(fullyQualifiedUrl, type, scimHttpClient);
  }

  /**
   * builds a get builder class based on the given type
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @param resourceId the id of the resource that should be returned (may be null if the endpoint path already
   *          contains the id)
   * @return a get-request builder for the given resource type
   */
  public <T extends ResourceNode> GetBuilder<T> get(Class<T> type, String endpoint, String resourceId)
  {
    return new GetBuilder<>(baseUrl, endpoint, resourceId, type, scimHttpClient);
  }

  /**
   * builds a get builder that is used to access a singleton resource from the provider
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @return a get-request builder for the given resource type
   */
  public <T extends ResourceNode> GetBuilder<T> get(Class<T> type, String endpoint)
  {
    return new GetBuilder<>(baseUrl, endpoint, null, type, scimHttpClient);
  }

  /**
   * builds a get builder class based on the given type
   *
   * @param fullyQualifiedUrl if the builder should not build the url on the baseUrl but use another fully
   *          qualified url
   * @param type the type that should be created
   * @return a get-request builder for the given resource type
   */
  public <T extends ResourceNode> GetBuilder<T> get(String fullyQualifiedUrl, Class<T> type)
  {
    return new GetBuilder<>(fullyQualifiedUrl, type, scimHttpClient);
  }

  /**
   * builds a delete builder class based on the given type
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @param resourceId the id of the resource that should be returned (may be null if the endpoint path already
   *          contains the id)
   * @return a delete-request builder for the given resource type
   */
  public <T extends ResourceNode> DeleteBuilder<T> delete(Class<T> type, String endpoint, String resourceId)
  {
    return new DeleteBuilder<>(baseUrl, endpoint, resourceId, type, scimHttpClient);
  }

  /**
   * builds a delete builder class based on the given type used to delete a singleton entry at the provider
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @return a delete-request builder for the given resource type
   */
  public <T extends ResourceNode> DeleteBuilder<T> delete(Class<T> type, String endpoint)
  {
    return new DeleteBuilder<>(baseUrl, endpoint, null, type, scimHttpClient);
  }

  /**
   * builds a delete builder class based on the given type
   *
   * @param fullyQualifiedUrl if the builder should not build the url on the baseUrl but use another fully
   *          qualified url
   * @param type the type that should be created
   * @return a delete-request builder for the given resource type
   */
  public <T extends ResourceNode> DeleteBuilder<T> delete(String fullyQualifiedUrl, Class<T> type)
  {
    return new DeleteBuilder<>(fullyQualifiedUrl, type, scimHttpClient);
  }

  /**
   * builds an update builder class based on the given type
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @param resourceId the id of the resource that should be returned (may be null if the endpoint path already
   *          contains the id)
   * @return a update-request builder for the given resource type
   */
  public <T extends ResourceNode> UpdateBuilder<T> update(Class<T> type, String endpoint, String resourceId)
  {
    return new UpdateBuilder<>(baseUrl, endpoint, resourceId, type, scimHttpClient);
  }

  /**
   * builds an update builder class used to update a singleton entry at the provider
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @return a update-request builder for the given resource type
   */
  public <T extends ResourceNode> UpdateBuilder<T> update(Class<T> type, String endpoint)
  {
    return new UpdateBuilder<>(baseUrl, endpoint, null, type, scimHttpClient);
  }

  /**
   * builds an update builder class based on the given type
   *
   * @param fullyQualifiedUrl if the builder should not build the url on the baseUrl but use another fully
   *          qualified url
   * @param type the type that should be created
   * @return a update-request builder for the given resource type
   */
  public <T extends ResourceNode> UpdateBuilder<T> update(String fullyQualifiedUrl, Class<T> type)
  {
    return new UpdateBuilder<>(fullyQualifiedUrl, type, scimHttpClient);
  }

  /**
   * builds an update builder class based on the given type
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @return a update-request builder for the given resource type
   */
  public <T extends ResourceNode> ListBuilder<T> list(Class<T> type, String endpoint)
  {
    return new ListBuilder<>(baseUrl, endpoint, type, scimHttpClient);
  }

  /**
   * builds an update builder class based on the given type
   *
   * @param fullyQualifiedUrl if the builder should not build the url on the baseUrl but use another fully
   *          qualified url
   * @param type the type that should be created
   * @return a update-request builder for the given resource type
   */
  public <T extends ResourceNode> ListBuilder<T> list(String fullyQualifiedUrl, Class<T> type)
  {
    return new ListBuilder<>(fullyQualifiedUrl, type, scimHttpClient);
  }

  /**
   * builds an bulk request builder
   *
   * @return a bulk-request builder
   */
  public BulkBuilder bulk()
  {
    return new BulkBuilder(baseUrl, scimHttpClient, false, this::loadServiceProviderConfiguration);
  }

  /**
   * builds an bulk request builder
   *
   * @param fullyQualifiedUrl if the builder should not build the url on the baseUrl but use another fully
   *          qualified url
   * @return a bulk-request builder
   */
  public BulkBuilder bulk(String fullyQualifiedUrl)
  {
    return new BulkBuilder(fullyQualifiedUrl, scimHttpClient, true, this::loadServiceProviderConfiguration);
  }

  /**
   * builds a patch request builder
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @param resourceId the id of the resource that should be returned (may be null if the endpoint path already
   *          contains the id)
   * @return a patch-request builder
   */
  public <T extends ResourceNode> PatchBuilder<T> patch(Class<T> type, String endpoint, String resourceId)
  {
    return new PatchBuilder<>(baseUrl, endpoint, resourceId, type, scimHttpClient);
  }

  /**
   * builds a patch request builder used to patch a singleton entry at the provider
   *
   * @param type the type that should be created
   * @param endpoint the endpoint path to the resource e.g. "/Users" or "/Groups"
   * @return a patch-request builder
   */
  public <T extends ResourceNode> PatchBuilder<T> patch(Class<T> type, String endpoint)
  {
    return new PatchBuilder<>(baseUrl, endpoint, null, type, scimHttpClient);
  }

  /**
   * builds a patch request builder
   *
   * @param fullyQualifiedUrl if the builder should not build the url on the baseUrl but use another fully
   *          qualified url
   * @param type the type that should be created
   * @return a patch-request builder
   */
  public <T extends ResourceNode> PatchBuilder<T> patch(String fullyQualifiedUrl, Class<T> type)
  {
    return new PatchBuilder<>(fullyQualifiedUrl, type, scimHttpClient);
  }

  /**
   * will try to load the complete data of the meta-endpoints from the configured SCIM provider
   *
   * @return the meta configuration details from the SCIM provider
   */
  public MetaConfigLoaderBuilder loadMetaConfiguration()
  {
    return new MetaConfigLoaderBuilder(baseUrl, scimHttpClient, new MetaConfigRequestDetails());
  }

  /**
   * will try to load the complete data of the meta-endpoints from the configured SCIM provider
   *
   * @return the meta configuration details from the SCIM provider
   */
  public MetaConfigLoaderBuilder loadMetaConfiguration(MetaConfigRequestDetails metaConfigLoaderDetails)
  {
    return new MetaConfigLoaderBuilder(baseUrl, scimHttpClient, metaConfigLoaderDetails);
  }

  /**
   * closes the underlying apache http client. If the http client is closed this request builder is still
   * usable. The next request will simply be executed with a new http client instance
   */
  @Override
  public void close()
  {
    scimHttpClient.close();
  }
}
