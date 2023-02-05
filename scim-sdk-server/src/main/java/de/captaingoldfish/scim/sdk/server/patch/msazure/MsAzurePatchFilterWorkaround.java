package de.captaingoldfish.scim.sdk.server.patch.msazure;

import de.captaingoldfish.scim.sdk.common.resources.base.ScimBooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.AttributeExpressionLeaf;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;


/**
 * @author Pascal Knueppel
 * @since 05.02.2023
 */
public class MsAzurePatchFilterWorkaround
{

  public ScimObjectNode createAttributeFromPatchFilter(AttributePathRoot path)
  {
    SchemaAttribute schemaAttribute = path.getSchemaAttribute();
    ScimObjectNode scimObjectNode = new ScimObjectNode(schemaAttribute);
    boolean creatableNode = path.getChild() instanceof AttributeExpressionLeaf;
    if (!creatableNode)
    {
      return scimObjectNode;
    }
    AttributeExpressionLeaf expressionLeaf = (AttributeExpressionLeaf)path.getChild();
    switch (expressionLeaf.getSchemaAttribute().getType())
    {
      case STRING:
      case REFERENCE:
        scimObjectNode.set(expressionLeaf.getAttributeName(),
                           new ScimTextNode(expressionLeaf.getSchemaAttribute(), expressionLeaf.getValue()));
        break;
      case BOOLEAN:
        scimObjectNode.set(expressionLeaf.getAttributeName(),
                           new ScimBooleanNode(expressionLeaf.getSchemaAttribute(),
                                               expressionLeaf.getBooleanValue().orElse(false)));
        break;
      // I am not handling other attributes here because this is simply done for ms-azure which is only supporting
      // users and groups as resources which do not have other attribute-types than the ones listed above
    }
    return scimObjectNode;
  }
}
