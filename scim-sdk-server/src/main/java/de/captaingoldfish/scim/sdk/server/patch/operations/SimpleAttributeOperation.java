package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 12.01.2024
 */
@Getter
public class SimpleAttributeOperation extends PatchOperation<JsonNode>
{

  /**
   * the filter-expression of this patch-operation. Should never contain a filter-expression
   */
  private final AttributePathRoot attributePath;

  public SimpleAttributeOperation(AttributePathRoot attributePath, PatchOp patchOp)
  {
    super(attributePath.getSchemaAttribute(), patchOp, null);
    this.attributePath = attributePath;
  }

  public SimpleAttributeOperation(AttributePathRoot attributePath, PatchOp patchOp, JsonNode value)
  {
    super(attributePath.getSchemaAttribute(), patchOp, value);
    this.attributePath = attributePath;
  }

  public SimpleAttributeOperation(AttributePathRoot attributePath,
                                  SchemaAttribute subAttribute,
                                  PatchOp patchOp,
                                  JsonNode values)
  {
    super(subAttribute.getSchema(), subAttribute, patchOp, values);
    this.attributePath = attributePath;
  }

  @Override
  public JsonNode parseJsonNode(JsonNode jsonNode)
  {
    if (jsonNode == null || jsonNode.isNull())
    {
      return null;
    }
    return jsonNode;
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
    valueStringList.add(valuesNode.asText());
    return valueStringList;
  }
}
