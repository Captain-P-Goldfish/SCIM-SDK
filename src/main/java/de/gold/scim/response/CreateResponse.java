package de.gold.scim.response;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.HttpHeader;
import de.gold.scim.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 14:49 <br>
 * <br>
 * represents a creation response
 */
public class CreateResponse extends ScimResponse
{

  /**
   * the resource that was created
   */
  private JsonNode createdResource;

  public CreateResponse(JsonNode createdResource, String location)
  {
    super();
    this.createdResource = createdResource;
    getHttpHeaders().put(HttpHeader.LOCATION_HEADER, location);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return HttpStatus.SC_CREATED;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toJsonDocument()
  {
    return createdResource.toString();
  }
}
