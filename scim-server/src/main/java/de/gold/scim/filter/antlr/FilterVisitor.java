package de.gold.scim.filter.antlr;

import java.util.Objects;

import org.antlr.v4.runtime.tree.ParseTree;

import de.gold.scim.filter.AndExpressionNode;
import de.gold.scim.filter.AttributeExpressionLeaf;
import de.gold.scim.filter.AttributePathRoot;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.filter.NotExpressionNode;
import de.gold.scim.filter.OrExpressionNode;
import de.gold.scim.schemas.ResourceType;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 12:09 <br>
 * <br>
 * The FilterVisitor iterates through the parsed SCIM-filter-tree and calls the here overridden methods for
 * the specific nodes that have been visited. <br>
 * This visitor will then build its own tree consisting of {@link FilterNode}s that will hold all necessary
 * informations about the filter expression
 */
@Slf4j
public class FilterVisitor extends ScimFilterBaseVisitor<FilterNode>
{

  /**
   * all attributes given in the filter must belong to a specific resource type and this instance is used to
   * check if the given attribute names belong to the given resource type
   */
  private ResourceType resourceType;

  public FilterVisitor(ResourceType resourceType)
  {
    this.resourceType = Objects.requireNonNull(resourceType);
  }

  /**
   * builds a {@link NotExpressionNode}
   *
   * @param ctx the parsing context from antlr
   * @return a {@link NotExpressionNode} that contains the {@link FilterNode} that should be negated
   */
  @Override
  public FilterNode visitNotExpression(ScimFilterParser.NotExpressionContext ctx)
  {
    ParseTree rightNode = ctx.getChild(2);
    return new NotExpressionNode(visit(rightNode));
  }

  /**
   * builds an {@link OrExpressionNode}
   *
   * @param ctx the parsing context from antlr
   * @return an {@link OrExpressionNode} that contains the left and the right {@link FilterNode} the expression
   */
  @Override
  public FilterNode visitOrExpression(ScimFilterParser.OrExpressionContext ctx)
  {
    ParseTree leftNode = ctx.getChild(0);
    ParseTree rightNode = ctx.getChild(2);
    return new OrExpressionNode(visit(leftNode), visit(rightNode));
  }

  /**
   * builds an {@link AndExpressionNode}
   *
   * @param ctx the parsing context from antlr
   * @return an {@link AndExpressionNode} that contains the left and the right {@link FilterNode} the expression
   */
  @Override
  public FilterNode visitAndExpression(ScimFilterParser.AndExpressionContext ctx)
  {
    ParseTree leftNode = ctx.getChild(0);
    ParseTree rightNode = ctx.getChild(2);
    return new AndExpressionNode(visit(leftNode), visit(rightNode));
  }

  /**
   * ignores this node and proceeds with the child of this node. We do not need to evaluate the parenthesis
   * directly because this is implicitly done by antlr in the way the tree is build
   *
   * @param ctx the parsing context from antlr
   * @return a {@link FilterNode} that might be any other {@link FilterNode} implementation
   */
  @Override
  public FilterNode visitParenthesisExpression(ScimFilterParser.ParenthesisExpressionContext ctx)
  {
    return visit(ctx.getChild(1));
  }

  /**
   * builds a leaf node in the tree. The leaf nodes do contain all necessary data to evaluate the filter and to
   * use them to build jpa predicates for example
   *
   * @param ctx the parsing context from antlr
   * @return an {@link AttributeExpressionLeaf} that does contain all necessary data and meta-data
   */
  @Override
  public FilterNode visitAttributeExpression(ScimFilterParser.AttributeExpressionContext ctx)
  {
    return new AttributeExpressionLeaf(ctx, resourceType);
  }

  /**
   * will resolve a value path that is representing a bracket filter notation
   *
   * @param ctx the parsing context from antlr
   * @return resolves the bracket notation into a normal filter expression
   */
  @Override
  public FilterNode visitValuePath(ScimFilterParser.ValuePathContext ctx)
  {
    FilterNode childNode = ctx.filter() == null ? null : visit(ctx.filter());
    return new AttributePathRoot(childNode, resourceType, ctx);
  }
}
