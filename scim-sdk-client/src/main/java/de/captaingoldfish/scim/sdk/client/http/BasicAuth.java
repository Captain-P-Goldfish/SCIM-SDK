package de.captaingoldfish.scim.sdk.client.http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 15:51 <br>
 * <br>
 * wrapper class for basic auth details
 */
@Builder
public class BasicAuth
{

  /**
   * the username for basic authentication
   */
  private String username;

  /**
   * the password for basic authentication
   */
  private String password;

  /**
   * generates a basic authentication header value
   */
  public String getAuthorizationHeaderValue()
  {
    byte[] encoded = (Optional.ofNullable(username).orElse("") + ":"
                      + Optional.ofNullable(password).orElse("")).getBytes(StandardCharsets.UTF_8);
    return "Basic " + Base64.getEncoder().encodeToString(encoded);
  }

}
