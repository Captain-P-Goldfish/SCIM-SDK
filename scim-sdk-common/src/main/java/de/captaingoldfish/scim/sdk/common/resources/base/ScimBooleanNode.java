package de.captaingoldfish.scim.sdk.common.resources.base;

import com.fasterxml.jackson.databind.node.BooleanNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:19 <br>
 * <br>
 */
public class ScimBooleanNode extends BooleanNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimBooleanNode(SchemaAttribute schemaAttribute, boolean value)
  {
    super(value);
    this.schemaAttribute = schemaAttribute;
  }

}
