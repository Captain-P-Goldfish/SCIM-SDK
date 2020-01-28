package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 * if the creation of a SSL context has failed within
 * {@link de.captaingoldfish.scim.sdk.client.http.SSLContextHelper}
 */
public class SslContextCreationFailedException extends RuntimeException
{

  public SslContextCreationFailedException(String message)
  {
    super(message);
  }

  public SslContextCreationFailedException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public SslContextCreationFailedException(Throwable cause)
  {
    super(cause);
  }
}
