package de.gold.scim.filter;


import lombok.EqualsAndHashCode;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 16:52 <br>
 * <br>
 * represents an expression that should be negated
 */
@EqualsAndHashCode
public class NotExpressionNode extends FilterNode
{

  /**
   * the node that should be negated
   */
  @Getter
  private FilterNode rightNode;

  public NotExpressionNode(FilterNode rightNode)
  {
    rightNode.setParent(this);
    this.rightNode = rightNode;
  }

  @Override
  public String toString()
  {
    return "not ( " + rightNode.toString() + " )";
  }
}
