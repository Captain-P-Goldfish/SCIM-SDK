package de.gold.scim.server.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:55 <br>
 * <br>
 */
public class InvalidResourceTypeException extends ScimException
{

  public InvalidResourceTypeException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
