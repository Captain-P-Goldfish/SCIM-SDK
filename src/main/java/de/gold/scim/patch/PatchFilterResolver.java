package de.gold.scim.patch;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.gold.scim.filter.AttributePathLeaf;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.schemas.ResourceType;


/**
 * author Pascal Knueppel <br>
 * created at: 30.10.2019 - 16:20 <br>
 * <br>
 */
public class PatchFilterResolver
{

  /**
   * the resource type for which the filter should be resolved
   */
  private ResourceType resourceType;

  public PatchFilterResolver(ResourceType resourceType)
  {
    this.resourceType = resourceType;
  }

  /**
   * will check if the given complex node matches the given filter
   * 
   * @param complexNode
   * @param path
   * @return
   */
  public Optional<ObjectNode> isNodeMatchingFilter(ObjectNode complexNode, FilterNode path)
  {
    if (AttributePathLeaf.class.isAssignableFrom(path.getClass()))
    {
      return Optional.of(complexNode);
    }
    return Optional.empty();
  }
}
