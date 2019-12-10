package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class ConnectRuntimeException extends IORuntimeException
{

  public ConnectRuntimeException(String message)
  {
    super(message);
  }

  public ConnectRuntimeException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public ConnectRuntimeException(Throwable cause)
  {
    super(cause);
  }
}
