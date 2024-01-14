package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 12.01.2024
 */
@Getter
public class MultivaluedComplexSubAttributeOperation extends PatchOperation<ArrayNode>
{

  /**
   * the filter-expression of this patch-operation that describes which element from the array should be
   * patched.
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

  public MultivaluedComplexSubAttributeOperation(AttributePathRoot attributePath,
                                                 SchemaAttribute subAttribute,
                                                 PatchOp patchOp,
                                                 ArrayNode values,
                                                 List<String> valueStringList)
  {
    super(attributePath.getSchemaAttribute().getSchema(), subAttribute, patchOp, values, valueStringList);
    this.attributePath = attributePath;
    this.subAttribute = subAttribute;
    this.parentAttribute = this.subAttribute.getParent();
  }

}
