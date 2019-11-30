package de.captaingoldfish.scim.sdk.common.exceptions;

public class InvalidSchemaException extends ScimException
{

  public InvalidSchemaException(String message)
  {
    this(message, null, null, null);
  }

  public InvalidSchemaException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
