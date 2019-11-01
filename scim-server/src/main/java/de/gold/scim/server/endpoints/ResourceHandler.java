package de.gold.scim.server.endpoints;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import de.gold.scim.common.constants.enums.SortOrder;
import de.gold.scim.common.exceptions.InternalServerException;
import de.gold.scim.common.resources.ResourceNode;
import de.gold.scim.common.schemas.SchemaAttribute;
import de.gold.scim.server.filter.FilterNode;
import de.gold.scim.server.response.PartialListResponse;
import lombok.Getter;


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
   * default constructor that resolves the generic type for this class
   */
  public ResourceHandler()
  {
    Type type = getClass().getGenericSuperclass();
    if (type instanceof ParameterizedType)
    {
      ParameterizedType parameterizedType = (ParameterizedType)type;
      this.type = (Class<T>)parameterizedType.getActualTypeArguments()[0];
    }
    else
    {
      throw new InternalServerException("ResourceHandler implementations must be generified!", null, null);
    }
  }

  /**
   * permanently create a resource
   *
   * @param resource the resource to store
   * @return the stored resource with additional meta information as id, created, lastModified timestamps etc.
   */
  public abstract T createResource(T resource);

  /**
   * extract a resource by its id
   *
   * @param id the id of the resource to return
   * @return the found resource
   */
  public abstract T getResource(String id);

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
   * @return a list of several resources and a total results value. You may choose to leave the totalResults
   *         value blank but this might lead to erroneous results on the client side
   */
  public abstract PartialListResponse<T> listResources(long startIndex,
                                                       int count,
                                                       FilterNode filter,
                                                       SchemaAttribute sortBy,
                                                       SortOrder sortOrder);

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
   * @return the updated resource with the values changed and a new lastModified value
   */
  public abstract T updateResource(T resourceToUpdate);

  /**
   * permanently deletes the resource with the given id
   *
   * @param id the id of the resource to delete
   */
  public abstract void deleteResource(String id);

}
