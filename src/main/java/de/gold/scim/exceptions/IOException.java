package de.gold.scim.exceptions;

/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 00:07 <br>
 * <br>
 * a simple runtime IO exception
 */
public class IOException extends RuntimeException
{

  public IOException(String message)
  {
    super(message);
  }

  public IOException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public IOException(Throwable cause)
  {
    super(cause);
  }
}
