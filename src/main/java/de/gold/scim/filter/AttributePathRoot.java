package de.gold.scim.filter;

import de.gold.scim.filter.antlr.FilterAttributeName;
import de.gold.scim.filter.antlr.ScimFilterParser;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.SchemaAttribute;
import de.gold.scim.utils.RequestUtils;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 28.10.2019 - 23:14 <br>
 * <br>
 * this is a leaf node for resolving patch expressions that will hold the full name of the attribute e.g.
 * name.givenName or userName or emails.primary etc.
 */
public class AttributePathRoot extends FilterNode
{

  /**
   * if the attribute path expression has a filter expression
   */
  @Getter
  private final FilterNode child;

  /**
   * the fully qualified resource uri if used
   */
  private final FilterAttributeName filterAttributeName;

  /**
   * the schema attribute that represents this attribute name
   */
  @Getter
  private final SchemaAttribute schemaAttribute;

  /**
   * represents the original expression of this node
   */
  @Setter
  private String originalExpressionString;

  public AttributePathRoot(FilterNode child, ResourceType resourceType, ScimFilterParser.ValuePathContext ctx)
  {
    this.child = child;
    this.filterAttributeName = new FilterAttributeName((ScimFilterParser.ValuePathContext)null, ctx.attributePath());
    this.schemaAttribute = RequestUtils.getSchemaAttributeForFilter(resourceType, filterAttributeName);
    setSubAttributeName(ctx.subattribute == null ? null : ctx.subattribute.getText());
  }

  public String getResourceUri()
  {
    return filterAttributeName.getResourceUri();
  }

  public String getShortName()
  {
    return filterAttributeName.getShortName();
  }

  public String getFullName()
  {
    return filterAttributeName.getFullName();
  }

  public String getParentAttributeName()
  {
    return filterAttributeName.getParentAttributeName();
  }

  public String getComplexSubAttributeName()
  {
    return filterAttributeName.getComplexSubAttributeName();
  }

  public String getAttributeName()
  {
    return filterAttributeName.getAttributeName();
  }

  @Override
  public String toString()
  {
    return originalExpressionString == null ? (child == null ? filterAttributeName.toString() : child.toString())
      : originalExpressionString;
  }
}
