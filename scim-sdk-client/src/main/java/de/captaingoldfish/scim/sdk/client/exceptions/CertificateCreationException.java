package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class CertificateCreationException extends RuntimeException
{

  public CertificateCreationException(String message)
  {
    super(message);
  }

  public CertificateCreationException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public CertificateCreationException(Throwable cause)
  {
    super(cause);
  }
}
