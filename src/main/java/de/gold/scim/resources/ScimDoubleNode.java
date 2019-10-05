package de.gold.scim.resources;

import com.fasterxml.jackson.databind.node.DoubleNode;

import de.gold.scim.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:19 <br>
 * <br>
 */
public class ScimDoubleNode extends DoubleNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimDoubleNode(SchemaAttribute schemaAttribute, double value)
  {
    super(value);
    this.schemaAttribute = schemaAttribute;
  }
}
