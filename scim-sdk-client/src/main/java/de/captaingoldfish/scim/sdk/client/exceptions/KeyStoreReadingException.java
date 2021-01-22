package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 * <br>
 */
public class KeyStoreReadingException extends RuntimeException
{

  public KeyStoreReadingException(String message)
  {
    super(message);
  }

  public KeyStoreReadingException(String message, Throwable cause)
  {
    super(message, cause);
  }

}
