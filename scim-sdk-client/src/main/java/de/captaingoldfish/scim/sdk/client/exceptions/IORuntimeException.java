package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 * to wrap any {@link java.io.IOException} wihtin a runtime exception
 */
public class IORuntimeException extends RuntimeException
{

  public IORuntimeException(String message, Throwable cause)
  {
    super(message, cause);
  }

}
