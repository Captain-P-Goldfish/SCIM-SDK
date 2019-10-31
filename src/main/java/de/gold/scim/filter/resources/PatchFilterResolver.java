package de.gold.scim.filter.resources;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.gold.scim.filter.AndExpressionNode;
import de.gold.scim.filter.AttributeExpressionLeaf;
import de.gold.scim.filter.AttributePathRoot;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.filter.NotExpressionNode;
import de.gold.scim.filter.OrExpressionNode;


/**
 * author Pascal Knueppel <br>
 * created at: 30.10.2019 - 16:20 <br>
 * <br>
 */
public class PatchFilterResolver
{

  /**
   * will check if the given complex node matches the given filter
   *
   * @param complexNode the complex type that must be checked if it does fit onto the expression
   * @param path the filter expression that tells us how the check should be executed
   * @return the complex node from the parameter if the node does match the filter expression, an empty else
   */
  public Optional<ObjectNode> isNodeMatchingFilter(ObjectNode complexNode, FilterNode path)
  {
    if (AttributePathRoot.class.isAssignableFrom(path.getClass()))
    {
      AttributePathRoot attributePathRoot = (AttributePathRoot)path;
      if (attributePathRoot.getChild() == null)
      {
        return Optional.of(complexNode);
      }
      else
      {
        return isNodeMatchingFilter(complexNode, attributePathRoot.getChild());
      }
    }


    if (AttributeExpressionLeaf.class.isAssignableFrom(path.getClass()))
    {
      return resolveExpression(complexNode, (AttributeExpressionLeaf)path);
    }
    else if (NotExpressionNode.class.isAssignableFrom(path.getClass()))
    {
      NotExpressionNode notExpressionNode = (NotExpressionNode)path;
      return isNodeMatchingFilter(complexNode, notExpressionNode.getRightNode());
    }
    else if (OrExpressionNode.class.isAssignableFrom(path.getClass()))
    {
      OrExpressionNode orExpressionNode = (OrExpressionNode)path;
      Optional<ObjectNode> leftNode = isNodeMatchingFilter(complexNode, orExpressionNode.getLeftNode());
      if (leftNode.isPresent())
      {
        return leftNode;
      }
      return isNodeMatchingFilter(complexNode, orExpressionNode.getRightNode());
    }
    else
    {
      // this can only be the AndExpressionNode
      AndExpressionNode andExpressionNode = (AndExpressionNode)path;
      Optional<ObjectNode> leftNode = isNodeMatchingFilter(complexNode, andExpressionNode.getLeftNode());
      Optional<ObjectNode> rightNode = isNodeMatchingFilter(complexNode, andExpressionNode.getRightNode());
      if (leftNode.isPresent() && rightNode.isPresent())
      {
        return leftNode;
      }
      else
      {
        return Optional.empty();
      }
    }
  }

  /**
   * this method expects the current node to be of a complex type. this means that we only need to resolve the
   * expression on low level nodes as simple types and simple array types.
   *
   * @param complexNode the complex type that must be checked if it does fit onto the expression
   * @param expressionLeaf the filter expression that tells us how the check should be executed
   * @return the complex node from the parameter list if the node does match an empty else
   */
  private Optional<ObjectNode> resolveExpression(ObjectNode complexNode, AttributeExpressionLeaf expressionLeaf)
  {
    if (FilterResourceResolver.checkValueEquality(complexNode.get(expressionLeaf.getSchemaAttribute().getName()),
                                                  expressionLeaf))
    {
      return Optional.of(complexNode);
    }
    else
    {
      return Optional.empty();
    }
  }


}
