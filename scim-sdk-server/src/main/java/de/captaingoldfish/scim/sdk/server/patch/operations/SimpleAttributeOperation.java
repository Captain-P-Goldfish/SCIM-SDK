package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
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

  public SimpleAttributeOperation(AttributePathRoot attributePath,
                                  PatchOp patchOp,
                                  JsonNode value,
                                  List<String> valueStringList)
  {
    super(attributePath.getSchemaAttribute().getSchema(), attributePath.getSchemaAttribute(), patchOp, value,
          valueStringList);
    this.attributePath = attributePath;
  }

}
