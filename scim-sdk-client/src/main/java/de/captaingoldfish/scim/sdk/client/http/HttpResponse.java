package de.captaingoldfish.scim.sdk.client.http;

import java.util.Map;

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
@Builder
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
}
