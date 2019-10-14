package de.gold.scim.exceptions;

import de.gold.scim.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 17:41 <br>
 * <br>
 */
public class ConflictException extends ScimException
{

  public ConflictException(String message)
  {
    super(message, null, HttpStatus.SC_CONFLICT, null);
  }
}
