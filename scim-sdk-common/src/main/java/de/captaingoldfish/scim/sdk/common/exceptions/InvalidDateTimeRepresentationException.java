package de.captaingoldfish.scim.sdk.common.exceptions;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 29.09.2019 - 21:49 <br>
 * <br>
 */
public class InvalidDateTimeRepresentationException extends ScimException
{

  public InvalidDateTimeRepresentationException(String message)
  {
    this(message, null, null, null);
  }

  public InvalidDateTimeRepresentationException(String message, String scimType)
  {
    this(message, null, null, scimType);
  }

  public InvalidDateTimeRepresentationException(String message, Throwable cause)
  {
    this(message, cause, null, null);
  }

  public InvalidDateTimeRepresentationException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status == null ? HttpStatus.BAD_REQUEST : status, scimType);
  }
}
