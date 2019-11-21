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
    super(null, null, HttpStatus.NOT_MODIFIED, null);
  }
}
