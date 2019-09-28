package de.gold.scim.exceptions;

import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 15:19 <br>
 * <br>
 */
public class DocumentValidationException extends ScimException
{

  @Builder
  public DocumentValidationException(String message, Throwable cause, int status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
