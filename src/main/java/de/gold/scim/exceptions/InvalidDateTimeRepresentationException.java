package de.gold.scim.exceptions;

import de.gold.scim.utils.HttpStatus;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 29.09.2019 - 21:49 <br>
 * <br>
 */
public class InvalidDateTimeRepresentationException extends ScimException
{

  @Builder
  public InvalidDateTimeRepresentationException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status == null ? HttpStatus.SC_BAD_REQUEST : status, scimType);
  }
}
