package de.captaingoldfish.scim.sdk.server.patch.operations;

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
public class ComplexSubAttributeOperation extends PatchOperation<JsonNode>
{

  /**
   * the filter-expression of this patch-operation. Should never contain a filter-expression
   */
  private final AttributePathRoot attributePath;

  /**
   * the parent-attributes definition of the {@link #subAttribute}
   */
  private final SchemaAttribute parentAttribute;

  /**
   * the addressed sub-attribute that should be patched
   */
  private final SchemaAttribute subAttribute;

  public ComplexSubAttributeOperation(AttributePathRoot attributePath,
                                      PatchOp patchOp,
                                      JsonNode value,
                                      List<String> valueStringList)
  {
    super(attributePath.getSchemaAttribute().getSchema(), attributePath.getSchemaAttribute(), patchOp, value,
          valueStringList);
    this.attributePath = attributePath;
    this.subAttribute = attributePath.getSchemaAttribute();
    this.parentAttribute = this.subAttribute.getParent();
  }

}
