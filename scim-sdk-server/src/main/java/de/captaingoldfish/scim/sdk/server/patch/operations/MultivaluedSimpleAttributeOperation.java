package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;

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

  public MultivaluedSimpleAttributeOperation(AttributePathRoot attributePath,
                                             PatchOp patchOp,
                                             ArrayNode values,
                                             List<String> valueStringList)
  {
    super(attributePath.getSchemaAttribute().getSchema(), attributePath.getSchemaAttribute(), patchOp, values,
          valueStringList);
    this.attributePath = attributePath;
  }
}
