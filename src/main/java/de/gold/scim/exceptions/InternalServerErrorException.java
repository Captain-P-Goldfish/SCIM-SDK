package de.gold.scim.exceptions;

import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:14 <br>
 * <br>
 */
public class InternalServerErrorException extends ScimException
{

  @Builder
  public InternalServerErrorException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
