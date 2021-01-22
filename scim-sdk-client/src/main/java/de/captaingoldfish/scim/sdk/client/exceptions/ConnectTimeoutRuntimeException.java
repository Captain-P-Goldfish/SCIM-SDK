package de.captaingoldfish.scim.sdk.client.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class ConnectTimeoutRuntimeException extends IORuntimeException
{

  public ConnectTimeoutRuntimeException(String message, Throwable cause)
  {
    super(message, cause);
  }

}
