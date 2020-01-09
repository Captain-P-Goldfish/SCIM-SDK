package de.captaingoldfish.scim.sdk.client.http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import lombok.Builder;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 15:51 <br>
 * <br>
 * wrapper class for basic auth details
 */
@Getter
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

  public BasicAuth build()
  {
    if (StringUtils.isBlank(username))
    {
      return null;
    }
    return new BasicAuth(username, password);
  }

  /**
   * generates a basic authentication header value
   */
  public String getAuthorizationHeaderValue()
  {
    byte[] encoded = (username + ":" + password).getBytes(StandardCharsets.UTF_8);
    return "Basic " + Base64.getEncoder().encodeToString(encoded);
  }

}
