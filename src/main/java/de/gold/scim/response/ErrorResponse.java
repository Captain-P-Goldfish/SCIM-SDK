package de.gold.scim.response;

import de.gold.scim.exceptions.ScimException;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 20:58 <br>
 * <br>
 * represents a SCIM error response
 */
@AllArgsConstructor
public class ErrorResponse extends ScimResponse
{

  /**
   * the exception that should be turned into a SCIM error response
   */
  @Getter
  private ScimException scimException;

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return scimException.getStatus();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toJsonDocument()
  {
    // TODO
    return null;
  }
}
