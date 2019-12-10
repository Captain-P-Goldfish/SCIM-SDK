package de.captaingoldfish.scim.sdk.client.response;

import java.util.Map;

import de.captaingoldfish.scim.sdk.client.constants.ResponseType;
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
@Getter
public class ScimServerResponse<T extends ResourceNode>
{

  /**
   * the response send by the scim service provider
   */
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
  private ResponseType responseType;

  @Builder
  public ScimServerResponse(ScimResponse scimResponse, Class<T> responseEntityType)
  {
    this.scimResponse = scimResponse;
    this.resourceType = responseEntityType;
    this.responseType = getResponseType(scimResponse);
  }

  /**
   * @return the expected resource type
   */
  public T getResource()
  {
    if (resource != null)
    {
      return resource;
    }
    if (responseType == null)
    {
      throw new IllegalStateException("no response type was set cannot translate response into a resource");
    }
    this.resource = JsonHelper.copyResourceToObject(scimResponse, resourceType);
    return resource;
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
    return scimResponse.getHttpStatus();
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
