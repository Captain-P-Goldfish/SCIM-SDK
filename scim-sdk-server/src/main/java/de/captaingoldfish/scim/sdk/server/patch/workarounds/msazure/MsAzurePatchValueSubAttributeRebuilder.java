package de.captaingoldfish.scim.sdk.server.patch.workarounds.msazure;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * This class is a workaround handler in order to handle the broken patch requests of Microsoft Azure. Azure
 * sends illegal patch requests that look like this:
 *
 * <pre>
 * {
 *         "op": "Add",
 *         "path": "roles",
 *         "value": [
 *             {
 *                 "value": "{\"id\":\"827f0d2e-be15-4d8f-a8e3-f8697239c112\",
 *                            \"value\":\"DocumentMgmt-Admin\",
 *                            \"displayName\":\"DocumentMgmt Admin\"}"
 *             },
 *             {
 *                 "value": "{\"id\":\"8ae06bd4-35bb-4fcd-977e-14e074ad1192\",
 *                            \"value\":\"Admin\",
 *                            \"displayName\":\"Admin\"}"
 *             }
 *         ]
 *     }
 * </pre>
 *
 * The problem in this request is the nested value-attribute that should not be there. Instead, the request
 * should look like this:
 *
 * <pre>
 *    {
 *         "op": "Add",
 *         "path": "roles",
 *         "value": [
 *
 *             "{\"id\":\"827f0d2e-be15-4d8f-a8e3-f7697239c112\",
 *               \"value\":\"DocumentMgmt-BuyerAdmin\",
 *               \"displayName\":\"DocumentMgmt BuyerAdmin\"}",
 *
 *             "{\"id\":\"8ae06bd4-35bb-4fcd-977e-12e074ad1192\",
 *               \"value\":\"Buyer-Admin\",
 *               \"displayName\":\"Buyer Admin\"}"
 *         ]
 *     }
 * </pre>
 *
 * @author Pascal Knueppel
 * @since 07.10.2023
 */
@Slf4j
@RequiredArgsConstructor
public class MsAzurePatchValueSubAttributeRebuilder extends PatchWorkaround
{


  @Override
  public boolean shouldBeHandled(PatchConfig patchConfig, ResourceType resourceType, PatchRequestOperation operation)
  {
    return patchConfig.isMsAzureValueSubAttributeWorkaroundActive();
  }

  @Override
  public boolean executeOtherHandlers()
  {
    return true;
  }

  /**
   * this method will try to resolve a PatchRequest as described in the class-documentation to its correct state
   *
   * @param patchValues the values sent in the PatchRequest
   * @return the fixed patch operation
   */
  @Override
  public PatchRequestOperation fixPatchRequestOperaton(ResourceType resourceType, PatchRequestOperation operation)
  {
    // the patch operation that is currently executed
    final PatchOp patchOp = operation.getOp();

    // the values of the patch operation. This attribute should actually be empty
    final List<String> patchValues = operation.getValues();

    // simply a check to return early from this method if a remove operation is used
    if (PatchOp.REMOVE.equals(patchOp))
    {
      log.trace("[MS Azure value-subAttribute workaround] only handling 'REPLACE' and 'ADD' requests");
      return operation;
    }

    if (patchValues == null || patchValues.isEmpty())
    {
      log.trace("[MS Azure value-subAttribute workaround] not executed for values-list is empty");
      return operation;
    }

    List<String> fixedValues = new ArrayList<>();
    for ( String patchValue : patchValues )
    {
      final JsonNode jsonNode;
      try
      {
        jsonNode = JsonHelper.readJsonDocument(patchValue);
      }
      catch (Exception ex)
      {
        log.trace("[MS Azure value-subAttribute workaround] ignored value-node because it is no valid JSON "
                  + "object-node");
        fixedValues.add(patchValue);
        continue;
      }

      boolean isObjectNode = jsonNode instanceof ObjectNode;
      if (!isObjectNode)
      {
        log.trace("[MS Azure value-subAttribute workaround] ignored value because it is no JSON-ObjectNode");
        fixedValues.add(patchValue);
        continue;
      }

      ObjectNode objectNode = (ObjectNode)jsonNode;
      if (objectNode.size() != 1)
      {
        log.trace("[MS Azure value-subAttribute workaround] ignored JSON-ObjectNode because it has less or more "
                  + "than 1 sub-nodes");
        fixedValues.add(patchValue);
        continue;
      }

      JsonNode innerValueNode = objectNode.get(AttributeNames.RFC7643.VALUE);
      if (innerValueNode == null)
      {
        log.trace("[MS Azure value-subAttribute workaround] ignored JSON-ObjectNode because it has no value-node");
        fixedValues.add(patchValue);
        continue;
      }

      final JsonNode innerObjectNode;
      try
      {
        innerObjectNode = JsonHelper.readJsonDocument(innerValueNode.textValue());
      }
      catch (Exception ex)
      {
        log.trace("[MS Azure value-subAttribute workaround] ignored inner value-node because it is no valid JSON-node");
        fixedValues.add(patchValue);
        continue;
      }

      isObjectNode = innerObjectNode instanceof ObjectNode;
      if (!isObjectNode)
      {
        log.trace("[MS Azure value-subAttribute workaround] ignored inner value-node because it is no not a JSON "
                  + "object-node");
        fixedValues.add(patchValue);
        continue;
      }

      fixedValues.add(innerObjectNode.toString());
    }

    return PatchRequestOperation.builder()
                                .op(patchOp)
                                .path(operation.getPath().orElse(null))
                                .values(fixedValues)
                                .build();
  }
}
