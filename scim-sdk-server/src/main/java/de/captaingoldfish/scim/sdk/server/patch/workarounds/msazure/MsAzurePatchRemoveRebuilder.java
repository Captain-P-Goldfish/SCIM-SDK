package de.captaingoldfish.scim.sdk.server.patch.workarounds.msazure;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
 * sends illegal patch-remove requests that look as follows:
 *
 * <pre>
 * PATCH /scim/Groups/2752513
 * {
 *     "schemas": [
 *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
 *     ],
 *     "Operations": [
 *         {
 *             "op": "Remove",
 *             "path": "members",
 *             "value": [
 *                 {
 *                     "value": "2392066"
 *                 }
 *             ]
 *         }
 *     ]
 * }
 * </pre>
 *
 * the value in the request must not be present. Instead, the request should look like this:
 *
 * <pre>
 * PATCH /scim/Groups/2752513
 * {
 *     "schemas": [
 *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
 *     ],
 *     "Operations": [
 *         {
 *             "op": "Remove",
 *             "path": "members[value eq \"2392066\"]"
 *         }
 *     ]
 * }
 * </pre>
 *
 * This class will try its best to fix the bad request and turn it into a valid request
 *
 * @author Pascal Knueppel
 * @since 07.06.2021
 */
@Slf4j
@RequiredArgsConstructor
public final class MsAzurePatchRemoveRebuilder extends PatchWorkaround
{

  /**
   * execute this handler if we have a remove-operation with a path and at least one value
   */
  @Override
  public boolean shouldBeHandled(PatchConfig patchConfig, ResourceType resourceType, PatchRequestOperation operation)
  {
    return PatchOp.REMOVE.equals(operation.getOp()) && operation.getPath().isPresent()
           && !operation.getValues().isEmpty();
  }

  /**
   * also execute other handlers if this one was executed
   */
  @Override
  public boolean executeOtherHandlers()
  {
    return true;
  }


  /**
   * tries to build a valid path operation from the illegal Azure request. If a new path is created it will also
   * clear the {@link #values} list in order to bypass the validation successfully
   *
   * @return the original path if the request not illegal or the path could not be fixed or a new fixed path
   */
  @Override
  public PatchRequestOperation fixPatchRequestOperaton(ResourceType resourceType, PatchRequestOperation operation)
  {
    String path = operation.getPath().get();
    final List<String> values = operation.getValues();

    StringBuilder newPath = new StringBuilder(path).append('[');
    for ( int i = 0 ; i < values.size() ; i++ )
    {
      // if several values are present in the request we will concatenate them with 'or'
      if (i > 0)
      {
        newPath.append(" or ");
      }
      String value = values.get(i);

      if (!JsonHelper.isValidJson(value))
      {
        // do nothing anymore this will cause the request to normally abort at the specific validation point
        log.trace("[MS Azure workaround] attribute in 'value' operand is not valid json: {}", value);
        return operation;
      }
      JsonNode jsonNode = JsonHelper.readJsonDocument(value);
      final boolean isNodeAnObject = Optional.ofNullable(jsonNode).map(JsonNode::isObject).orElse(false);
      if (!isNodeAnObject)
      {
        // do nothing anymore this will cause the request to normally abort at the specific validation point
        log.trace("[MS Azure workaround] attribute in 'value' operand is not an object: {}", value);
        return operation;
      }
      ObjectNode objectNode = (ObjectNode)jsonNode;
      // we will only support the case when one attribute is present per object
      if (objectNode.size() != 1)
      {
        // do nothing anymore this will cause the request to normally abort at the specific validation point
        log.trace("[MS Azure workaround] workaround not executed for 'value' operand object has more than one "
                  + "attributes: {}",
                  objectNode.toPrettyString());
        return operation;
      }
      final String fieldName = objectNode.fieldNames().next();
      final JsonNode valueNode = objectNode.get(fieldName);
      if (valueNode.isObject() || valueNode.isArray())
      {
        // do nothing anymore this will cause the request to normally abort at the specific validation point
        // for simplicity we will only support simple values in such a case
        log.trace("[MS Azure workaround] workaround not executed for attribute in value 'operand' is not a simple type: {}",
                  valueNode.toPrettyString());
        return operation;
      }
      newPath.append(fieldName).append(" eq \"").append(valueNode.textValue()).append("\"");
    }
    // removes all value references from the PatchTargetHandler to bypass the request validation
    values.clear();
    return PatchRequestOperation.builder().op(operation.getOp()).path(newPath.append(']').toString()).build();
  }
}
