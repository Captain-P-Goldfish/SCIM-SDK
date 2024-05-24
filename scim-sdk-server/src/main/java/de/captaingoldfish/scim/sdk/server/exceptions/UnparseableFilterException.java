package de.captaingoldfish.scim.sdk.server.exceptions;

/**
 * @author Pascal Knueppel
 * @since 24.05.2024
 */
public class UnparseableFilterException extends RuntimeException
{

  public UnparseableFilterException(String message)
  {
    super(message);
  }
}
