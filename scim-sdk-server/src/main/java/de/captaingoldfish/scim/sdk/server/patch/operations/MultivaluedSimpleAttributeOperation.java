package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 12.01.2024
 */
@Getter
public class MultivaluedSimpleAttributeOperation extends PatchOperation<ArrayNode>
{

  /**
   * the filter-expression of this patch-operation. Might contain a filter in case of <em>REMOVE</em>-operations
   */
  private final AttributePathRoot attributePath;

  public MultivaluedSimpleAttributeOperation(AttributePathRoot attributePath, PatchOp patchOp)
  {
    super(attributePath.getSchemaAttribute().getSchema(), attributePath.getSchemaAttribute(), patchOp, null);
    this.attributePath = attributePath;
  }

  public MultivaluedSimpleAttributeOperation(AttributePathRoot attributePath, PatchOp patchOp, ArrayNode values)
  {
    super(attributePath.getSchemaAttribute().getSchema(), attributePath.getSchemaAttribute(), patchOp, values);
    this.attributePath = attributePath;
  }

  @Override
  public ArrayNode parseJsonNode(JsonNode jsonNode)
  {
    if (jsonNode == null || jsonNode.isNull())
    {
      return null;
    }
    if (jsonNode.isArray())
    {
      return (ArrayNode)jsonNode;
    }
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.add(jsonNode);
    return arrayNode;
  }

  @Override
  public List<String> getValueStringList()
  {
    if (PatchOp.REMOVE.equals(patchOp))
    {
      return Collections.emptyList();
    }
    if (!valueStringList.isEmpty())
    {
      return valueStringList;
    }
    for ( JsonNode jsonNode : valuesNode )
    {
      valueStringList.add(jsonNode.asText());
    }
    return valueStringList;
  }
}
