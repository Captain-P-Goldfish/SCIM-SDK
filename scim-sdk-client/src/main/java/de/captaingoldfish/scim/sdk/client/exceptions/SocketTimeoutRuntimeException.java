package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class SocketTimeoutRuntimeException extends IORuntimeException
{

  public SocketTimeoutRuntimeException(String message)
  {
    super(message);
  }

  public SocketTimeoutRuntimeException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public SocketTimeoutRuntimeException(Throwable cause)
  {
    super(cause);
  }
}
