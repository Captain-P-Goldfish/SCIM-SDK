package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class KeyStoreCreationFailedException extends RuntimeException
{

  public KeyStoreCreationFailedException(String message)
  {
    super(message);
  }

  public KeyStoreCreationFailedException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public KeyStoreCreationFailedException(Throwable cause)
  {
    super(cause);
  }
}
