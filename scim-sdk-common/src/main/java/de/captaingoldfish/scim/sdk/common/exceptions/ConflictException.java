package de.captaingoldfish.scim.sdk.common.exceptions;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 17:41 <br>
 * <br>
 */
public class ConflictException extends ScimException
{

  public ConflictException(String message)
  {
    this(message, null, null);
  }

  public ConflictException(String message, Throwable cause)
  {
    this(message, cause, null);
  }

  public ConflictException(String message, String scimType)
  {
    this(message, null, scimType);
  }

  public ConflictException(String message, Throwable cause, String scimType)
  {
    super(message, cause, HttpStatus.CONFLICT, scimType);
  }
}
