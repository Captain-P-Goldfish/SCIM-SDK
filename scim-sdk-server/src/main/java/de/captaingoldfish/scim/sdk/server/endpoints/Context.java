package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.DefaultAuthorization;
import de.captaingoldfish.scim.sdk.server.endpoints.bulkcontext.BulkRequestContext;
import de.captaingoldfish.scim.sdk.server.utils.UriInfos;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


/**
 * a context object that may be optionally added to the request. If a null instance is used on the
 * {@link ResourceEndpoint#handleRequest(...)} methods the API will instantiate an instance by itself which
 * will then be used
 *
 * @author Pascal Knueppel
 * @since 19.06.2021
 */
public class Context
{


  /**
   * the authorization information of the current request
   */
  @Getter
  private Authorization authorization;

  /**
   * the request infos
   */
  @Getter
  @Setter(AccessLevel.PROTECTED)
  private UriInfos uriInfos;

  /**
   * allows to get the current resource URL reference by passing an id <br>
   * <br>
   * e.g. if called on the {@link ResourceHandler} implementation for Groups
   *
   * <pre>
   *     getResourceReferenceUrl().apply("123456")
   *     => http://localhost:8080/scim/v2/Groups/123456
   * </pre>
   */
  @Setter(AccessLevel.PROTECTED)
  private Function<String, String> resourceReferenceUrl;

  /**
   * allows to get cross resource URL of a related resource by passing its name and its id.<br>
   * <br>
   * e.g. if called on the {@link ResourceHandler} implementation for Groups
   *
   * <pre>
   *     getExternalResourceReferenceUrl().apply("User", "123456")
   *     => http://localhost:8080/scim/v2/Users/123456
   * </pre>
   */
  @Setter(AccessLevel.PROTECTED)
  private BiFunction<String, String, String> crossResourceReferenceUrl;

  /**
   * allows to access the original request body if necessary
   */
  @Setter(AccessLevel.PROTECTED)
  private Supplier<String> requestBodySupplier;

  /**
   * will be set if the current request is a bulk request, this object will be null otherwise.
   */
  @Setter(AccessLevel.PROTECTED)
  private BulkRequestContext bulkRequestContext;

  /**
   * if the attributes within the resource objects should be extracted case-insensitive or case exact by their
   * attribute-names.<br>
   * This feature does not work for patch requests
   */
  @Setter
  private Boolean caseInsensitiveValidation;

  /**
   * if the {@link SchemaAttribute#getDefaultValue()} should be respected on requests
   */
  @Setter
  private Boolean useDefaultValuesOnRequest;

  /**
   * if the {@link SchemaAttribute#getDefaultValue()} should be respected on responses
   */
  @Setter
  private Boolean useDefaultValuesOnResponse;

  /**
   * This attribute specifies whether required attributes should be ignored during schema validation of a
   * request.
   */
  @Setter
  private Boolean ignoreRequiredAttributesOnRequest;

  /**
   * if required attributes should only be validated on request or on request and response
   */
  @Setter
  private Boolean ignoreRequiredAttributesOnResponse;

  /**
   * if required extensions should only be validated on request or on request and response
   */
  @Setter
  private Boolean ignoreRequiredExtensionsOnResponse;

  /**
   * defines if the content-type of the HTTP header is validated strict with application/scim+json or not.
   */
  @Setter
  private Boolean lenientContentTypeChecking;

  public Context(Authorization authorization)
  {
    this.authorization = Optional.ofNullable(authorization).orElse(new DefaultAuthorization());
  }

  
  /**
   * merges the configuration of the service provider into this context. Attributes that are already set in
   * this context will not be overwritten.
   *
   * @param serviceProvider the service provider configuration to merge into this context
   */
  public void mergeWithServiceProviderConfig(ServiceProvider serviceProvider)
  {
    this.caseInsensitiveValidation = Optional.ofNullable(this.caseInsensitiveValidation)
                                             .orElseGet(serviceProvider::isCaseInsensitiveValidation);
    this.useDefaultValuesOnRequest = Optional.ofNullable(this.useDefaultValuesOnRequest)
                                             .orElseGet(serviceProvider::isUseDefaultValuesOnRequest);
    this.useDefaultValuesOnResponse = Optional.ofNullable(this.useDefaultValuesOnResponse)
                                              .orElseGet(serviceProvider::isUseDefaultValuesOnResponse);
    this.ignoreRequiredAttributesOnRequest = Optional.ofNullable(this.ignoreRequiredAttributesOnRequest)
                                                     .orElseGet(serviceProvider::isIgnoreRequiredAttributesOnRequest);
    this.ignoreRequiredAttributesOnResponse = Optional.ofNullable(this.ignoreRequiredAttributesOnResponse)
                                                      .orElseGet(serviceProvider::isIgnoreRequiredAttributesOnResponse);
    this.ignoreRequiredExtensionsOnResponse = Optional.ofNullable(this.ignoreRequiredExtensionsOnResponse)
                                                      .orElseGet(serviceProvider::isIgnoreRequiredExtensionsOnResponse);
    this.lenientContentTypeChecking = Optional.ofNullable(this.lenientContentTypeChecking)
                                              .orElseGet(serviceProvider::isLenientContentTypeChecking);
  }

  /**
   * creates a direct reference url to the current resource. <br>
   * <br>
   * e.g. if called on the {@link ResourceHandler} implementation for Users
   *
   * <pre>
   *     getResourceReferenceUrl("123456")
   *     => http://localhost:8080/scim/v2/Users/123456
   * </pre>
   *
   * @param id the id of the resource. The id is not checked if a resource with this id does exist or not
   * @return the fully qualified url to the specific resource with the given id
   */
  public String getResourceReferenceUrl(String id)
  {
    return resourceReferenceUrl.apply(id);
  }

  /**
   * creates a cross-reference url to another resource. <br>
   * <br>
   * e.g. if called on the {@link ResourceHandler} implementation for Groups to create a reference to a user
   * member of the group
   *
   * <pre>
   *     getExternalResourceReferenceUrl("123456", "User")
   *     => http://localhost:8080/scim/v2/Users/123456
   * </pre>
   *
   * @param id the id of the resource. The id is not checked if a resource with this id does exist or not
   * @return the fully qualified url to the specific resource with the given id or an empty if no resource with
   *         the given name was registered
   */
  public Optional<String> getCrossResourceReferenceUrl(String id, String resourceName)
  {
    return Optional.ofNullable(crossResourceReferenceUrl.apply(id, resourceName));
  }

  /**
   * will retrieve the original request body before it was parsed and modified
   */
  public String getRequestBody()
  {
    return requestBodySupplier.get();
  }

  /**
   * will be set if the current request is a bulk request.
   */
  public Optional<BulkRequestContext> getBulkRequestContext()
  {
    return Optional.ofNullable(bulkRequestContext);
  }

  /**
   * @see #caseInsensitiveValidation
   */
  public boolean isCaseInsensitiveValidation()
  {
    return Optional.ofNullable(caseInsensitiveValidation).orElse(false);
  }

  /**
   * @see #useDefaultValuesOnRequest
   */
  public boolean isUseDefaultValuesOnRequest()
  {
    return Optional.ofNullable(useDefaultValuesOnRequest).orElse(true);
  }

  /**
   * @see #useDefaultValuesOnResponse
   */
  public boolean isUseDefaultValuesOnResponse()
  {
    return Optional.ofNullable(useDefaultValuesOnResponse).orElse(true);
  }

  /**
   * @see #ignoreRequiredAttributesOnRequest
   */
  public boolean isIgnoreRequiredAttributesOnRequest()
  {
    return Optional.ofNullable(ignoreRequiredAttributesOnRequest).orElse(false);
  }

  /**
   * @see #ignoreRequiredAttributesOnResponse
   */
  public boolean isIgnoreRequiredAttributesOnResponse()
  {
    return Optional.ofNullable(ignoreRequiredAttributesOnResponse).orElse(false);
  }

  /**
   * @see #ignoreRequiredExtensionsOnResponse
   */
  public boolean isIgnoreRequiredExtensionsOnResponse()
  {
    return Optional.ofNullable(ignoreRequiredExtensionsOnResponse).orElse(false);
  }

  /**
   * @see #lenientContentTypeChecking
   */
  public boolean isLenientContentTypeChecking()
  {
    return Optional.ofNullable(lenientContentTypeChecking).orElse(false);
  }
}
