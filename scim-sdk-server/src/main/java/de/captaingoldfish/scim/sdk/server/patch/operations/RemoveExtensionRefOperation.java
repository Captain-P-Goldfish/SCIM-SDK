package de.captaingoldfish.scim.sdk.server.patch.operations;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.server.patch.PatchExtensionAttributePath;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 12.01.2024
 */
@Getter
public class RemoveExtensionRefOperation extends PatchOperation<ObjectNode>
{

  /**
   * the filter-expression of this patch-operation. Should not have much relevant information
   */
  private final PatchExtensionAttributePath attributePath;

  public RemoveExtensionRefOperation(Schema schema, PatchOp patchOp)
  {
    super(schema, null, patchOp, null);
    this.attributePath = new PatchExtensionAttributePath(schema);
  }

  @Override
  public ObjectNode parseJsonNode(JsonNode jsonNode)
  {
    return null;
  }
}
