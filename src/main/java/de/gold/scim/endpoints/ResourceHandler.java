package de.gold.scim.endpoints;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 23:17 <br>
 * <br>
 */
public interface ResourceHandler<T extends JsonNode>
{

  T createResource(T resource);

  T readResource(String id);

  T listResources();

  T updateResource(T resource, String id);

  T deleteResource(String id);

}
