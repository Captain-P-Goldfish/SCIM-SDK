package de.gold.scim.response;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.HttpHeader;
import de.gold.scim.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 20:09 <br>
 * <br>
 * represents an update response object
 */
public class UpdateResponse extends ScimResponse
{

  public UpdateResponse(JsonNode responseNode, String location)
  {
    super(responseNode);
    getHttpHeaders().put(HttpHeader.LOCATION_HEADER, location);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return HttpStatus.OK;
  }
}
