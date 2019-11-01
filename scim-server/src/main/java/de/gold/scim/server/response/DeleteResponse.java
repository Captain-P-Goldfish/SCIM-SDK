package de.gold.scim.server.response;

import de.gold.scim.server.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 20:09 <br>
 * <br>
 * represents an update response object
 */
public class DeleteResponse extends ScimResponse
{

  public DeleteResponse()
  {
    super(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return HttpStatus.NO_CONTENT;
  }
}
