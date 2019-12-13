package de.captaingoldfish.scim.sdk.common.exceptions;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 27.11.2019 - 22:44 <br>
 * <br>
 */
public class ForbiddenException extends ScimException
{

  public ForbiddenException(String message)
  {
    this(message, null, null);
  }

  public ForbiddenException(String message, String scimType)
  {
    this(message, null, scimType);
  }

  public ForbiddenException(String message, Throwable cause)
  {
    this(message, cause, null);
  }

  public ForbiddenException(String message, Throwable cause, String scimType)
  {
    super(message, cause, HttpStatus.FORBIDDEN, scimType);
  }
}
