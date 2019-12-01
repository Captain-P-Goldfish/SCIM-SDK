package de.captaingoldfish.scim.sdk.common.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 15:19 <br>
 * <br>
 */
public class DocumentValidationException extends ScimException
{

  public DocumentValidationException(String message, Integer status, String scimType)
  {
    super(message, null, status, scimType);
  }

  public DocumentValidationException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause, status, scimType);
  }
}
