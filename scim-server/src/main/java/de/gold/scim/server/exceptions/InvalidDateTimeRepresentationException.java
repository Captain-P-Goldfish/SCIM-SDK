package de.gold.scim.server.exceptions;

import de.gold.scim.server.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 29.09.2019 - 21:49 <br>
 * <br>
 */
public class InvalidDateTimeRepresentationException extends ScimException
{

  public InvalidDateTimeRepresentationException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status == null ? HttpStatus.BAD_REQUEST : status, scimType);
  }
}
