package de.gold.scim.exceptions;

import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 15:19 <br>
 * <br>
 */
public class InvalidSchemaException extends ScimException
{

  @Builder
  public InvalidSchemaException(String message, Throwable cause, String detail, int status, String scimType)
  {
    super(message, cause, detail, status, scimType);
  }
}
