package de.captaingoldfish.scim.sdk.common.response;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * this class represents an empty patch-response with a http-status of 204
 *
 * @author Pascal Knueppel
 * @since 27.01.2024
 */
public class EmptyPatchResponse extends ScimResponse
{

  public EmptyPatchResponse(String location)
  {
    getHttpHeaders().put(HttpHeader.LOCATION_HEADER, location);
  }

  @Override
  public int getHttpStatus()
  {
    return HttpStatus.NO_CONTENT;
  }
}
