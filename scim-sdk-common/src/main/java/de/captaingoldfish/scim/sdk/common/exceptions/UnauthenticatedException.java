package de.captaingoldfish.scim.sdk.common.exceptions;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;


/**
 * author Pascal Knueppel <br>
 * created at: 13.07.2020 - 09:05 <br>
 * <br>
 */
public class UnauthenticatedException extends ScimException
{

  public UnauthenticatedException(String message)
  {
    super(message, null, HttpStatus.UNAUTHORIZED, ScimType.Custom.UNAUTENTICATED);
  }
}
