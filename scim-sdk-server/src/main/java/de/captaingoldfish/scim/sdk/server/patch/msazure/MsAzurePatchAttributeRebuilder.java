package de.captaingoldfish.scim.sdk.server.patch.msazure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * This class is a workaround handler in order to handle the broken patch requests of Microsoft Azure. Azure
 * sends illegal patch requests that look as follows:
 *
 * <pre>
 * PATCH /scim/Users/2752513
 * {
 *     "schemas": [
 *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
 *     ],
 *     "Operations": [
 *         {
 *             "op": "replace",
 *             "value": {
 *                 "name.givenName": "captain",
 *                 "name.familyName": "goldfish"
 *             }
 *         }
 *     ]
 * }
 * </pre>
 *
 * the value in the request must not be present. Instead the request should look like this:
 *
 * <pre>
 * PATCH /scim/Users/2752513
 * {
 *     "schemas": [
 *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
 *     ],
 *     "Operations": [
 *         {
 *             "op": "replace",
 *             "value": {
 *                 "name": {
 *                     "givenName": "captain",
 *                     "familyName": "goldfish"
 *                 }
 *             }
 *         }
 *     ]
 * }
 * </pre>
 */


@Slf4j
@RequiredArgsConstructor
public final class MsAzurePatchAttributeRebuilder
{

  /**
   * the patch operation that is currently executed
   */
  private final PatchOp patchOp;

  /**
   * the values of the patch operation. This attribute should actually be empty
   */
  private final List<String> values;


  public List<String> fixValues()
  {
    // simply a check to return early from this method if a remove operation is used
    if (PatchOp.REMOVE.equals(patchOp))
    {
      log.trace("[MS Azure REPLACE workaround] only handling 'REPLACE' and 'ADD' requests");
      return values;
    }

    // nothing must be done patch request can be handled normally since no illegal value operand is present
    if (values.isEmpty())
    {
      log.trace("[MS Azure REPLACE workaround] workaround not executed for values-list is empty");
      return values;
    }

    if (values.size() > 1)
    {
      log.trace("[MS Azure REPLACE workaround] workaround not executed for values-list with more than one value");
      return values;
    }

    String value = values.get(0);

    if (!JsonHelper.isValidJson(value))
    {
      // do nothing anymore this will cause the request to normally abort at the specific validation point
      log.trace("[MS Azure REPLACE workaround] attribute in 'value' operand is not valid json: {}", value);
      return values;
    }

    JsonNode jsonNode = JsonHelper.readJsonDocument(value);
    final boolean isNodeAnObject = Optional.ofNullable(jsonNode).map(JsonNode::isObject).orElse(false);
    if (!isNodeAnObject)
    {
      // do nothing anymore this will cause the request to normally abort at the specific validation point
      log.trace("[MS Azure REPLACE workaround] attribute in 'value' operand is not an object: {}", value);
      return values;
    }
    ObjectNode rootObjectNode = (ObjectNode)jsonNode;
    List<ObjectNode> resourceObjectNodes = new ArrayList<>();
    resourceObjectNodes.add(rootObjectNode);

    Optional<List<String>> schemas = JsonHelper.getSimpleAttributeArray(rootObjectNode, AttributeNames.RFC7643.SCHEMAS);
    if (schemas.isPresent())
    {
      // set extension resource object as resourceObjectNode if found
      for ( Iterator<String> it = rootObjectNode.fieldNames() ; it.hasNext() ; )
      {
        String fieldName = it.next();

        if (schemas.get().contains(fieldName))
        {
          JsonNode childNode = rootObjectNode.get(fieldName);

          final boolean isChildNodeAnObject = Optional.ofNullable(childNode).map(JsonNode::isObject).orElse(false);
          if (!isChildNodeAnObject)
          {
            // do nothing anymore this will cause the request to normally abort at the specific validation point
            log.trace("[MS Azure REPLACE workaround] extension attribute in 'value' operand is not an object: {}",
                      value);
            return values;
          }

          resourceObjectNodes.add((ObjectNode)childNode);
        }
      }
    }

    boolean workaroundApplied = false;

    for ( ObjectNode resourceObjectNode : resourceObjectNodes )
    {
      if (fixValuesForResourceObjectNode(resourceObjectNode))
      {
        workaroundApplied = true;
      }
    }

    if (workaroundApplied)
    {
      List<String> newValues = Arrays.asList(JsonHelper.toJsonString(rootObjectNode));
      return newValues;
    }
    else
    {
      return values;
    }
  }

  private boolean fixValuesForResourceObjectNode(ObjectNode resourceObjectNode)
  {
    boolean workaroundApplied = false;

    List<String> fieldNames = new ArrayList<>();
    resourceObjectNode.fieldNames().forEachRemaining(fieldNames::add);

    // apply workaround if necessary
    for ( String originalFieldName : fieldNames )
    {
      if (originalFieldName.lastIndexOf(":") > -1)
      {
        // another resourceObjectNode, it is handled in the outer loop
        continue;
      }

      String[] split = originalFieldName.split("\\.");

      // only one level of nested dot notation supported
      if (split.length == 2)
      {
        JsonNode originalFieldValue = resourceObjectNode.get(originalFieldName);

        String fieldName = split[0];
        String childFieldName = split[1];

        JsonNode node = resourceObjectNode.get(fieldName);
        if (node != null && node.isObject())
        {
          ((ObjectNode)node).set(childFieldName, originalFieldValue);
        }
        else
        {
          resourceObjectNode.putObject(fieldName).set(childFieldName, originalFieldValue);
        }

        resourceObjectNode.remove(originalFieldName);
        workaroundApplied = true;
      }
    }

    return workaroundApplied;
  }
}


