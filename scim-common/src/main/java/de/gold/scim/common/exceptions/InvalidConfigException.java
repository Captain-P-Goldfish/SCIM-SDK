package de.gold.scim.common.exceptions;

import de.gold.scim.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:14 <br>
 * <br>
 */
public class InvalidConfigException extends ScimException
{

  public InvalidConfigException(String message)
  {
    super(message, null, HttpStatus.INTERNAL_SERVER_ERROR, null);
  }
}
