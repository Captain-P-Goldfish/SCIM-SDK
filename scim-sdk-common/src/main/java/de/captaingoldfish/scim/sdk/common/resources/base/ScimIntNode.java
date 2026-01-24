package de.captaingoldfish.scim.sdk.common.resources.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
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
