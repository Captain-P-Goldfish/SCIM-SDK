package de.gold.scim.common.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 18:05 <br>
 * <br>
 */
public class UnknownValueException extends ScimException
{

  public UnknownValueException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
