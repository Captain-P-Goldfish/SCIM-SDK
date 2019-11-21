package de.captaingoldfish.scim.sdk.common.exceptions;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 21.11.2019 - 10:02 <br>
 * <br>
 */
public class PreconditionFailedException extends ScimException
{

  public PreconditionFailedException(String message)
  {
    super(message, null, HttpStatus.PRECONDITION_FAILED, null);
  }
}
