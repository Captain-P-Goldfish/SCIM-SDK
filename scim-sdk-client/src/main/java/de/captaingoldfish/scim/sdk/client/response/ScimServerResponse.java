package de.captaingoldfish.scim.sdk.client.response;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.client.constants.ResponseType;
import de.captaingoldfish.scim.sdk.common.exceptions.ResponseException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;
import de.captaingoldfish.scim.sdk.common.response.DeleteResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.GetResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.response.UpdateResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.Builder;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 10:16 <br>
 * <br>
 * holds the response from a service provider request
 */
public class ScimServerResponse<T extends ResourceNode>
{

  /**
   * the response send by the scim service provider
   */
  @Getter
  private ScimResponse scimResponse;

  /**
   * the expected resource from the response
   */
  private T resource;

  /**
   * the expected resource type into which the response should be cast e.g. User or Group
   */
  private Class<T> resourceType;

  /**
   * the type of response the client received from the server
   */
  @Getter
  private ResponseType responseType;

  /**
   * this exception will be parsed from the response if the response from the server was an error
   */
  private ResponseException responseException;

  /**
   * holds the http response status from the server
   */
  private Integer responseStatus;

  @Builder
  public ScimServerResponse(ScimResponse scimResponse, Class<T> responseEntityType, Integer responseStatus)
  {
    this.scimResponse = StringUtils.isBlank(scimResponse.toString()) ? null : scimResponse;
    this.resourceType = responseEntityType;
    this.responseType = getResponseType(scimResponse);
    this.responseStatus = responseStatus;
  }

  /**
   * @return the expected resource type
   */
  public Optional<T> getResource()
  {
    if (ResponseType.ERROR.equals(responseType))
    {
      return Optional.empty();
    }
    if (resource != null)
    {
      return Optional.of(resource);
    }
    if (responseType == null)
    {
      throw new IllegalStateException("no response type was set cannot translate response into a resource");
    }
    this.resource = JsonHelper.copyResourceToObject(scimResponse, resourceType);
    return Optional.of(resource);
  }

  /**
   * turns the error response into an exception type
   * 
   * @return the exception or an empty if the response is not an error
   */
  public Optional<ResponseException> getErrorResponse()
  {
    if (responseException != null)
    {
      return Optional.of(responseException);
    }
    if (!ResponseType.ERROR.equals(responseType))
    {
      return Optional.empty();
    }
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    return Optional.of(new ResponseException(errorResponse.getDetail().orElse(null), errorResponse.getStatus(),
                                             errorResponse.getScimType().orElse(null)));
  }

  /**
   * @return the http headers from the response
   */
  public Map<String, String> getHttpHeaders()
  {
    return scimResponse.getHttpHeaders();
  }

  /**
   * @return the http status of the response
   */
  public int getHttpStatus()
  {
    return responseStatus;
  }

  /**
   * will check which type of response the server returned and will set the member accordingly
   */
  private ResponseType getResponseType(ScimResponse scimResponse)
  {
    if (scimResponse instanceof ErrorResponse)
    {
      return ResponseType.ERROR;
    }
    if (scimResponse instanceof CreateResponse)
    {
      return ResponseType.CREATE;
    }
    if (scimResponse instanceof GetResponse)
    {
      return ResponseType.READ;
    }
    if (scimResponse instanceof UpdateResponse)
    {
      return ResponseType.UPDATE;
    }
    if (scimResponse instanceof DeleteResponse)
    {
      return ResponseType.DELETE;
    }
    if (scimResponse instanceof ListResponse)
    {
      return ResponseType.LIST;
    }
    if (scimResponse instanceof BulkResponse)
    {
      return ResponseType.BULK;
    }
    throw new IllegalStateException("unreachable statement");
  }


}
