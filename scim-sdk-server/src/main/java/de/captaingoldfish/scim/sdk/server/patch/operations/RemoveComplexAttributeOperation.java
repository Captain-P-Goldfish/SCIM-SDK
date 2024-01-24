package de.captaingoldfish.scim.sdk.server.patch.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 12.01.2024
 */
@Getter
public class RemoveComplexAttributeOperation extends PatchOperation<ObjectNode>
{

  /**
   * the filter-expression of this patch-operation
   */
  private final AttributePathRoot attributePath;

  public RemoveComplexAttributeOperation(AttributePathRoot attributePath, PatchOp patchOp)
  {
    super(attributePath.getSchemaAttribute(), patchOp, null);
    this.attributePath = attributePath;
  }

  @Override
  public ObjectNode parseJsonNode(JsonNode jsonNode)
  {
    return null;
  }
}
