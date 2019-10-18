package de.gold.scim.resources;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.resources.complex.AuthenticationScheme;
import de.gold.scim.resources.complex.BulkConfig;
import de.gold.scim.resources.complex.ChangePasswordConfig;
import de.gold.scim.resources.complex.ETagConfig;
import de.gold.scim.resources.complex.FilterConfig;
import de.gold.scim.resources.complex.PatchConfig;
import de.gold.scim.resources.complex.SortConfig;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 09:39 <br>
 * <br>
 * SCIM provides a schema for representing the service provider's configuration, identified using the
 * following schema URI: "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig". The service provider
 * configuration resource enables a service provider to discover SCIM specification features in a standardized
 * form as well as provide additional implementation details to clients. All attributes have a mutability of
 * "readOnly". Unlike other core resources, the "id" attribute is not required for the service provider
 * configuration resource.
 */
@Slf4j
public class ServiceProvider extends ResourceNode
{

  @Builder
  public ServiceProvider(String documentationUri,
                         PatchConfig patchConfig,
                         ChangePasswordConfig changePasswordConfig,
                         SortConfig sortConfig,
                         ETagConfig eTagConfig,
                         FilterConfig filterConfig,
                         BulkConfig bulkConfig,
                         List<AuthenticationScheme> authenticationSchemes,
                         ServiceProviderUrlExtension serviceProviderUrlExtension)
  {
    setSchemas(Arrays.asList(SchemaUris.SERVICE_PROVIDER_CONFIG_URI));
    setDocumentationUri(documentationUri);
    setPatchConfig(patchConfig);
    setChangePasswordConfig(changePasswordConfig);
    setSortConfig(sortConfig);
    setETagConfig(eTagConfig);
    setFilterConfig(filterConfig);
    setBulkConfig(bulkConfig);
    setAuthenticationSchemes(authenticationSchemes);
    setServiceProviderUrlExtension(serviceProviderUrlExtension);
  }

  /**
   * An HTTP-addressable URL pointing to the service provider's human-consumable help documentation. OPTIONAL.
   */
  public Optional<String> getDocumentationUri()
  {
    return getStringAttribute(AttributeNames.DOCUMENTATION_URI);
  }

  /**
   * An HTTP-addressable URL pointing to the service provider's human-consumable help documentation. OPTIONAL.
   */
  public void setDocumentationUri(String documentationUri)
  {
    setAttribute(AttributeNames.DOCUMENTATION_URI, documentationUri);
  }

  /**
   * A complex type that specifies PATCH configuration options. REQUIRED. See Section 3.5.2 of [RFC7644].
   */
  public PatchConfig getPatchConfig()
  {
    return getObjectAttribute(AttributeNames.PATCH, PatchConfig.class).orElse(PatchConfig.builder().build());
  }

  /**
   * A complex type that specifies PATCH configuration options. REQUIRED. See Section 3.5.2 of [RFC7644].
   */
  public void setPatchConfig(PatchConfig patchConfig)
  {
    setAttribute(AttributeNames.PATCH, Optional.ofNullable(patchConfig).orElse(PatchConfig.builder().build()));
  }

  /**
   * A complex type that specifies bulk configuration options. See Section 3.7 of [RFC7644]. REQUIRED.
   */
  public BulkConfig getBulkConfig()
  {
    return getObjectAttribute(AttributeNames.BULK, BulkConfig.class).orElse(BulkConfig.builder().build());
  }

  /**
   * A complex type that specifies bulk configuration options. See Section 3.7 of [RFC7644]. REQUIRED.
   */
  public void setBulkConfig(BulkConfig bulkConfig)
  {
    setAttribute(AttributeNames.BULK, Optional.ofNullable(bulkConfig).orElse(BulkConfig.builder().build()));
  }

  /**
   * A complex type that specifies FILTER options. REQUIRED. See Section 3.4.2.2 of [RFC7644].
   */
  public FilterConfig getFilterConfig()
  {
    return getObjectAttribute(AttributeNames.FILTER, FilterConfig.class).orElse(FilterConfig.builder().build());
  }

  /**
   * A complex type that specifies FILTER options. REQUIRED. See Section 3.4.2.2 of [RFC7644].
   */
  public void setFilterConfig(FilterConfig filterConfig)
  {
    setAttribute(AttributeNames.FILTER, Optional.ofNullable(filterConfig).orElse(FilterConfig.builder().build()));
  }

  /**
   * A complex type that specifies configuration options related to changing a password. REQUIRED.
   */
  public ChangePasswordConfig getChangePasswordConfig()
  {
    return getObjectAttribute(AttributeNames.CHANGE_PASSWORD,
                              ChangePasswordConfig.class).orElse(ChangePasswordConfig.builder().build());
  }

  /**
   * A complex type that specifies configuration options related to changing a password. REQUIRED.
   */
  public void setChangePasswordConfig(ChangePasswordConfig changePasswordConfig)
  {
    setAttribute(AttributeNames.CHANGE_PASSWORD,
                 Optional.ofNullable(changePasswordConfig).orElse(ChangePasswordConfig.builder().build()));
  }

  /**
   * A complex type that specifies Sort configuration options. REQUIRED.
   */
  public SortConfig getSortConfig()
  {
    return getObjectAttribute(AttributeNames.SORT, SortConfig.class).orElse(SortConfig.builder().build());
  }

  /**
   * A complex type that specifies Sort configuration options. REQUIRED.
   */
  public void setSortConfig(SortConfig sortConfig)
  {
    setAttribute(AttributeNames.SORT, Optional.ofNullable(sortConfig).orElse(SortConfig.builder().build()));
  }

  /**
   * A complex type that specifies ETag configuration options. REQUIRED.
   */
  public ETagConfig getETagConfig()
  {
    return getObjectAttribute(AttributeNames.ETAG, ETagConfig.class).orElse(ETagConfig.builder().build());
  }

  /**
   * A complex type that specifies ETag configuration options. REQUIRED.
   */
  public void setETagConfig(ETagConfig eTagConfig)
  {
    setAttribute(AttributeNames.ETAG, Optional.ofNullable(eTagConfig).orElse(ETagConfig.builder().build()));
  }

  /**
   * A multi-valued complex type that specifies supported authentication scheme properties. To enable seamless
   * discovery of configurations, the service provider SHOULD, with the appropriate security considerations,
   * make the authenticationSchemes attribute publicly accessible without prior authentication. REQUIRED.
   */
  public List<AuthenticationScheme> getAuthenticationSchemes()
  {
    return getArrayAttribute(AttributeNames.AUTHENTICATION_SCHEMES, AuthenticationScheme.class);
  }

  /**
   * A multi-valued complex type that specifies supported authentication scheme properties. To enable seamless
   * discovery of configurations, the service provider SHOULD, with the appropriate security considerations,
   * make the authenticationSchemes attribute publicly accessible without prior authentication. REQUIRED.
   */
  public void setAuthenticationSchemes(List<AuthenticationScheme> authenticationSchemes)
  {
    if (authenticationSchemes == null || authenticationSchemes.isEmpty())
    {
      log.warn("no authentication scheme has been set this might cause InternalServerErrors on the "
               + "ResourceTypeEndpoint");
    }
    setAttribute(AttributeNames.AUTHENTICATION_SCHEMES, authenticationSchemes);
  }

  /**
   * represents an extension to the {@link ServiceProvider} that can be used to add a global base url that
   * should be used to access the resources
   */
  public Optional<ServiceProviderUrlExtension> getServiceProviderUrlExtension()
  {
    return getObjectAttribute(SchemaUris.SERVICE_PROVIDER_EXTENSION_URL_URI, ServiceProviderUrlExtension.class);
  }

  /**
   * represents an extension to the {@link ServiceProvider} that can be used to add a global base url that
   * should be used to access the resources
   */
  public void setServiceProviderUrlExtension(ServiceProviderUrlExtension serviceProviderUrlExtension)
  {
    setAttribute(SchemaUris.SERVICE_PROVIDER_EXTENSION_URL_URI, serviceProviderUrlExtension);
  }
}
