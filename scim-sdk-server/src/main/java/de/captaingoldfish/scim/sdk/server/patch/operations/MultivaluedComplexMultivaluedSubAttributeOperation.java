package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.Collections;
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
public class MultivaluedComplexMultivaluedSubAttributeOperation extends MultivaluedComplexAttributeOperation
{


  /**
   * the parent-attributes definition of the {@link #subAttribute}
   */
  private final SchemaAttribute parentAttribute;

  /**
   * the addressed sub-attribute that should be patched
   */
  private final SchemaAttribute subAttribute;

  public MultivaluedComplexMultivaluedSubAttributeOperation(AttributePathRoot attributePath,
                                                            SchemaAttribute subAttribute,
                                                            PatchOp patchOp,
                                                            JsonNode values)
  {
    super(attributePath, subAttribute, patchOp, values);
    this.subAttribute = subAttribute;
    this.parentAttribute = this.subAttribute.getParent();
  }

  public MultivaluedComplexMultivaluedSubAttributeOperation(AttributePathRoot attributePath,
                                                            SchemaAttribute subAttribute,
                                                            PatchOp patchOp)
  {
    super(attributePath, subAttribute, patchOp, null);
    this.subAttribute = subAttribute;
    this.parentAttribute = this.subAttribute.getParent();
  }

  @Override
  public List<String> getValueStringList()
  {
    if (!valueStringList.isEmpty())
    {
      return valueStringList;
    }
    if (valuesNode == null || valuesNode.isNull())
    {
      return Collections.emptyList();
    }
    for ( JsonNode jsonNode : valuesNode )
    {
      valueStringList.add(jsonNode.asText());
    }
    return valueStringList;
  }
}
