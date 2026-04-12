package de.captaingoldfish.scim.sdk.common.resources.base;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.Getter;
import tools.jackson.databind.node.StringNode;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:19 <br>
 * <br>
 */
public class ScimTextNode extends StringNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimTextNode(SchemaAttribute schemaAttribute, String value)
  {
    super(value);
    this.schemaAttribute = schemaAttribute;
  }

}
