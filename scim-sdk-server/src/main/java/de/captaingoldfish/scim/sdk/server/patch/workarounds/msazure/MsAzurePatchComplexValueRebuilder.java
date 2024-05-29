package de.captaingoldfish.scim.sdk.server.patch.workarounds.msazure;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;


/**
 * This class handles one of the freestyle-SCIM-patch requests by MsAzure. A complex attribute that wants a
 * value to be set on a key with the name value looks as follows if it comes from MsAzure:
 *
 * <pre>
 * {
 *     "Operations": [
 *         {
 *             "op": "Add",
 *             "path": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager",
 *             "value": "271"
 *         }
 *     ],
 *     "schemas": [
 *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
 *     ]
 * }
 * </pre>
 *
 * This is wrong of course because there is no direct path set to the managers subAttribute. MsAzure wants to
 * set this value to the "value"-attribute, so we need to fix this request to look like this:
 *
 * <pre>
 * {
 *     "Operations": [
 *         {
 *             "op": "Add",
 *             "path": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager",
 *             "value": {
 *                 "value": "271"
 *             }
 *         }
 *     ],
 *     "schemas": [
 *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
 *     ]
 * }
 * </pre>
 *
 * @author Pascal Knueppel
 * @since 31.10.2023
 */
@Slf4j
public class MsAzurePatchComplexValueRebuilder extends PatchWorkaround
{

  private SchemaAttribute schemaAttribute;

  @Override
  public boolean shouldBeHandled(PatchConfig patchConfig, ResourceType resourceType, PatchRequestOperation operation)
  {
    boolean isWorkaroundActive = patchConfig.isMsAzureComplexSimpleValueWorkaroundActive();
    if (!isWorkaroundActive)
    {
      return false;
    }
    final String attributeName = operation.getPath().orElse(null);
    if (attributeName == null)
    {
      return false;
    }
    try
    {
      schemaAttribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, attributeName);
    }
    catch (Exception ex)
    {
      return false;
    }
    boolean isComplexDefinition = Type.COMPLEX.equals(schemaAttribute.getType());
    if (!isComplexDefinition)
    {
      log.trace("[MS Azure complex-patch-path-value workaround] ignoring non-complex attribute {}",
                schemaAttribute.getScimNodeName());
      return false;
    }
    ArrayNode arrayNode = operation.getValueNode().orElse(null);
    if (arrayNode == null)
    {
      return false;
    }
    for ( JsonNode jsonNode : arrayNode )
    {
      if (!jsonNode.isObject())
      {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean executeOtherHandlers()
  {
    return true;
  }

  @Override
  public PatchRequestOperation fixPatchRequestOperaton(ResourceType resourceType, PatchRequestOperation operation)
  {
    List<String> patchValues = operation.getValues();
    List<String> fixedValues = new ArrayList<>();
    for ( int i = 0 ; i < patchValues.size() ; i++ )
    {
      String patchValue = patchValues.get(i);
      JsonNode jsonNode;
      try
      {
        jsonNode = JsonHelper.readJsonDocument(patchValue);
      }
      catch (Exception ex)
      {
        log.trace("[MS Azure complex-patch-path-value workaround] ignored value-node because it is no valid JSON "
                  + "object-node");
        fixedValues.add(patchValue);
        continue;
      }

      if (jsonNode != null && (jsonNode.isObject() || jsonNode.isArray()))
      {
        log.trace("[MS Azure complex-patch-path-value workaround] ignoring non-simple-value for attribute {}",
                  schemaAttribute.getScimNodeName());
        fixedValues.add(patchValue);
        continue;
      }

      log.trace("[MS Azure complex-patch-path-value workaround] replacing simple-value with objectNode on attribute {}",
                schemaAttribute.getScimNodeName());
      ObjectNode complexObjectNode = new ScimObjectNode(schemaAttribute);
      complexObjectNode.set(AttributeNames.RFC7643.VALUE, new TextNode(patchValue));
      fixedValues.add(complexObjectNode.toString());
    }

    operation.setValues(fixedValues);
    return operation;
  }
}
