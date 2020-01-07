package de.captaingoldfish.scim.sdk.common.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 18:05 <br>
 * <br>
 */
public class UnknownValueException extends ScimException
{

  public UnknownValueException(String message)
  {
    this(message, null, null, null);
  }

  public UnknownValueException(String message, Throwable cause)
  {
    this(message, cause, null, null);
  }

  public UnknownValueException(String message, Throwable cause, String scimType)
  {
    this(message, cause, null, scimType);
  }

  public UnknownValueException(String message, Throwable cause, Integer status)
  {
    this(message, cause, status, null);
  }

  public UnknownValueException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
