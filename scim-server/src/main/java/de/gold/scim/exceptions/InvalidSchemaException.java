package de.gold.scim.exceptions;

public class InvalidSchemaException extends ScimException
{

  public InvalidSchemaException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
