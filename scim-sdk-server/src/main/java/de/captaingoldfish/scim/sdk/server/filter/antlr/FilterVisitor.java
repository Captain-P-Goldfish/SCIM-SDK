package de.captaingoldfish.scim.sdk.server.filter.antlr;

import java.util.Objects;
import java.util.Optional;

import org.antlr.v4.runtime.tree.ParseTree;

import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.exceptions.UnparseableFilterException;
import de.captaingoldfish.scim.sdk.server.filter.AndExpressionNode;
import de.captaingoldfish.scim.sdk.server.filter.AttributeExpressionLeaf;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.filter.NotExpressionNode;
import de.captaingoldfish.scim.sdk.server.filter.OrExpressionNode;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.Getter;
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
  private final ResourceType resourceType;

  /**
   * if the current filter is based on a patch-expression or a list-filter-expression
   */
  private final boolean patchFilter;

  /**
   * the maximum filter depth that must not be exceeded
   */
  private final int maxFilterDepth;

  /**
   * counts the filter-depth of the current filter expression
   */
  @Getter
  private int filterDepth = 1;

  public FilterVisitor(ResourceType resourceType, boolean patchFilter)
  {
    this.resourceType = Objects.requireNonNull(resourceType);
    this.patchFilter = patchFilter;
    this.maxFilterDepth = Optional.ofNullable(resourceType.getResourceHandlerImpl())
                                  .map(ResourceHandler::getServiceProvider)
                                  .map(ServiceProvider::getFilterConfig)
                                  .map(FilterConfig::getMaxFilterDepth)
                                  .orElse(FilterConfig.DEFAULT_MAX_FILTER_DEPTH);
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
    if (maxFilterDepth < ++filterDepth)
    {
      throw new BadRequestException(String.format("Filter depth exceeded maximum allowed depth is '%s'",
                                                  maxFilterDepth));
    }
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
    if (maxFilterDepth < ++filterDepth)
    {
      throw new BadRequestException(String.format("Filter depth exceeded maximum allowed depth is '%s'",
                                                  maxFilterDepth));
    }
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
    // this if-case represents an illegal MsAzure filter-expression like:
    // "emails[type eq \"work\"].value sw \"%s\""
    // we are trying to fix it here by wrapping it into an AndExpressionNode
    if (ctx.compareOperator() != null && ctx.compareValue() != null)
    {
      FilterNode outerExpression = new AttributeExpressionLeaf(ctx, resourceType);
      return new AndExpressionNode(childNode, outerExpression);
    }
    // the following if-block is based on: https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/650
    if (!patchFilter && childNode == null && ctx.compareOperator() == null)
    {
      throw new UnparseableFilterException(String.format("Invalid unparseable filter '%s'", ctx.getText()));
    }
    return new AttributePathRoot(childNode, resourceType, ctx);
  }
}
