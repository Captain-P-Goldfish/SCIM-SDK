package de.captaingoldfish.scim.sdk.common.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:55 <br>
 * <br>
 */
public class InvalidResourceTypeException extends ScimException
{

  public InvalidResourceTypeException(String message)
  {
    this(message, null, null, null);
  }

  public InvalidResourceTypeException(String message, Throwable cause)
  {
    this(message, cause, null, null);
  }

  public InvalidResourceTypeException(String message, Throwable cause, String scimType)
  {
    this(message, cause, null, scimType);
  }

  public InvalidResourceTypeException(String message, Throwable cause, Integer status)
  {
    this(message, cause, status, null);
  }

  public InvalidResourceTypeException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
