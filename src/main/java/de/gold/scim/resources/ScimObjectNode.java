package de.gold.scim.resources;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.gold.scim.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:10 <br>
 * <br>
 */
public class ScimObjectNode extends ObjectNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimObjectNode(SchemaAttribute schemaAttribute)
  {
    super(JsonNodeFactory.instance);
    this.schemaAttribute = schemaAttribute;
  }

}
