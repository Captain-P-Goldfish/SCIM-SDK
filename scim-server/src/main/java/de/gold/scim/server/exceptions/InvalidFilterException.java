package de.gold.scim.server.exceptions;

import de.gold.scim.server.constants.HttpStatus;
import de.gold.scim.server.constants.ScimType;


/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 00:55 <br>
 * <br>
 */
public class InvalidFilterException extends ScimException
{

  public InvalidFilterException(String message, Throwable cause)
  {
    super(message, cause, HttpStatus.BAD_REQUEST, ScimType.RFC7644.INVALID_FILTER);
  }
}
