package de.captaingoldfish.scim.sdk.common.resources.base;

import com.fasterxml.jackson.databind.node.BinaryNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 13.10.2023
 */
public class ScimBinaryNode extends BinaryNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimBinaryNode(SchemaAttribute schemaAttribute, byte[] value)
  {
    super(value);
    this.schemaAttribute = schemaAttribute;
  }
}
