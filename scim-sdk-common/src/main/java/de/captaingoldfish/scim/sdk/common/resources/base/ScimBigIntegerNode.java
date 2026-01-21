package de.captaingoldfish.scim.sdk.common.resources.base;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.node.BigIntegerNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:19 <br>
 * <br>
 */
public class ScimBigIntegerNode extends BigIntegerNode implements ScimNode
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimBigIntegerNode(SchemaAttribute schemaAttribute, BigInteger value)
  {
    super(value);
    this.schemaAttribute = schemaAttribute;
  }
}
