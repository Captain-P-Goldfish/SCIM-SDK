package de.captaingoldfish.scim.sdk.client.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import lombok.Builder;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 20:39 <br>
 * <br>
 * represents a response object that will be returned by the {@link ScimHttpClient} if a response from a
 * server was received
 */
@Getter
public class HttpResponse
{

  /**
   * the status code of the response
   */
  private int httpStatusCode;

  /**
   * the body of the response
   */
  private String responseBody;

  /**
   * the headers of the response
   */
  private Map<String, String> responseHeaders;

  /**
   * Some implementations tend to modify the header names causing issues when trying to read the headers in the
   * response. Therefore, we will store each header also with its lower-case key representation.
   */
  private Map<String, String> lowercaseResponseHeaders;

  @Builder
  public HttpResponse(int httpStatusCode, String responseBody, Map<String, String> responseHeaders)
  {
    this.httpStatusCode = httpStatusCode;
    this.responseBody = responseBody;
    setResponseHeaders(responseHeaders);
  }

  /**
   * @see #responseHeaders
   */
  public void setResponseHeaders(Map<String, String> responseHeaders)
  {
    this.responseHeaders = Optional.ofNullable(responseHeaders).orElseGet(HashMap::new);
    this.lowercaseResponseHeaders = new HashMap<>();
    this.responseHeaders.forEach((key, value) -> {
      this.lowercaseResponseHeaders.put(StringUtils.toRootLowerCase(key), value);
    });
  }

  /**
   * Some implementations tend to modify the header names causing issues when trying to read the headers in the
   * response. Therefore, we will iterate here over the headers and compare their lower case representations
   *
   * @param headerKey the header key to search for
   * @return the header value or null if not found
   */
  public Optional<String> getResponseHeader(String headerKey)
  {
    return Optional.ofNullable(Optional.ofNullable(responseHeaders.get(headerKey)).orElseGet(() -> {
      return lowercaseResponseHeaders.get(StringUtils.toRootLowerCase(headerKey));
    }));
  }
}
