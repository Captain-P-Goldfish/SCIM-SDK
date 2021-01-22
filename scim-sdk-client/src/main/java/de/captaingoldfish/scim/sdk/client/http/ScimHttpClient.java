package de.captaingoldfish.scim.sdk.client.http;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.exceptions.ConnectTimeoutRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.IORuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.SSLHandshakeRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.SocketTimeoutRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.UnknownHostRuntimeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


// @formatter:off
/**
 * author: Pascal Knueppel<br>
 * created at: 09.12.2019 - 12:26 <br>
 * <br>
 * will provide a service for creating pre-configured apache {@link CloseableHttpClient}s<br>
 * the configuration added to the created clients are the following
 * <ol>
 *   <li>proxy authentication for a configured proxy (see {@link ProxyHelper})</li>
 *   <li>a pre-configured {@link SSLContext}. Depends on the specific declarated bean provided by the developer</li>
 * </ol>
 */
// @formatter:on
@Slf4j
public class ScimHttpClient implements Closeable
{

  /**
   * Number by which timeout variables are multiplied.
   */
  @Getter
  private static final int TIMEOUT_MILLIS = 1000;

  /**
   * reads the timeout in seconds for setting up a connection<br>
   * <b>default: </b>3
   */
  @Getter
  private ScimClientConfig scimClientConfig;

  /**
   * used to send requests to the server
   */
  private CloseableHttpClient httpClient;

  public ScimHttpClient(ScimClientConfig scimClientConfig)
  {
    this.scimClientConfig = scimClientConfig;
  }

  /**
   * translates an apache {@link CloseableHttpResponse} to an {@link HttpResponse} object
   *
   * @param response the apache http response
   * @return the {@link HttpResponse} representation
   * @throws IOException if the inputstream of the response body could not be read
   */
  private static HttpResponse toResponse(CloseableHttpResponse response) throws IOException
  {
    Map<String, String> headers = new HashMap<>();
    Arrays.stream(response.getAllHeaders()).forEach(header -> headers.put(header.getName(), header.getValue()));
    return HttpResponse.builder()
                       .httpStatusCode(response.getStatusLine().getStatusCode())
                       .responseBody(response.getEntity() == null ? null
                         : IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8))
                       .responseHeaders(headers)
                       .build();
  }

  /**
   * this method generates a http-client instance and will set the ssl-context
   *
   * @return a http-client instance
   */
  public CloseableHttpClient getHttpClient()
  {
    if (httpClient != null)
    {
      return httpClient;
    }
    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    CredentialsProvider credentialsProvider = null;
    if (scimClientConfig.getProxy() != null && scimClientConfig.getProxy().isProxySet())
    {
      credentialsProvider = scimClientConfig.getProxy().getProxyCredentials();
    }
    clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    if (scimClientConfig.getClientAuth() != null || scimClientConfig.getTruststore() != null)
    {
      clientBuilder.setSSLContext(SSLContextHelper.getSslContext(scimClientConfig.getClientAuth(),
                                                                 scimClientConfig.getTruststore()));
    }

    clientBuilder.setConnectionReuseStrategy((response, context) -> false);
    if (scimClientConfig.getHostnameVerifier() != null)
    {
      clientBuilder.setSSLHostnameVerifier(scimClientConfig.getHostnameVerifier());
    }
    clientBuilder.setDefaultRequestConfig(getRequestConfig());
    if (scimClientConfig.getConfigManipulator() != null)
    {
      scimClientConfig.getConfigManipulator().modifyHttpClientConfig(clientBuilder);
    }
    httpClient = clientBuilder.build();
    return httpClient;
  }

  /**
   * will configure the apache http-client
   *
   * @return the original request with an extended configuration
   */
  public RequestConfig getRequestConfig()
  {
    RequestConfig.Builder configBuilder;
    if (scimClientConfig.getProxy() == null)
    {
      configBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
    }
    else
    {
      RequestConfig proxyConfig = scimClientConfig.getProxy().getProxyConfig();
      configBuilder = RequestConfig.copy(proxyConfig);
    }
    if (scimClientConfig.getConnectTimeout() > 0)
    {
      configBuilder.setConnectTimeout(scimClientConfig.getConnectTimeout() * TIMEOUT_MILLIS);
      log.debug("connection timeout '{}' seconds", scimClientConfig.getConnectTimeout());
    }
    if (scimClientConfig.getSocketTimeout() > 0)
    {
      configBuilder.setSocketTimeout(scimClientConfig.getSocketTimeout() * TIMEOUT_MILLIS);
      log.debug("socket timeout '{}' seconds", scimClientConfig.getSocketTimeout());
    }
    if (scimClientConfig.getRequestTimeout() > 0)
    {
      configBuilder.setConnectionRequestTimeout(scimClientConfig.getRequestTimeout() * TIMEOUT_MILLIS);
      log.debug("request timeout '{}' seconds", scimClientConfig.getRequestTimeout());
    }

    if (scimClientConfig.getConfigManipulator() != null)
    {
      scimClientConfig.getConfigManipulator().modifyRequestConfig(configBuilder);
    }
    return configBuilder.build();
  }

  /**
   * this method will send the request with the apache http client and will also handle {@link IOException}s and
   * wrap them into {@link IORuntimeException}s
   *
   * @param uriRequest the request that should be send
   * @return the response of the server
   * @throws SSLHandshakeRuntimeException in case that the server represented a certificate that is not trusted
   * @throws ConnectRuntimeException happens if the server does not respond (wrong URL wrong port or something
   *           else)
   * @throws ConnectTimeoutRuntimeException if the server took too long to establish a connection
   * @throws SocketTimeoutRuntimeException if the server took too long to send the response
   * @throws IORuntimeException base exception that represents all previous mentioned exceptions
   */
  public HttpResponse sendRequest(HttpUriRequest uriRequest)
  {
    if (httpClient == null)
    {
      httpClient = getHttpClient();
    }
    if (log.isTraceEnabled())
    {
      log.trace("sending http request: \n\tmethod: {}\n\turi: {}",
                uriRequest.getMethod(),
                uriRequest.getURI().toString());
    }
    try (CloseableHttpResponse response = httpClient.execute(uriRequest))
    {
      return toResponse(response);
    }
    catch (SSLHandshakeException ex)
    {
      throw new SSLHandshakeRuntimeException("handshake error during connection setup", ex);
    }
    catch (ConnectTimeoutException ex)
    {
      throw new ConnectTimeoutRuntimeException("connection timeout after '" + scimClientConfig.getConnectTimeout()
                                               + "' seconds", ex);
    }
    catch (SocketTimeoutException ex)
    {
      throw new SocketTimeoutRuntimeException("socket timeout after '" + scimClientConfig.getSocketTimeout()
                                              + "' seconds", ex);
    }
    catch (UnknownHostException ex)
    {
      throw new UnknownHostRuntimeException("could not find host '" + uriRequest.getURI().getHost() + "'", ex);
    }
    catch (IOException ex)
    {
      throw new IORuntimeException("communication with server failed", ex);
    }
  }

  /**
   * will close the apache http client
   */
  @Override
  public void close()
  {
    if (httpClient == null)
    {
      return;
    }
    try
    {
      httpClient.close();
    }
    catch (IOException e)
    {
      // will never happen since the implementation of httpclient is an InternalHttpClient from apache
      log.error(e.getMessage(), e);
    }
    httpClient = null;
  }
}
