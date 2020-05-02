package de.captaingoldfish.scim.sdk.client.response;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * represents a response from the server<br>
 * <br>
 * create at: 01.05.2020
 *
 * @author Pascal Knüppel
 */
@Slf4j
public class ServerResponse<T extends ScimObjectNode>
{

  /**
   * the original http response object
   */
  private final HttpResponse httpResponse;

  /**
   * if the response was succesful
   */
  @Getter
  private final boolean success;

  /**
   * the resource that should represent this response
   */
  private T resource;

  /**
   * will be instantiated if the field {@link #success} is false and {@link #isValidScimResponse()} is true
   */
  private ErrorResponse errorResponse;

  /**
   * if this response is a valid scim response
   */
  private Boolean validScimResponse;

  /**
   * the expected response type
   */
  @Getter(AccessLevel.PROTECTED)
  private Class<T> type;

  /**
   * an implementation provided by a request builder that tells this response if the response body may be parsed
   * into a resource object
   */
  private Function<HttpResponse, Boolean> isResponseParseable;

  public ServerResponse(HttpResponse httpResponse,
                        boolean expectedResponseCode,
                        Class<T> type,
                        Function<HttpResponse, Boolean> isResponseParseable)
  {
    this.httpResponse = httpResponse;
    this.success = expectedResponseCode && isValidScimResponse();
    this.type = type;
    this.isResponseParseable = isResponseParseable;
  }

  /**
   * tries to resolve the returned resource as scim object
   *
   * @return the parsed resource that should be returned
   * @throws de.captaingoldfish.scim.sdk.common.exceptions.IOException if the response body is not a valid json
   *           document
   */
  public T getResource()
  {
    boolean isSuccessResponse = resource == null && success && StringUtils.isNotBlank(getResponseBody())
                                && isValidScimResponse();
    Boolean result = isResponseParseable.apply(httpResponse);
    boolean isParseable = result != null && result;
    if (isParseable || isSuccessResponse)
    {
      resource = getResource(type);
    }
    return resource;
  }

  /**
   * tries to resolve the returned resource as scim object
   *
   * @param responseType the type of the node which might be of type
   *          {@link de.captaingoldfish.scim.sdk.common.resources.User},
   *          {@link de.captaingoldfish.scim.sdk.common.resources.Group},
   *          {@link de.captaingoldfish.scim.sdk.common.response.BulkResponse} or any other type that extends
   *          {@link ScimObjectNode}
   * @return the parsed resource that should be returned
   * @throws de.captaingoldfish.scim.sdk.common.exceptions.IOException if the response body is not a valid json
   *           document
   */
  public <R extends ScimObjectNode> R getResource(Class<R> responseType)
  {
    return JsonHelper.readJsonDocument(getResponseBody(), responseType);
  }

  /**
   * if this response is a valid scim response
   */
  public boolean isValidScimResponse()
  {
    if (validScimResponse == null)
    {
      validScimResponse = doesHeaderMapContain(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE)
                          && ((getResponseBody() != null && JsonHelper.isValidJson(getResponseBody()))
                              || getResponseBody() == null);
    }
    return validScimResponse;
  }

  /**
   * will be instantiated if the response contains a scim json structure with a schemas-element that contains
   * the value {@link SchemaUris#ERROR_URI}
   */
  public ErrorResponse getErrorResponse()
  {
    if (errorResponse == null && !success && StringUtils.isNotBlank(getResponseBody()) && isValidScimResponse()
        && isUriInSchemasElement(SchemaUris.ERROR_URI))
    {
      errorResponse = JsonHelper.readJsonDocument(getResponseBody(), ErrorResponse.class);
    }
    return errorResponse;
  }

  /**
   * the headers of the response
   */
  public Map<String, String> getHttpHeaders()
  {
    return httpResponse.getResponseHeaders();
  }

  /**
   * the body of the response
   */
  public String getResponseBody()
  {
    return httpResponse.getResponseBody();
  }

  /**
   * the status code of the response
   */
  public int getHttpStatus()
  {
    return httpResponse.getHttpStatusCode();
  }

  /**
   * checks (with a case insensitive check on the given header name) that the value is equal to the expected
   * value
   *
   * @param headerName the header name that should be present
   * @param expectedValue the value that should be found under the given header
   * @return true if the header exists, false else
   */
  private boolean doesHeaderMapContain(String headerName, String expectedValue)
  {
    List<String> headerNameList = getHttpHeaders().keySet()
                                                  .stream()
                                                  .filter(name -> name.equalsIgnoreCase(headerName))
                                                  .collect(Collectors.toList());
    if (headerNameList.size() > 1)
    {
      log.error("could not validate header value for duplicate headerName found in response: {} -> {}",
                headerName,
                String.join(", ", headerNameList));
    }
    return headerNameList.size() == 1
           && StringUtils.startsWithIgnoreCase(getHttpHeaders().get(headerNameList.get(0)), expectedValue);
  }

  /**
   * checks if the given uri is in the schemas element of the current http response body
   *
   * @param uri the schema uri that should be present within the body
   * @return true, if the uri is present false else
   */
  private boolean isUriInSchemasElement(String uri)
  {
    if (!isValidScimResponse() || StringUtils.isBlank(getResponseBody()))
    {
      return false;
    }
    ScimObjectNode scimObjectNode = JsonHelper.readJsonDocument(getResponseBody(), ScimObjectNode.class);
    ArrayNode schemasNode = (ArrayNode)scimObjectNode.get(AttributeNames.RFC7643.SCHEMAS);
    if (schemasNode == null || schemasNode.isEmpty())
    {
      return false;
    }
    for ( JsonNode jsonNode : schemasNode )
    {
      if (uri.equals(jsonNode.textValue()))
      {
        return true;
      }
    }
    return false;
  }
}
