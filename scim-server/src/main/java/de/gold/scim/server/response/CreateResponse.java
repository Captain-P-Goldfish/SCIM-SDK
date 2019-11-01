package de.gold.scim.server.response;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.server.constants.HttpHeader;
import de.gold.scim.server.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 14:49 <br>
 * <br>
 * represents a creation response
 */
public class CreateResponse extends ScimResponse
{

  public CreateResponse(JsonNode responseNode, String location)
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
    return HttpStatus.CREATED;
  }
}
