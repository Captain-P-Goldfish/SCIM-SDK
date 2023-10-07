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

  /**
   * this method is a workaround for MsAzures expected behaviour that a value is added to a multivalued complex
   * node if it is reference in a patch-filter but not added within the attribute itself
   *
   * <pre>
   * {
   *   "schemas": [
   *     "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *   ],
   *   "Operations": [
   *     {
   *       "op": "add",
   *       "path": "emails[type eq \"work\"].value",
   *       "value": "max@mustermann.de"
   *     }
   *   ]
   * }
   * </pre>
   *
   * must be rebuilt to:
   *
   * <pre>
   * {
   *   "schemas": [
   *     "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *   ],
   *   "Operations": [
   *     {
   *       "op": "add",
   *       "path": "emails[type eq \"work\"].value",
   *       "value": [
   *          {
   *             "value": "max@mustermann.de"
   *          },
   *          {
   *             "type": "work"
   *          }
   *       ]
   *     }
   *   ]
   * }
   * </pre>
   *
   * The complete rebuild is not done within this method. This method will simply create the last objectNode
   * from the filter-expression:
   *
   * <pre>
   *  {
   *    "type": "work"
   *  }
   * </pre>
   *
   * @param path the patch filter expression node.
   * @return the
   */
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
