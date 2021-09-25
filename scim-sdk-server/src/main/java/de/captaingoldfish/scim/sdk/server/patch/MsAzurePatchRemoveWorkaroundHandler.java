package de.captaingoldfish.scim.sdk.server.patch;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * This class is a workaround handler in order to handle the broken patch requests of Microsoft Azure. Azure
 * sends illegal patch-remove requests that looks as follows:
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
 * the value in the request must not be present. Instead the request should look like this:
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
public final class MsAzurePatchRemoveWorkaroundHandler
{

  /**
   * the patch operation that is currently executed
   */
  private final PatchOp patchOp;

  /**
   * the path of the patch operation
   */
  private final String path;

  /**
   * the values of the patch operation. This attribute should actually be empty
   */
  private final List<String> values;

  /**
   * tries to build a valid path operation from the illegal Azure request. If a new path is created it will also
   * clear the {@link #values} list in order to bypass the validation successfully
   *
   * @return the original path if the request not illegal or the path could not be fixed or a new fixed path
   */
  public String fixPath()
  {
    // just a security check to make sure that the if-block that prevents this class to be executed in case of ADD
    // and REPLACE should disappear
    if (!PatchOp.REMOVE.equals(patchOp))
    {
      log.trace("[MS Azure workaround] only handling 'REMOVE' requests");
      return path;
    }
    // nothing must be done patch request can be handled normally since no illegal value operand is present
    if (values.isEmpty())
    {
      log.trace("[MS Azure workaround] workaround not executed for values-list is empty");
      return path;
    }

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
        return path;
      }
      JsonNode jsonNode = JsonHelper.readJsonDocument(value);
      final boolean isNodeAnObject = Optional.ofNullable(jsonNode).map(JsonNode::isObject).orElse(false);
      if (!isNodeAnObject)
      {
        // do nothing anymore this will cause the request to normally abort at the specific validation point
        log.trace("[MS Azure workaround] attribute in 'value' operand is not an object: {}", value);
        return path;
      }
      ObjectNode objectNode = (ObjectNode)jsonNode;
      // we will only support the case when one attribute is present per object
      if (objectNode.size() != 1)
      {
        // do nothing anymore this will cause the request to normally abort at the specific validation point
        log.trace("[MS Azure workaround] workaround not executed for 'value' operand object has more than one "
                  + "attributes: {}",
                  objectNode.toPrettyString());
        return path;
      }
      final String fieldName = objectNode.fieldNames().next();
      final JsonNode valueNode = objectNode.get(fieldName);
      if (valueNode.isObject() || valueNode.isArray())
      {
        // do nothing anymore this will cause the request to normally abort at the specific validation point
        // for simplicity we will only support simple values in such a case
        log.trace("[MS Azure workaround] workaround not executed for attribute in value 'operand' is not a simple type: {}",
                  valueNode.toPrettyString());
        return path;
      }
      newPath.append(fieldName).append(" eq \"").append(valueNode.textValue()).append("\"");
    }
    // removes all value references from the PatchTargetHandler to bypass the request validation
    values.clear();
    return newPath.append(']').toString();
  }
}
