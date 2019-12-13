package de.captaingoldfish.scim.sdk.common.exceptions;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:14 <br>
 * <br>
 */
public class InternalServerException extends ScimException
{

  public InternalServerException(String message)
  {
    this(message, null, null);
  }

  public InternalServerException(String message, String scimType)
  {
    this(message, null, scimType);
  }

  public InternalServerException(String message, Throwable cause)
  {
    this(message, cause, null);
  }

  public InternalServerException(String message, Throwable cause, String scimType)
  {
    super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, scimType);
  }
}
