package de.captaingoldfish.scim.sdk.server.schemas.exceptions;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 09.04.2021
 */
public class AttributeValidationException extends RuntimeException
{

  @Getter
  private SchemaAttribute schemaAttribute;

  public AttributeValidationException(SchemaAttribute schemaAttribute, String message)
  {
    super(message);
    this.schemaAttribute = schemaAttribute;
  }

  public AttributeValidationException(SchemaAttribute schemaAttribute, String message, Throwable cause)
  {
    super(message, cause);
    this.schemaAttribute = schemaAttribute;
  }
}
