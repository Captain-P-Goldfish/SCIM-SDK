package de.gold.scim.filter;

import de.gold.scim.filter.antlr.FilterAttributeName;
import de.gold.scim.filter.antlr.ScimFilterParser;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.SchemaAttribute;
import de.gold.scim.utils.RequestUtils;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 28.10.2019 - 23:14 <br>
 * <br>
 * this is a leaf node for resolving patch expressions that will hold the full name of the attribute e.g.
 * name.givenName or userName or emails.primary etc.
 */
public class AttributePathLeaf extends FilterNode
{

  /**
   * the fully qualified resource uri if used
   */
  @Getter
  private final FilterAttributeName filterAttributeName;

  /**
   * the schema attribute that represents this attribute name
   */
  @Getter
  private final SchemaAttribute schemaAttribute;

  public AttributePathLeaf(ResourceType resourceType, ScimFilterParser.AttributePathContext ctx)
  {
    this.filterAttributeName = new FilterAttributeName((ScimFilterParser.ValuePathContext)null, ctx);
    this.schemaAttribute = RequestUtils.getSchemaAttributeForFilter(resourceType, filterAttributeName);
  }

  @Override
  public String toString()
  {
    return filterAttributeName.toString();
  }
}
