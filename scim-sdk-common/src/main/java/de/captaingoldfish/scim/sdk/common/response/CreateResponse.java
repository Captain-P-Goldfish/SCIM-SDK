package de.captaingoldfish.scim.sdk.common.response;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 14:49 <br>
 * <br>
 * represents a creation response
 */
@NoArgsConstructor
public class CreateResponse extends ScimResponse
{

  public CreateResponse(JsonNode responseNode, String location, Meta meta)
  {
    super(responseNode);
    getHttpHeaders().put(HttpHeader.LOCATION_HEADER, location);
    setETag(meta);
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
