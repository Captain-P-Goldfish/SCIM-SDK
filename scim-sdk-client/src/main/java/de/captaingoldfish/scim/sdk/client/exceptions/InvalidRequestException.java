package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 10.12.2019 - 12:46 <br>
 * <br>
 */
public class InvalidRequestException extends RuntimeException
{

  public InvalidRequestException(String message)
  {
    super(message);
  }

  public InvalidRequestException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
