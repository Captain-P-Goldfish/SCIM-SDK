package de.gold.scim.resources.base;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.gold.scim.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:11 <br>
 * <br>
 */
public class ScimArrayNode extends ArrayNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimArrayNode(SchemaAttribute schemaAttribute)
  {
    super(JsonNodeFactory.instance);
    this.schemaAttribute = schemaAttribute;
  }
}
