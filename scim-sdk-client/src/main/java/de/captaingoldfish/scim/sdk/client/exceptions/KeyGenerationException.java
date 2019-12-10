package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class KeyGenerationException extends RuntimeException
{

  public KeyGenerationException(String message)
  {
    super(message);
  }

  public KeyGenerationException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public KeyGenerationException(Throwable cause)
  {
    super(cause);
  }

}
