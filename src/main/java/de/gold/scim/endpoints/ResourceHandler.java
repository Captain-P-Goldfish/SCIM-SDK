package de.gold.scim.endpoints;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import de.gold.scim.resources.ResourceNode;
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

  public abstract T readResource(String id);

  public abstract T listResources();

  public abstract T updateResource(T resource, String id);

  public abstract T deleteResource(String id);

}
