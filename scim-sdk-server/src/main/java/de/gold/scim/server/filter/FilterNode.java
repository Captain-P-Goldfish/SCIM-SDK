package de.gold.scim.server.filter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 16:07 <br>
 * <br>
 * the abstract tree declaration that will be build when the SCIM filter expression is parsed
 */
public abstract class FilterNode
{

  /**
   * each node should now its parent node just in case
   */
  @Getter
  @Setter(AccessLevel.PROTECTED)
  private FilterNode parent;

  /**
   * this attribute is relevant for resolving value-paths on patch operations
   */
  @Getter
  private String subAttributeName;

  /**
   * this attribute is relevant for resolving value-paths on patch operations
   */
  public void setSubAttributeName(String subAttributeName)
  {
    this.subAttributeName = subAttributeName;
  }
}
