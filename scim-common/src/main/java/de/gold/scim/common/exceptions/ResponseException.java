package de.gold.scim.common.exceptions;

import de.gold.scim.common.response.ErrorResponse;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 23:18 <br>
 * <br>
 * this exception is used for {@link ErrorResponse}s to parse an error response into an exception type
 */
public class ResponseException extends ScimException
{

  public ResponseException(String message, Integer status, String scimType)
  {
    super(message, null, status, scimType);
  }
}
