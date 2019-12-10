package de.captaingoldfish.scim.sdk.client.http;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * author: Pascal Knueppel <br>
 * created at: 09.12.2019 - 12:17 <br>
 * <br>
 * this helper can be used with the apache http-client to create an http client that will use a proxy.
 *
 * @see ScimHttpClient
 */
@Slf4j
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ProxyHelper
{

  /**
   * the host under which the proxy can be reached
   */
  private String systemProxyHost;

  /**
   * the port on the host under which the proxy can be reached
   */
  private int systemProxyPort;

  /**
   * optional proxy username in case of proxy authentication
   */
  private String systemProxyUsername;

  /**
   * optional proxy password in case of proxy authentication
   */
  private String systemProxyPassword;


  /**
   * @return true if a proxy configuration is present, false else
   */
  public boolean isProxySet()
  {
    return StringUtils.isNotBlank(systemProxyHost) && systemProxyPort != 0;
  }

  /**
   * @return a basic credentials provider that will be used for proxy authentication.
   */
  public CredentialsProvider getProxyCredentials()
  {
    if (StringUtils.isBlank(getSystemProxyUsername()))
    {
      log.trace("proxy username is empty cannot create client credentials");
      return null;
    }
    if (getSystemProxyPassword() == null)
    {
      log.debug("proxy password is null cannot create client credentials");
      return null;
    }
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(new AuthScope(getSystemProxyHost(), systemProxyPort),
                                       new UsernamePasswordCredentials(getSystemProxyUsername(),
                                                                       getSystemProxyPassword()));
    return credentialsProvider;
  }

  /**
   * will give back a request-config with the proxy settings based on the configuration-poperties
   *
   * @return a new config with the configured proxy or the default-config
   * @see #getSystemProxyHost()
   * @see #getSystemProxyPort()
   * @see #getSystemProxyUsername()
   * @see #getSystemProxyPassword()
   */
  public RequestConfig getProxyConfig()
  {
    if (StringUtils.isNotBlank(systemProxyHost))
    {
      HttpHost systemProxy = new HttpHost(systemProxyHost, systemProxyPort);
      log.debug("using proxy configuration: {}", systemProxy);
      return RequestConfig.custom().setProxy(systemProxy).build();
    }
    return RequestConfig.DEFAULT;
  }

  /**
   * @return the currently configured proxy settings as string in the form "localhost:8888"
   */
  public String getProxyAddress()
  {
    return getSystemProxyHost() + ":" + getSystemProxyPort();
  }

  /**
   * overriding lombok builder class
   */
  public static class ProxyHelperBuilder
  {

    /**
     * public default constructor to enable builder-inheritance
     */
    public ProxyHelperBuilder()
    {
      super();
    }

    /**
     * sets the proxy port
     */
    public ProxyHelperBuilder systemProxyPort(int systemProxyPort)
    {
      this.systemProxyPort = systemProxyPort; // NOPMD
      return this;
    }

    /**
     * will parse the given string to the desired port
     *
     * @param systemProxyPort the port as string
     * @throws IllegalArgumentException if the port is empty or does not match a valid number
     */
    public ProxyHelperBuilder systemProxyPort(String systemProxyPort)
    {
      if (StringUtils.isBlank(systemProxyPort) || !systemProxyPort.matches("\\d+"))
      {
        throw new IllegalArgumentException("Port must not be empty and must contain only numbers but is: "
                                           + systemProxyPort + "\n Set Port to '0' if proxy is not required.");
      }
      this.systemProxyPort = Integer.parseInt(systemProxyPort); // NOPMD
      return this;
    }
  }
}
