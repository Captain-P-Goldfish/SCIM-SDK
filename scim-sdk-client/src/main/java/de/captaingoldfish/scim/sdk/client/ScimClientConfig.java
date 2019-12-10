package de.captaingoldfish.scim.sdk.client;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 10.12.2019 - 13:39 <br>
 * <br>
 */
@Getter
@NoArgsConstructor
public class ScimClientConfig
{

  /**
   * request timeout in seconds
   */
  private int requestTimeout = 5;

  /**
   * socket timeout in seconds
   */
  private int socketTimeout = 5;

  /**
   * connect timeout in seconds
   */
  private int connectTimeout = 5;

  @Builder
  public ScimClientConfig(int requestTimeout, int socketTimeout, int connectTimeout)
  {
    this.requestTimeout = requestTimeout;
    this.socketTimeout = socketTimeout;
    this.connectTimeout = connectTimeout;
  }
}
