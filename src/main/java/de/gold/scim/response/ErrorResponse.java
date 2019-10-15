package de.gold.scim.response;

import de.gold.scim.constants.HttpStatus;
import de.gold.scim.exceptions.ScimException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 20:58 <br>
 * <br>
 * represents a SCIM error response
 */
@Slf4j
public class ErrorResponse extends ScimResponse
{

  /**
   * the exception that should be turned into a SCIM error response
   */
  @Getter
  private ScimException scimException;

  public ErrorResponse(ScimException scimException)
  {
    this.scimException = scimException;
    if (HttpStatus.SC_INTERNAL_SERVER_ERROR == getHttpStatus())
    {
      log.error(scimException.getMessage(), scimException);
    }
    else
    {
      log.debug(scimException.getMessage(), scimException);
    }
  }

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
