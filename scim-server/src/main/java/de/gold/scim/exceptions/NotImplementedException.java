package de.gold.scim.exceptions;

import de.gold.scim.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 00:55 <br>
 * <br>
 */
public class NotImplementedException extends ScimException
{

  public NotImplementedException(String message)
  {
    super(message, null, HttpStatus.NOT_IMPLEMENTED, null);
  }
}
