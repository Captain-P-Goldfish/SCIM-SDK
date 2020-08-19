package de.captaingoldfish.scim.sdk.client;

import de.captaingoldfish.scim.sdk.client.builder.BulkBuilder;
import de.captaingoldfish.scim.sdk.client.builder.CreateBuilder;
import de.captaingoldfish.scim.sdk.client.builder.DeleteBuilder;
import de.captaingoldfish.scim.sdk.client.builder.GetBuilder;
import de.captaingoldfish.scim.sdk.client.builder.ListBuilder;
import de.captaingoldfish.scim.sdk.client.builder.PatchBuilder;
import de.captaingoldfish.scim.sdk.client.builder.UpdateBuilder;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 07.12.2019 - 23:08 <br>
 * <br>
 * this class can be used to build any type of request for SCIM
 */
public class ScimRequestBuilder implements AutoCloseable
{

  /**
   * must contain the baseUrl to the scim service
   */
  private final String baseUrl;

  /**
   * the configuration for the client that should be used
   */
  @Getter
  private final ScimClientConfig scimClientConfig;

  /**
   * a convenience implementation that wraps the apache http client
   */
  private ScimHttpClient scimHttpClient;

  public ScimRequestBuilder(String baseUrl, ScimClientConfig scimClientConfig)
  {
    this.baseUrl = baseUrl.replaceFirst("/$", "");
    this.scimClientConfig = scimClientConfig;
    this.scimHttpClient = new ScimHttpClient(scimClientConfig);
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
   * builds an bulk request builder
   *
   * @return a bulk-request builder
   */
  public BulkBuilder bulk()
  {
    return new BulkBuilder(baseUrl, scimHttpClient);
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
   * closes the underlying apache http client. If the http client is closed this request builder is still
   * usable. The next request will simply be executed with a new http client instance
   */
  @Override
  public void close()
  {
    scimHttpClient.close();
  }
}
