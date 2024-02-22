package de.captaingoldfish.scim.sdk.client.builder.config;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import lombok.Builder;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 14.10.2023
 */
public class MetaConfigRequestDetails
{

  public static final List<String> DEFAULT_META_SCHEMA_URIS = Arrays.asList(SchemaUris.META,
                                                                            SchemaUris.SERVICE_PROVIDER_CONFIG_URI,
                                                                            SchemaUris.RESOURCE_TYPE_URI,
                                                                            SchemaUris.RESOURCE_TYPE_FEATURE_EXTENSION_URI,
                                                                            SchemaUris.SCHEMA_URI);

  public static final List<String> DEFAULT_META_RESOURCE_TYPES_NAMES = Arrays.asList(ResourceTypeNames.SERVICE_PROVIDER_CONFIG,
                                                                                     ResourceTypeNames.RESOURCE_TYPE,
                                                                                     ResourceTypeNames.SCHEMA);

  /**
   * the endpoint where the ServiceProviderConfig can be found
   */
  @Getter
  private final String serviceProviderEndpoint;

  /**
   * the endpoint where the ResourceTypes can be found
   */
  @Getter
  private final String resourceTypeEndpoint;

  /**
   * the endpoint where the Schemas can be found
   */
  @Getter
  private final String schemasEndpoint;

  /**
   * if the meta-schemas should be excluded
   */
  @Getter
  private final boolean excludeMetaSchemas;

  /**
   * if the meta-ResourceTypes should be excluded
   */
  @Getter
  private final boolean excludeMetaResourceTypes;

  /**
   * the meta-ResourceTypes. This field is only used if the field {@link #excludeMetaResourceTypes} is true
   */
  @Getter
  private final List<String> metaResourceTypeNames;

  /**
   * the meta-Schemas. This field is only used if the field {@link #excludeMetaSchemas} is true
   */
  @Getter
  private final List<String> metaSchemaUris;


  public MetaConfigRequestDetails()
  {
    this.serviceProviderEndpoint = EndpointPaths.SERVICE_PROVIDER_CONFIG;
    this.resourceTypeEndpoint = EndpointPaths.RESOURCE_TYPES;
    this.schemasEndpoint = EndpointPaths.SCHEMAS;
    this.excludeMetaSchemas = false;
    this.excludeMetaResourceTypes = false;
    this.metaResourceTypeNames = DEFAULT_META_RESOURCE_TYPES_NAMES;
    this.metaSchemaUris = DEFAULT_META_SCHEMA_URIS;
  }

  @Builder
  public MetaConfigRequestDetails(String serviceProviderEndpoint,
                                  String resourceTypeEndpoint,
                                  String schemasEndpoint,
                                  boolean excludeMetaSchemas,
                                  boolean excludeMetaResourceTypes,
                                  List<String> metaSchemaUris,
                                  List<String> metaResourceTypeNames)
  {
    this.serviceProviderEndpoint = Optional.ofNullable(serviceProviderEndpoint)
                                           .orElse(EndpointPaths.SERVICE_PROVIDER_CONFIG);
    this.resourceTypeEndpoint = Optional.ofNullable(resourceTypeEndpoint).orElse(EndpointPaths.RESOURCE_TYPES);
    this.schemasEndpoint = Optional.ofNullable(schemasEndpoint).orElse(EndpointPaths.SCHEMAS);
    this.excludeMetaSchemas = excludeMetaSchemas;
    this.excludeMetaResourceTypes = excludeMetaResourceTypes;
    this.metaSchemaUris = Optional.ofNullable(metaSchemaUris).orElse(DEFAULT_META_SCHEMA_URIS);
    this.metaResourceTypeNames = Optional.ofNullable(metaResourceTypeNames).orElse(DEFAULT_META_RESOURCE_TYPES_NAMES);
  }
}
