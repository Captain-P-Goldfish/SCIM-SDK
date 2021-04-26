package de.captaingoldfish.scim.sdk.server.endpoints;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.RequestValidator;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 23:17 <br>
 * <br>
 * this abstract class is the base for the developer to implement
 */
public abstract class ResourceHandler<T extends ResourceNode>
{

  /**
   * the generic type of this class
   */
  @Getter
  private Class<T> type;

  /**
   * allows to access the definition of the main schema
   */
  @Getter
  @Setter(AccessLevel.PROTECTED)
  private Schema schema;

  /**
   * allows to access the attribute definitions of a schema extension
   */
  @Getter
  @Setter(AccessLevel.PROTECTED)
  private List<Schema> schemaExtensions;

  /**
   * gives access to the changePassword value of the current service provider configuration
   */
  @Getter
  @Setter(AccessLevel.PACKAGE)
  private Supplier<Boolean> changePasswordSupported;

  /**
   * gives access to the filter max results value of the current service provider configuration
   */
  @Setter(AccessLevel.PACKAGE)
  private Supplier<Integer> maxResults;

  /**
   * default constructor that resolves the generic type for this class
   */
  public ResourceHandler()
  {
    Class clazz = getClass();
    Type type = clazz.getGenericSuperclass();
    boolean isParametrizedType;
    do
    {
      isParametrizedType = type instanceof ParameterizedType;
      if (isParametrizedType)
      {
        ParameterizedType parameterizedType = (ParameterizedType)type;
        this.type = (Class<T>)parameterizedType.getActualTypeArguments()[0];
      }
      else
      {
        clazz = clazz.getSuperclass();
        type = clazz.getGenericSuperclass();
      }
    }
    while (!isParametrizedType && !ResourceHandler.class.getName().equals(clazz.getName()));
    if (this.type == null)
    {
      throw new InternalServerException("ResourceHandler implementations must be generified!", null, null);
    }
  }

  /**
   * permanently create a resource
   *
   * @param resource the resource to store
   * @param authorization should return the roles of an user and may contain arbitrary data needed in the
   *          handler implementation
   * @return the stored resource with additional meta information as id, created, lastModified timestamps etc.
   */
  public abstract T createResource(T resource, Authorization authorization);

  /**
   * extract a resource by its id
   *
   * @param id the id of the resource to return
   * @param authorization should return the roles of an user and may contain arbitrary data needed in the
   *          handler implementation
   * @param attributes the attributes that should be returned to the client. If the client sends this parameter
   *          the evaluation of these parameters might help to improve database performance by omitting
   *          unnecessary table joins
   * @param excludedAttributes the attributes that should NOT be returned to the client. If the client send this
   *          parameter the evaluation of these parameters might help to improve database performance by
   *          omitting unnecessary table joins
   * @return the found resource
   */
  public abstract T getResource(String id,
                                Authorization authorization,
                                List<SchemaAttribute> attributes,
                                List<SchemaAttribute> excludedAttributes);

  /**
   * queries several resources based on the following values
   *
   * @param startIndex the start index that has a minimum value of 1. So the given startIndex here will never be
   *          lower than 1
   * @param count the number of entries that should be returned to the client. The minimum value of this value
   *          is 0.
   * @param filter the parsed filter expression if the client has given a filter
   * @param sortBy the attribute value that should be used for sorting
   * @param sortOrder the sort order
   * @param attributes the attributes that should be returned to the client. If the client sends this parameter
   *          the evaluation of these parameters might help to improve database performance by omitting
   *          unnecessary table joins
   * @param excludedAttributes the attributes that should NOT be returned to the client. If the client send this
   *          parameter the evaluation of these parameters might help to improve database performance by
   *          omitting unnecessary table joins
   * @param authorization should return the roles of an user and may contain arbitrary data needed in the
   *          handler implementation
   * @return a list of several resources and a total results value. You may choose to leave the totalResults
   *         value blank but this might lead to erroneous results on the client side
   */
  public abstract PartialListResponse<T> listResources(long startIndex,
                                                       int count,
                                                       FilterNode filter,
                                                       SchemaAttribute sortBy,
                                                       SortOrder sortOrder,
                                                       List<SchemaAttribute> attributes,
                                                       List<SchemaAttribute> excludedAttributes,
                                                       Authorization authorization);

  /**
   * should update an existing resource with the given one. Simply use the id of the given resource and override
   * the existing one with the given one. Be careful there have been no checks in advance for you if the
   * resource to update does exist. This has to be done manually.<br>
   * <br>
   * <b>NOTE:</b><br>
   * this method is also called by patch. But in the case of patch the check if the resource does exist will
   * have been executed and the given resource is the already updated resource.
   *
   * @param resourceToUpdate the resource that should override an existing one
   * @param authorization should return the roles of an user and may contain arbitrary data needed in the
   *          handler implementation
   * @return the updated resource with the values changed and a new lastModified value
   */
  public abstract T updateResource(T resourceToUpdate, Authorization authorization);

  /**
   * permanently deletes the resource with the given id
   *
   * @param id the id of the resource to delete
   * @param authorization should return the roles of an user and may contain arbitrary data needed in the
   *          handler implementation
   */
  public abstract void deleteResource(String id, Authorization authorization);

  /**
   * @return true if the value in the in the corresponding value in the
   *         {@link de.captaingoldfish.scim.sdk.common.resources.ServiceProvider} configuration is true, false
   *         else
   */
  public final boolean isChangePasswordSupported()
  {
    return Optional.ofNullable(changePasswordSupported).map(Supplier::get).orElse(false);
  }

  /**
   * @return the maximum results value from the current
   *         {@link de.captaingoldfish.scim.sdk.common.resources.ServiceProvider} configuration
   */
  public final int getMaxResults()
  {
    return Optional.ofNullable(maxResults).map(Supplier::get).orElse(Integer.MAX_VALUE);
  }

  /**
   * @return allows to define a custom request validator that is executed after schema validation and before the
   *         call to the actual {@link ResourceHandler} implementation.
   */
  public RequestValidator<T> getRequestValidator()
  {
    return null;
  }
}
