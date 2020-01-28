package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class UriNotValidException extends RuntimeException
{

  public UriNotValidException(String message)
  {
    super(message);
  }

  public UriNotValidException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public UriNotValidException(Throwable cause)
  {
    super(cause);
  }
}
