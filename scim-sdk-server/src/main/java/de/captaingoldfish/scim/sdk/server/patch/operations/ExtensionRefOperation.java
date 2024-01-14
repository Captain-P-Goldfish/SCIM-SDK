package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.Collections;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.server.patch.PatchExtensionAttributePath;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 12.01.2024
 */
@Getter
public class ExtensionRefOperation extends PatchOperation<ObjectNode>
{

  /**
   * the filter-expression of this patch-operation. Should not have much relevant information
   */
  private final PatchExtensionAttributePath attributePath;

  public ExtensionRefOperation(Schema schema, String path, PatchOp patchOp, ObjectNode value)
  {
    super(schema, null, patchOp, value,
          Optional.ofNullable(value)
                  .map(v -> Collections.singletonList(v.toString()))
                  .orElseGet(Collections::emptyList));
    this.attributePath = new PatchExtensionAttributePath(path);
  }
}
