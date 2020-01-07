package de.captaingoldfish.scim.sdk.common.exceptions;

public class InvalidSchemaException extends ScimException
{

  public InvalidSchemaException(String message)
  {
    this(message, null, null, null);
  }

  public InvalidSchemaException(String message, Throwable cause)
  {
    this(message, cause, null, null);
  }

  public InvalidSchemaException(String message, Throwable cause, Integer status)
  {
    this(message, cause, status, null);
  }

  public InvalidSchemaException(String message, Throwable cause, String scimType)
  {
    this(message, cause, null, scimType);
  }

  public InvalidSchemaException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
