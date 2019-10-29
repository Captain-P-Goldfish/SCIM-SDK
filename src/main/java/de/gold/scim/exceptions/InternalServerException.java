package de.gold.scim.exceptions;

import de.gold.scim.constants.HttpStatus;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:14 <br>
 * <br>
 */
public class InternalServerException extends ScimException
{

  @Builder
  public InternalServerException(String message, Throwable cause, String scimType)
  {
    super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, scimType);
  }
}
