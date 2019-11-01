package de.gold.scim.common.exceptions;

import de.gold.scim.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 17:41 <br>
 * <br>
 */
public class ResourceNotFoundException extends ScimException
{

  public ResourceNotFoundException(String message, Throwable cause, String scimType)
  {
    super(message, cause, HttpStatus.NOT_FOUND, scimType);
  }
}
