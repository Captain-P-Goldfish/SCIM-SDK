package de.captaingoldfish.scim.sdk.client.http;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;


/**
 * author Pascal Knueppel <br>
 * created at: 09.01.2020 - 10:27 <br>
 * <br>
 * an optional interface that may be used to manipulate the apache http client configuration before the http
 * client is created
 */
public interface ConfigManipulator
{

  /**
   * an optional method that may be used to manipulate the apache http client configuration before the http
   * client is created
   *
   * @param requestConfig the request configuration from apache
   */
  public void modifyRequestConfig(RequestConfig.Builder requestConfig);

  /**
   * an optional method that may be used to manipulate the apache request configuration before the http client
   * is created
   *
   * @param clientBuilder the http client builder from apache
   */
  public void modifyHttpClientConfig(HttpClientBuilder clientBuilder);
}
