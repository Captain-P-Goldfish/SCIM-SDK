package de.captaingoldfish.scim.sdk.common.exceptions;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 21.11.2019 - 10:02 <br>
 * <br>
 */
public class NotModifiedException extends ScimException
{

  public NotModifiedException()
  {
    this(null, null, null);
  }

  public NotModifiedException(String message)
  {
    this(message, null, null);
  }

  public NotModifiedException(String message, Throwable cause)
  {
    this(message, cause, null);
  }

  public NotModifiedException(String message, Throwable cause, String scimType)
  {
    super(message, cause, HttpStatus.NOT_MODIFIED, scimType);
  }
}
