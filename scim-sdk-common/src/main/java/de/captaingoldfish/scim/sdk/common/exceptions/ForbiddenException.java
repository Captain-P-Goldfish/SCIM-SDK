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
    super(message, null, HttpStatus.FORBIDDEN, null);
  }
}
