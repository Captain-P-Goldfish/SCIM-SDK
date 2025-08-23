package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

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

  public Context(Authorization authorization)
  {
    this.authorization = Optional.ofNullable(authorization).orElse(new DefaultAuthorization());
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
}
