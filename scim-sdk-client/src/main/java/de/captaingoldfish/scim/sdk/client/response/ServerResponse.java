package de.captaingoldfish.scim.sdk.client.response;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
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
  @Getter
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
   * the plaintext response
   */
  private String responseBody;

  /**
   * the http status code of the response
   */
  @Getter
  private Integer httpStatus;

  /**
   * if this response is a valid scim response
   */
  private Boolean validScimResponse;

  /**
   * the expected response type
   */
  @Getter
  private Class<T> type;

  /**
   * an implementation provided by a request builder that tells this response if the response body may be parsed
   * into a resource object
   */
  @Getter
  private Function<HttpResponse, Boolean> isResponseParseable;

  /**
   * these headers are expected within the response in order to make sure that the content-type of a response
   * matches a scim response
   */
  @Getter
  private Map<String, String> requiredResponseHeaders;

  public ServerResponse(HttpResponse httpResponse,
                        boolean expectedResponseCode,
                        Class<T> type,
                        Function<HttpResponse, Boolean> isResponseParseable,
                        Map<String, String> requiredResponseHeaders)
  {
    this.httpResponse = httpResponse;
    this.httpStatus = httpResponse.getHttpStatusCode();
    this.requiredResponseHeaders = requiredResponseHeaders;
    this.success = expectedResponseCode && isValidScimResponse();
    this.type = type;
    this.isResponseParseable = isResponseParseable;
  }

  public ServerResponse(HttpResponse httpResponse, boolean success, T resource)
  {
    this.httpResponse = httpResponse;
    this.httpStatus = httpResponse.getHttpStatusCode();
    this.success = success;
    this.resource = resource;
    this.isResponseParseable = response -> false; // should never be called
  }

  public ServerResponse(boolean success,
                        String responseBody,
                        Boolean validScimResponse,
                        Integer httpStatus,
                        Class<T> type,
                        Function<HttpResponse, Boolean> isResponseParseable,
                        Map<String, String> requiredResponseHeaders)
  {
    this(null, success, responseBody, validScimResponse, httpStatus, type, isResponseParseable,
         requiredResponseHeaders);
  }

  public ServerResponse(HttpResponse httpResponse,
                        boolean success,
                        String responseBody,
                        Boolean validScimResponse,
                        Integer httpStatus,
                        Class<T> type,
                        Function<HttpResponse, Boolean> isResponseParseable,
                        Map<String, String> requiredResponseHeaders)
  {
    this.httpResponse = httpResponse;
    this.success = success;
    this.resource = null;
    this.errorResponse = null;
    this.responseBody = responseBody;
    this.httpStatus = httpStatus;
    this.validScimResponse = validScimResponse;
    this.type = type;
    this.isResponseParseable = isResponseParseable;
    this.requiredResponseHeaders = requiredResponseHeaders;
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
    if (resource != null)
    {
      return resource;
    }
    boolean isSuccessResponse = success && StringUtils.isNotBlank(getResponseBody()) && isValidScimResponse();
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
      validScimResponse = doesHeaderMapContain(getHttpHeaders(), requiredResponseHeaders)
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
    if (responseBody == null)
    {
      responseBody = httpResponse.getResponseBody();
    }
    return responseBody;
  }

  /**
   * checks (with a case-insensitive check on the given header name) that the value is equal to the expected
   * value
   *
   * @param headerName the header name that should be present
   * @param expectedValue the value that should be found under the given header
   * @return true if the header exists, false else
   */
  private boolean doesHeaderMapContain(Map<String, String> httpHeaders, Map<String, String> expectedHttpHeaders)
  {
    boolean allHeadersPresent = true;
    for ( Map.Entry<String, String> keyValue : expectedHttpHeaders.entrySet() )
    {
      String headerName = keyValue.getKey();
      String expectedValue = keyValue.getValue();

      List<String> headerNameList = httpHeaders.keySet()
                                               .stream()
                                               .filter(name -> name.equalsIgnoreCase(headerName))
                                               .collect(Collectors.toList());
      if (headerNameList.size() > 1)
      {
        log.info("Could not validate header value for duplicate headerName found in response: {} -> {}",
                 headerName,
                 String.join(", ", headerNameList));
      }
      boolean isHeaderPresent = headerNameList.size() == 1
                                && Strings.CI.startsWith(httpHeaders.get(headerNameList.get(0)), expectedValue);
      if (isHeaderPresent)
      {
        log.trace("Successfully validated {} header with value '{}'", headerName, expectedValue);
      }
      else
      {
        log.info("Expected header {} was not found in response '{}'", headerName, expectedValue);
        allHeadersPresent = false;
      }
    }
    return allHeadersPresent;
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
