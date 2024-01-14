package de.captaingoldfish.scim.sdk.server.filter;

import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.antlr.FilterAttributeName;
import de.captaingoldfish.scim.sdk.server.filter.antlr.ScimFilterParser;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
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
   * this attribute is only set if the field {@link #subAttributeName} is not null. It will hold the attributes
   * definition to the sub-attribute
   */
  @Getter
  private final SchemaAttribute subAttribute;

  /**
   * represents the original expression of this node
   */
  @Setter
  private String originalExpressionString;

  protected AttributePathRoot()
  {
    this.child = null;
    this.filterAttributeName = null;
    this.schemaAttribute = null;
    this.subAttribute = null;
  }

  public AttributePathRoot(FilterNode child, ResourceType resourceType, ScimFilterParser.ValuePathContext ctx)
  {
    this.child = child;
    this.filterAttributeName = new FilterAttributeName((ScimFilterParser.ValuePathContext)null, ctx.attributePath());
    this.schemaAttribute = RequestUtils.getSchemaAttributeForFilter(resourceType, filterAttributeName);
    setSubAttributeName(ctx.subattribute == null ? null : ctx.subattribute.getText());
    if (getSubAttributeName() == null)
    {
      this.subAttribute = null;
    }
    else
    {
      String subAttributeNameRef = String.format("%s.%s", filterAttributeName, getSubAttributeName());
      this.subAttribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, subAttributeNameRef);
    }
  }

  public AttributePathRoot(SchemaAttribute schemaAttribute)
  {
    final String parentAttributeName = Optional.ofNullable(schemaAttribute.getParent())
                                               .map(SchemaAttribute::getName)
                                               .orElseGet(schemaAttribute::getName);
    final String childAttributeName = Optional.ofNullable(schemaAttribute.getParent())
                                              .map(t -> schemaAttribute.getName())
                                              .orElse(null);

    this.child = null;
    this.filterAttributeName = new FilterAttributeName(schemaAttribute.getSchema().getNonNullId(), parentAttributeName,
                                                       childAttributeName);
    this.schemaAttribute = Optional.ofNullable(schemaAttribute.getParent()).orElse(schemaAttribute);
    setSubAttributeName(childAttributeName);
    this.subAttribute = schemaAttribute;
  }

  public boolean isWithFilter()
  {
    return child != null;
  }

  public boolean isWithSubAttributeRef()
  {
    return subAttribute != null;
  }

  public SchemaAttribute getDirectlyReferencedAttribute()
  {
    if (subAttribute == null)
    {
      return schemaAttribute;
    }
    else
    {
      return subAttribute;
    }
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
