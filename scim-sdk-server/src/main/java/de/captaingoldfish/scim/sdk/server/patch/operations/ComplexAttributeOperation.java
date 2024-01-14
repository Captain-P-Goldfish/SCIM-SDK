package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 12.01.2024
 */
@Getter
public class ComplexAttributeOperation extends PatchOperation<ObjectNode>
{

  /**
   * the filter-expression of this patch-operation
   */
  private final AttributePathRoot attributePath;

  public ComplexAttributeOperation(SchemaAttribute schemaAttribute,
                                   PatchOp patchOp,
                                   ObjectNode value,
                                   List<String> valueStringList)
  {
    super(schemaAttribute, patchOp, value, valueStringList);
    this.attributePath = new AttributePathRoot(schemaAttribute);
  }

  public ComplexAttributeOperation(AttributePathRoot attributePath,
                                   PatchOp patchOp,
                                   ObjectNode value,
                                   List<String> valueStringList)
  {
    super(attributePath.getSchemaAttribute(), patchOp, value, valueStringList);
    this.attributePath = attributePath;
  }

}
