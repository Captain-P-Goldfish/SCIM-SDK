package de.gold.scim.exceptions;

import de.gold.scim.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 00:55 <br>
 * <br>
 */
public class BadRequestException extends ScimException
{

  public BadRequestException(String message, Throwable cause, String scimType)
  {
    super(message, cause, HttpStatus.SC_BAD_REQUEST, scimType);
  }
}
