package de.gold.scim.common.resources.base;

import com.fasterxml.jackson.databind.node.IntNode;

import de.gold.scim.common.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:19 <br>
 * <br>
 */
public class ScimIntNode extends IntNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimIntNode(SchemaAttribute schemaAttribute, int value)
  {
    super(value);
    this.schemaAttribute = schemaAttribute;
  }

}
