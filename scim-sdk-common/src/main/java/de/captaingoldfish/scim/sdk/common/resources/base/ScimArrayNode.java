package de.captaingoldfish.scim.sdk.common.resources.base;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
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

  public ScimArrayNode()
  {
    super(JsonNodeFactory.instance);
  }

  public ScimArrayNode(SchemaAttribute schemaAttribute)
  {
    super(JsonNodeFactory.instance);
    this.schemaAttribute = schemaAttribute;
  }
}
