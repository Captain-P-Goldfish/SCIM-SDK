package de.captaingoldfish.scim.sdk.client.builder;

import java.util.List;
import java.util.Optional;

import org.apache.http.client.methods.HttpUriRequest;

import de.captaingoldfish.scim.sdk.client.builder.ListBuilder.FilterBuilder;
import de.captaingoldfish.scim.sdk.client.builder.config.MetaConfigRequestDetails;
import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.resources.MetaConfiguration;
import de.captaingoldfish.scim.sdk.client.resources.ResourceType;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.Comparator;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;


/**
 * this class can be used to load the meta-configuration from the ServiceProvider
 *
 * @author Pascal Knueppel
 * @since 14.10.2023
 */
public class MetaConfigLoaderBuilder extends RequestBuilder<MetaConfiguration>
{

  /**
   * the builder to retrieve the ServiceProviderConfig
   */
  private final GetBuilder<ServiceProvider> serviceProviderLoaderBuilder;

  /**
   * the builder to retrieve the resourceTypes
   */
  private final ListBuilder<ResourceType> resourceTypeLoaderBuilder;

  /**
   * the builder to retrieve the schemas
   */
  private final ListBuilder<Schema> schemaLoaderBuilder;

  /**
   * the request configuration details that tell us what should be retrieved and how from the provider
   */
  private final MetaConfigRequestDetails metaConfigLoaderDetails;

  public MetaConfigLoaderBuilder(String baseUrl,
                                 ScimHttpClient scimHttpClient,
                                 MetaConfigRequestDetails metaConfigLoaderDetails)
  {
    super(null, null, null, null);

    this.serviceProviderLoaderBuilder = new GetBuilder<>(baseUrl, metaConfigLoaderDetails.getServiceProviderEndpoint(),
                                                         null, ServiceProvider.class, scimHttpClient);
    this.resourceTypeLoaderBuilder = new ListBuilder<>(baseUrl, metaConfigLoaderDetails.getResourceTypeEndpoint(),
                                                       ResourceType.class, scimHttpClient);
    this.schemaLoaderBuilder = new ListBuilder<>(baseUrl, metaConfigLoaderDetails.getSchemasEndpoint(), Schema.class,
                                                 scimHttpClient);
    this.metaConfigLoaderDetails = metaConfigLoaderDetails;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isExpectedResponseCode(int httpStatus)
  {
    return HttpStatus.OK == httpStatus;
  }

  /**
   * never called
   */
  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    return null; // never called
  }

  /**
   * loads the details from the meta-endpoints and gathers these details in a single object
   *
   * @return the {@link MetaConfiguration} from the called SCIM provider
   */
  @Override
  public ServerResponse<MetaConfiguration> sendRequest()
  {
    ServerResponse<ServiceProvider> serviceProviderResponse = serviceProviderLoaderBuilder.sendRequest();
    ServerResponse<ListResponse<ResourceType>> resourceTypeResponse = loadResourceTypes(serviceProviderResponse);
    ServerResponse<ListResponse<Schema>> schemasResponse = loadSchemas(serviceProviderResponse);

    HttpResponse.HttpResponseBuilder httpResponseBuilder = HttpResponse.builder();
    final int statusCode;
    getStatusCode:
    {
      if (!serviceProviderResponse.isSuccess())
      {
        statusCode = serviceProviderResponse.getHttpStatus();
        httpResponseBuilder.responseBody(serviceProviderResponse.getResponseBody());
        break getStatusCode;
      }
      if (!resourceTypeResponse.isSuccess())
      {
        statusCode = resourceTypeResponse.getHttpStatus();
        httpResponseBuilder.responseBody(resourceTypeResponse.getResponseBody());
        break getStatusCode;
      }
      if (!schemasResponse.isSuccess())
      {
        statusCode = schemasResponse.getHttpStatus();
        httpResponseBuilder.responseBody(schemasResponse.getResponseBody());
        break getStatusCode;
      }
      statusCode = HttpStatus.OK;
    }
    httpResponseBuilder.httpStatusCode(statusCode);

    boolean isExpectedResponseCode = isExpectedResponseCode(statusCode);

    final List<Schema> schemaList;
    if (metaConfigLoaderDetails.isExcludeMetaSchemas() && schemasResponse.isSuccess())
    {
      ListResponse<Schema> schemaListResponse = schemasResponse.getResource();
      schemaList = schemaListResponse.getListedResources();
      schemaList.removeIf(schema -> metaConfigLoaderDetails.getMetaSchemaUris().contains(schema.getNonNullId()));
    }
    else
    {
      schemaList = schemasResponse.isSuccess() ? schemasResponse.getResource().getListedResources() : null;
    }

    final List<ResourceType> resourceTypeList;
    if (metaConfigLoaderDetails.isExcludeMetaResourceTypes() && resourceTypeResponse.isSuccess())
    {
      ListResponse<ResourceType> resourceTypeListResponse = resourceTypeResponse.getResource();
      resourceTypeList = resourceTypeListResponse.getListedResources();
      resourceTypeList.removeIf(resourceType -> metaConfigLoaderDetails.getMetaResourceTypeNames()
                                                                       .contains(resourceType.getName()));
    }
    else
    {
      resourceTypeList = resourceTypeResponse.isSuccess() ? resourceTypeResponse.getResource().getListedResources()
        : null;
    }

    MetaConfiguration metaConfiguration = MetaConfiguration.builder()
                                                           .serviceProvider(serviceProviderResponse.getResource())
                                                           .resourceTypes(resourceTypeList)
                                                           .schemas(schemaList)
                                                           .build();
    return new ServerResponse<>(httpResponseBuilder.build(), isExpectedResponseCode, metaConfiguration);

  }

  /**
   * loads the ResourceTypes from the ServiceProvider. If meta-ResourceTypes should be excluded the code will
   * try to add a filter into the request to exclude the ResourceTypes directly on server-side
   */
  private ServerResponse<ListResponse<ResourceType>> loadResourceTypes(ServerResponse<ServiceProvider> serviceProviderResponse)
  {
    if (metaConfigLoaderDetails.isExcludeMetaResourceTypes() && serviceProviderResponse.isSuccess())
    {
      if (serviceProviderResponse.getResource().getFilterConfig().isSupported())
      {
        FilterBuilder filterBuilder = null;
        for ( String metaResourceTypeEndpoint : metaConfigLoaderDetails.getMetaResourceTypeNames() )
        {
          if (filterBuilder == null)
          {
            filterBuilder = resourceTypeLoaderBuilder.filter(true,
                                                             AttributeNames.RFC7643.NAME,
                                                             Comparator.NE,
                                                             metaResourceTypeEndpoint)
                                                     .or(AttributeNames.RFC7643.ID,
                                                         Comparator.NE,
                                                         metaResourceTypeEndpoint,
                                                         true);
          }
          else
          {
            filterBuilder.and(true, AttributeNames.RFC7643.NAME, Comparator.NE, metaResourceTypeEndpoint)
                         .or(AttributeNames.RFC7643.ID, Comparator.NE, metaResourceTypeEndpoint, true);
          }
        }
        return Optional.ofNullable(filterBuilder)
                       .map(FilterBuilder::build)
                       .orElse(resourceTypeLoaderBuilder)
                       .post()
                       .sendRequest();
      }
    }
    return resourceTypeLoaderBuilder.post().sendRequest();
  }

  /**
   * loads the Schemas from the ServiceProvider. If meta-schemas should be excluded the code will try to add a
   * filter into the request to exclude the schemas directly on server-side
   */
  private ServerResponse<ListResponse<Schema>> loadSchemas(ServerResponse<ServiceProvider> serviceProviderResponse)
  {
    if (metaConfigLoaderDetails.isExcludeMetaSchemas() && serviceProviderResponse.isSuccess())
    {
      if (serviceProviderResponse.getResource().getFilterConfig().isSupported())
      {
        FilterBuilder filterBuilder = null;
        for ( String metaSchemaUri : metaConfigLoaderDetails.getMetaSchemaUris() )
        {
          if (filterBuilder == null)
          {
            filterBuilder = schemaLoaderBuilder.filter(AttributeNames.RFC7643.ID, Comparator.NE, metaSchemaUri);
          }
          else
          {
            filterBuilder.and(AttributeNames.RFC7643.ID, Comparator.NE, metaSchemaUri);
          }
        }
        return Optional.ofNullable(filterBuilder)
                       .map(FilterBuilder::build)
                       .orElse(schemaLoaderBuilder)
                       .post()
                       .sendRequest();
      }
    }
    return schemaLoaderBuilder.post().sendRequest();
  }
}
