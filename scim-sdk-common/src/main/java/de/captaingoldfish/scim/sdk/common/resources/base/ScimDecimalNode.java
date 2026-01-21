package de.captaingoldfish.scim.sdk.common.resources.base;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.node.DecimalNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.Getter;


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
}
