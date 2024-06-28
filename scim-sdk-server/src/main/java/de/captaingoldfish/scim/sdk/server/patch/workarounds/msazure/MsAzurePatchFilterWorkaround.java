package de.captaingoldfish.scim.sdk.server.patch.workarounds.msazure;

import java.util.List;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.AttributeExpressionLeaf;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;


/**
 * this class is a workaround for MsAzures expected behaviour that a value is added to a multivalued complex
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
 *       "path": "emails",
 *       "value": [
 *          {
 *             "value": "max@mustermann.de",
 *             "type": "work"
 *          }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * @author Pascal Knueppel
 * @since 05.02.2023
 */
public class MsAzurePatchFilterWorkaround extends PatchWorkaround
{

  /**
   */

  @Override
  public boolean shouldBeHandled(PatchConfig patchConfig, ResourceType resourceType, PatchRequestOperation operation)
  {
    // workaround is not needed
    // if enabled unit-test testMsAzureBehaviourForMultivaluedComplexTypesWithFilterInPathExpression2Update will fail
    return false;
//    Optional<String> path = operation.getPath();
//    return patchConfig.isMsAzureFilterWorkaroundActive() && PatchOp.ADD.equals(operation.getOp()) && path.isPresent()
//           && operation.getValues().size() == 1 && path.get().matches(".*?\\[.*?]\\..*");
  }

  @Override
  public boolean executeOtherHandlers()
  {
    return false;
  }

  @Override
  public PatchRequestOperation fixPatchRequestOperaton(ResourceType resourceType, PatchRequestOperation operation)
  {
    PatchOp patchOp = operation.getOp();
    Optional<String> path = operation.getPath();
    List<String> values = operation.getValues();

    AttributePathRoot attributePathRoot;
    SchemaAttribute schemaAttribute;

    if (resourceType.getAllSchemaExtensions().stream().anyMatch(schema -> schema.getNonNullId().equals(path.get())))
    {
      return operation;
    }
    else
    {
      attributePathRoot = RequestUtils.parsePatchPath(resourceType, path.get());
      schemaAttribute = attributePathRoot.getSchemaAttribute();
    }

    FilterNode childNode = attributePathRoot.getChild();
    if (!(childNode instanceof AttributeExpressionLeaf))
    {
      return operation;
    }

    ScimObjectNode value = new ScimObjectNode();
    value.set(attributePathRoot.getSubAttributeName(), new ScimTextNode(null, values.get(0)));

    AttributeExpressionLeaf attributeExpressionLeaf = (AttributeExpressionLeaf)childNode;
    value.set(attributeExpressionLeaf.getSchemaAttribute().getName(),
              new ScimTextNode(null, attributeExpressionLeaf.getValue()));

    return PatchRequestOperation.builder()
                                .op(patchOp)
                                .path(schemaAttribute.getFullResourceName())
                                .valueNode(value)
                                .build();
  }
}
