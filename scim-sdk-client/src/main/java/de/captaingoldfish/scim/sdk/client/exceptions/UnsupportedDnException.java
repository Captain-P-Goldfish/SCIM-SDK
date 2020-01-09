package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 * This exception is thrown if a DN contains illegal or rather unexpected characters
 */
public class UnsupportedDnException extends RuntimeException
{


  public UnsupportedDnException(String message, String dn)
  {
    super(message + ": " + dn);
  }

  public UnsupportedDnException(String message, String dn, String supportedCharacterSet)
  {
    super(message + ": " + dn + "\n" + "supported characters are: " + supportedCharacterSet);
  }

  public UnsupportedDnException(String message, String dn, Throwable cause)
  {
    super(message + ": " + dn, cause);
  }

  public UnsupportedDnException(String message, String dn, String supportedCharacterSet, Throwable cause)
  {
    super(message + ": " + dn + "\n" + "supported characters are: " + supportedCharacterSet, cause);
  }
}
