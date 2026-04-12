package de.captaingoldfish.scim.sdk.common.resources.base;

import java.math.BigDecimal;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.Getter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.DecimalNode;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:19 <br>
 * <br>
 */
public class ScimDecimalNode extends DecimalNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimDecimalNode(SchemaAttribute schemaAttribute, BigDecimal value)
  {
    super(value);
    this.schemaAttribute = schemaAttribute;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof JsonNode)
    {
      return JsonHelper.isEqual(this, (JsonNode)o);
    }
    return false;
  }
}
