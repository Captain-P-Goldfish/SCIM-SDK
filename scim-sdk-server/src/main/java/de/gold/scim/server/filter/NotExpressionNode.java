package de.gold.scim.server.filter;


import lombok.EqualsAndHashCode;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 16:52 <br>
 * <br>
 * represents an expression that should be negated
 */
@EqualsAndHashCode(callSuper = false)
public final class NotExpressionNode extends FilterNode
{

  /**
   * the node that should be negated
   */
  @Getter
  private final FilterNode rightNode;

  public NotExpressionNode(FilterNode rightNode)
  {
    rightNode.setParent(this);
    this.rightNode = rightNode;
    setSubAttributeName(rightNode.getSubAttributeName());
  }

  @Override
  public String toString()
  {
    return "not ( " + rightNode.toString() + " )";
  }
}
