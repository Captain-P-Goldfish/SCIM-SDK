package de.gold.scim.resources.base;

import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:19 <br>
 * <br>
 */
public class ScimTextNode extends TextNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimTextNode(SchemaAttribute schemaAttribute, String value)
  {
    super(value);
    this.schemaAttribute = schemaAttribute;
  }

}
