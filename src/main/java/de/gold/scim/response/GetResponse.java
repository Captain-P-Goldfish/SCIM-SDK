package de.gold.scim.response;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.HttpHeader;
import de.gold.scim.constants.HttpStatus;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 20:09 <br>
 * <br>
 * represents a get response object
 */
public class GetResponse extends ScimResponse
{

  /**
   * the resource that was created
   */
  @Getter
  private JsonNode existingResource;

  public GetResponse(JsonNode existingResource, String location)
  {
    super();
    this.existingResource = existingResource;
    getHttpHeaders().put(HttpHeader.LOCATION_HEADER, location);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return HttpStatus.SC_OK;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toJsonDocument()
  {
    return existingResource.toString();
  }
}
