package de.gold.scim.endpoints;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import de.gold.scim.constants.enums.SortOrder;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.resources.ResourceNode;
import de.gold.scim.response.PartialListResponse;
import de.gold.scim.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 23:17 <br>
 * <br>
 */
public abstract class ResourceHandler<T extends ResourceNode>
{

  @Getter
  private Class<T> type;

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
      this.type = (Class<T>)ResourceNode.class;
    }
  }

  public abstract T createResource(T resource);

  public abstract T getResource(String id);

  public abstract PartialListResponse<T> listResources(int startIndex,
                                                       int count,
                                                       FilterNode filter,
                                                       SchemaAttribute sortBy,
                                                       SortOrder sortOrder);

  public abstract T updateResource(T resourceToUpdate);

  public abstract void deleteResource(String id);

}
