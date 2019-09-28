package de.gold.scim.exceptions;

import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 00:07 <br>
 * <br>
 * a simple runtime IO exception
 */
public class IOException extends ScimException
{

  @Builder
  public IOException(String message, Throwable cause, String detail, int status, String scimType)
  {
    super(message, cause, detail, status, scimType);
  }
}
