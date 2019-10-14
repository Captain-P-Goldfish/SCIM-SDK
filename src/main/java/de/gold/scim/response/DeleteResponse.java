package de.gold.scim.response;

import de.gold.scim.constants.HttpStatus;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 20:09 <br>
 * <br>
 * represents an update response object
 */
@NoArgsConstructor
public class DeleteResponse extends ScimResponse
{

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return HttpStatus.SC_NO_CONTENT;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toJsonDocument()
  {
    return null;
  }
}
