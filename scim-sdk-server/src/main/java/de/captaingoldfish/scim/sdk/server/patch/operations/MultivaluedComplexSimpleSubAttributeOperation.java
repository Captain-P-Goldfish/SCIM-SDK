package de.captaingoldfish.scim.sdk.server.patch.operations;

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
public class MultivaluedComplexSimpleSubAttributeOperation extends SimpleAttributeOperation
{


  /**
   * the parent-attributes definition of the {@link #subAttribute}
   */
  private final SchemaAttribute parentAttribute;

  /**
   * the addressed sub-attribute that should be patched
   */
  private final SchemaAttribute subAttribute;

  public MultivaluedComplexSimpleSubAttributeOperation(AttributePathRoot attributePath,
                                                       SchemaAttribute subAttribute,
                                                       PatchOp patchOp,
                                                       JsonNode values)
  {
    super(attributePath, subAttribute, patchOp, values);
    this.subAttribute = subAttribute;
    this.parentAttribute = this.subAttribute.getParent();
  }
}
