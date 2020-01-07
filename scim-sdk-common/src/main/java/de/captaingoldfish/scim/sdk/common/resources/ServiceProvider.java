package de.captaingoldfish.scim.sdk.common.resources;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.SortConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;
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
                         List<AuthenticationScheme> authenticationSchemes)
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
    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.SERVICE_PROVIDER_CONFIG)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    setMeta(meta);
  }

  /**
   * An HTTP-addressable URL pointing to the service provider's human-consumable help documentation. OPTIONAL.
   */
  public Optional<String> getDocumentationUri()
  {
    return getStringAttribute(AttributeNames.RFC7643.DOCUMENTATION_URI);
  }

  /**
   * An HTTP-addressable URL pointing to the service provider's human-consumable help documentation. OPTIONAL.
   */
  public void setDocumentationUri(String documentationUri)
  {
    setAttribute(AttributeNames.RFC7643.DOCUMENTATION_URI, documentationUri);
    getMeta().ifPresent(meta -> meta.setLastModified(LocalDateTime.now()));
  }

  /**
   * A complex type that specifies PATCH configuration options. REQUIRED. See Section 3.5.2 of [RFC7644].
   */
  public PatchConfig getPatchConfig()
  {
    return getObjectAttribute(AttributeNames.RFC7643.PATCH, PatchConfig.class).orElse(PatchConfig.builder().build());
  }

  /**
   * A complex type that specifies PATCH configuration options. REQUIRED. See Section 3.5.2 of [RFC7644].
   */
  public void setPatchConfig(PatchConfig patchConfig)
  {
    setAttribute(AttributeNames.RFC7643.PATCH, Optional.ofNullable(patchConfig).orElse(PatchConfig.builder().build()));
    getMeta().ifPresent(meta -> meta.setLastModified(LocalDateTime.now()));
  }

  /**
   * A complex type that specifies bulk configuration options. See Section 3.7 of [RFC7644]. REQUIRED.
   */
  public BulkConfig getBulkConfig()
  {
    return getObjectAttribute(AttributeNames.RFC7643.BULK, BulkConfig.class).orElse(BulkConfig.builder().build());
  }

  /**
   * A complex type that specifies bulk configuration options. See Section 3.7 of [RFC7644]. REQUIRED.
   */
  public void setBulkConfig(BulkConfig bulkConfig)
  {
    setAttribute(AttributeNames.RFC7643.BULK, Optional.ofNullable(bulkConfig).orElse(BulkConfig.builder().build()));
    getMeta().ifPresent(meta -> meta.setLastModified(LocalDateTime.now()));
  }

  /**
   * A complex type that specifies FILTER options. REQUIRED. See Section 3.4.2.2 of [RFC7644].
   */
  public FilterConfig getFilterConfig()
  {
    return getObjectAttribute(AttributeNames.RFC7643.FILTER, FilterConfig.class).orElse(FilterConfig.builder().build());
  }

  /**
   * A complex type that specifies FILTER options. REQUIRED. See Section 3.4.2.2 of [RFC7644].
   */
  public void setFilterConfig(FilterConfig filterConfig)
  {
    setAttribute(AttributeNames.RFC7643.FILTER,
                 Optional.ofNullable(filterConfig).orElse(FilterConfig.builder().build()));
    getMeta().ifPresent(meta -> meta.setLastModified(LocalDateTime.now()));
  }

  /**
   * A complex type that specifies configuration options related to changing a password. REQUIRED.
   */
  public ChangePasswordConfig getChangePasswordConfig()
  {
    return getObjectAttribute(AttributeNames.RFC7643.CHANGE_PASSWORD,
                              ChangePasswordConfig.class).orElse(ChangePasswordConfig.builder().build());
  }

  /**
   * A complex type that specifies configuration options related to changing a password. REQUIRED.
   */
  public void setChangePasswordConfig(ChangePasswordConfig changePasswordConfig)
  {
    setAttribute(AttributeNames.RFC7643.CHANGE_PASSWORD,
                 Optional.ofNullable(changePasswordConfig).orElse(ChangePasswordConfig.builder().build()));
    getMeta().ifPresent(meta -> meta.setLastModified(LocalDateTime.now()));
  }

  /**
   * A complex type that specifies Sort configuration options. REQUIRED.
   */
  public SortConfig getSortConfig()
  {
    return getObjectAttribute(AttributeNames.RFC7643.SORT, SortConfig.class).orElse(SortConfig.builder().build());
  }

  /**
   * A complex type that specifies Sort configuration options. REQUIRED.
   */
  public void setSortConfig(SortConfig sortConfig)
  {
    setAttribute(AttributeNames.RFC7643.SORT, Optional.ofNullable(sortConfig).orElse(SortConfig.builder().build()));
    getMeta().ifPresent(meta -> meta.setLastModified(LocalDateTime.now()));
  }

  /**
   * A complex type that specifies ETag configuration options. REQUIRED.
   */
  public ETagConfig getETagConfig()
  {
    return getObjectAttribute(AttributeNames.RFC7643.ETAG, ETagConfig.class).orElse(ETagConfig.builder().build());
  }

  /**
   * A complex type that specifies ETag configuration options. REQUIRED.
   */
  public void setETagConfig(ETagConfig eTagConfig)
  {
    setAttribute(AttributeNames.RFC7643.ETAG, Optional.ofNullable(eTagConfig).orElse(ETagConfig.builder().build()));
    getMeta().ifPresent(meta -> meta.setLastModified(LocalDateTime.now()));
  }

  /**
   * A multi-valued complex type that specifies supported authentication scheme properties. To enable seamless
   * discovery of configurations, the service provider SHOULD, with the appropriate security considerations,
   * make the authenticationSchemes attribute publicly accessible without prior authentication. REQUIRED.
   */
  public List<AuthenticationScheme> getAuthenticationSchemes()
  {
    return getArrayAttribute(AttributeNames.RFC7643.AUTHENTICATION_SCHEMES, AuthenticationScheme.class);
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
      log.warn("no authentication scheme has been set, this will cause a DocumentValidationException on the "
               + "\"/ServiceProviderConfig\" endpoint!");
    }
    setAttribute(AttributeNames.RFC7643.AUTHENTICATION_SCHEMES, authenticationSchemes);
    getMeta().ifPresent(meta -> meta.setLastModified(LocalDateTime.now()));
  }
}
