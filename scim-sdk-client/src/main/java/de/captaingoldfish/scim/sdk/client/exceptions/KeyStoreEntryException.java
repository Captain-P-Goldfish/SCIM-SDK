package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 * This Exception will be thrown if an entry could not be added to a keystore
 */
public class KeyStoreEntryException extends RuntimeException
{

  public KeyStoreEntryException(String message)
  {
    super(message);
  }

}
