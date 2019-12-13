package de.captaingoldfish.scim.sdk.common.exceptions;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 00:55 <br>
 * <br>
 */
public class NotImplementedException extends ScimException
{

  public NotImplementedException(String message)
  {
    this(message, null, null);
  }

  public NotImplementedException(String message, String scimType)
  {
    this(message, null, scimType);
  }

  public NotImplementedException(String message, Throwable cause)
  {
    this(message, cause, null);
  }

  public NotImplementedException(String message, Throwable cause, String scimType)
  {
    super(message, null, HttpStatus.NOT_IMPLEMENTED, null);
  }
}
