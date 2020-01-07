package de.captaingoldfish.scim.sdk.common.exceptions;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:14 <br>
 * <br>
 */
public class InvalidConfigException extends ScimException
{

  public InvalidConfigException(String message)
  {
    this(message, null, null);
  }

  public InvalidConfigException(String message, String scimType)
  {
    this(message, null, scimType);
  }

  public InvalidConfigException(String message, Throwable cause)
  {
    this(message, cause, null);
  }

  public InvalidConfigException(String message, Throwable cause, String scimType)
  {
    super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, scimType);
  }
}
