package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 12.01.2024
 */
@Getter
public class MultivaluedComplexAttributeOperation extends PatchOperation<ArrayNode>
{

  /**
   * the filter-expression of this patch-operation that describes which element from the array should be patched
   */
  protected final AttributePathRoot attributePath;

  public MultivaluedComplexAttributeOperation(AttributePathRoot attributePath, PatchOp patchOp)
  {
    super(attributePath.getSchemaAttribute().getSchema(), attributePath.getSchemaAttribute(), patchOp, null);
    this.attributePath = attributePath;
  }

  public MultivaluedComplexAttributeOperation(AttributePathRoot attributePath,
                                              SchemaAttribute schemaAttribute,
                                              PatchOp patchOp,
                                              JsonNode values)
  {
    super(attributePath.getSchemaAttribute().getSchema(), schemaAttribute, patchOp, values);
    this.attributePath = attributePath;
  }

  public MultivaluedComplexAttributeOperation(AttributePathRoot attributePath, PatchOp patchOp, JsonNode values)
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
    ArrayNode arrayNode;
    if (jsonNode instanceof TextNode)
    {
      try
      {
        arrayNode = (ArrayNode)JsonHelper.readJsonDocument(jsonNode.asText());
        return arrayNode;
      }
      catch (Exception ex)
      {
        // do nothing
      }
    }
    arrayNode = new ArrayNode(JsonNodeFactory.instance);
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
      valueStringList.add(jsonNode.toString());
    }
    return valueStringList;
  }
}
