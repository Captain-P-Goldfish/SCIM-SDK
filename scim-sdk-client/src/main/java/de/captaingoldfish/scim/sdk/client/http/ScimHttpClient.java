package de.captaingoldfish.scim.sdk.client.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import de.captaingoldfish.scim.sdk.client.exceptions.ConnectRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.ConnectTimeoutRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.IORuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.SSLHandshakeRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.SocketTimeoutRuntimeException;
import de.captaingoldfish.scim.sdk.client.exceptions.UriNotValidException;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;
import lombok.Builder;
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
@Builder
public class ScimHttpClient
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
  private int connectTimeout;

  /**
   * reads the timeout in seconds for a request <br>
   * <b>default: </b>3
   */
  private int requestTimeout;

  /**
   * reads the timeout in seconds for a request <br>
   * <b>default: </b>3
   */
  private int socketTimeout;

  /**
   * a helper that will simplify using the proxy settings
   */
  private ProxyHelper proxyHelper;

  /**
   * verimi demands client authentication on the token endpoint and the user-info endpoint
   */
  private KeyStoreWrapper tlsClientAuthenticatonKeystore;

  /**
   * in case that a truststore is used for testing
   */
  private KeyStoreWrapper truststore;

  /**
   * in case that the default hostname verifier should not be used
   */
  private HostnameVerifier hostnameVerifier;

  /**
   * translates an apache {@link CloseableHttpResponse} to an {@link HttpResponse} object
   *
   * @param response the apache http response
   * @return the governikus {@link HttpResponse} representation
   * @throws IOException if the inputstream of the response body could not be read
   */
  private static HttpResponse toResponse(CloseableHttpResponse response) throws IOException
  {
    Map<String, String> headers = new HashMap<>();
    Arrays.stream(response.getAllHeaders()).forEach(header -> headers.put(header.getName(), header.getValue()));
    return HttpResponse.builder()
                       .httpStatusCode(response.getStatusLine().getStatusCode())
                       .responseBody(response.getEntity() == null ? ""
                         : IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8))
                       .responseHeaders(headers)
                       .build();
  }

  /**
   * gets an url object from the given string and throws an exception if the url is not valid
   *
   * @param url the url to which the request should be send
   * @return the url object
   */
  private static URL getUrl(String url)
  {
    try
    {
      return new URL(url);
    }
    catch (MalformedURLException e)
    {
      throw new UriNotValidException("URL '" + url + "' is not a valid URL", e);
    }
  }

  /**
   * gets an url object from the given string and throws an exception if the url is not valid
   *
   * @param url the url to which the request should be send
   * @param parameterBuilder adds the given parameters to the given url as query parameters
   * @return the url object
   */
  private static URL getUrl(String url, ParameterBuilder parameterBuilder)
  {
    try
    {
      URL urlAddress = new URL(url);
      if (parameterBuilder == null || parameterBuilder.build().isEmpty())
      {
        return urlAddress;
      }
      StringBuilder query = new StringBuilder();
      parameterBuilder.build().forEach((key, value) -> {
        query.append("&").append(key);
        if (StringUtils.isNotBlank(value))
        {
          query.append("=").append(value);
        }
      });
      if (StringUtils.contains(url, "?") && !StringUtils.endsWith(url, "?"))
      {
        urlAddress = new URL(url + query.toString());
      }
      else
      {
        urlAddress = new URL(url + "?" + query.substring(1));
      }
      return urlAddress;
    }
    catch (MalformedURLException e)
    {
      throw new UriNotValidException("URL '" + url + "' is not a valid URL", e);
    }
  }

  /**
   * this method generates a http-client instance and will set the ssl-context
   *
   * @return a http-client instance
   */
  public CloseableHttpClient getHttpClient()
  {
    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    if (proxyHelper != null && proxyHelper.isProxySet())
    {
      clientBuilder.setDefaultCredentialsProvider(proxyHelper.getProxyCredentials());
    }
    if (tlsClientAuthenticatonKeystore != null || truststore != null)
    {
      clientBuilder.setSSLContext(SSLContextHelper.getSslContext(tlsClientAuthenticatonKeystore, truststore));
    }

    clientBuilder.setDefaultRequestConfig(getRequestConfig());
    clientBuilder.setConnectionReuseStrategy((response, context) -> false);
    if (hostnameVerifier != null)
    {
      clientBuilder.setSSLHostnameVerifier(hostnameVerifier);
    }
    return clientBuilder.build();
  }

  /**
   * will configure the apache http-client
   *
   * @return the original request with an extended configuration
   */
  public RequestConfig getRequestConfig()
  {
    RequestConfig.Builder configBuilder;
    if (proxyHelper == null)
    {
      configBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
    }
    else
    {
      RequestConfig proxyConfig = proxyHelper.getProxyConfig();
      configBuilder = RequestConfig.copy(proxyConfig);
    }
    if (connectTimeout > 0)
    {
      configBuilder.setConnectTimeout(connectTimeout * TIMEOUT_MILLIS);
      log.debug("connection timeout '{}' seconds", connectTimeout);
    }
    if (socketTimeout > 0)
    {
      configBuilder.setSocketTimeout(socketTimeout * TIMEOUT_MILLIS);
      log.debug("socket timeout '{}' seconds", socketTimeout);
    }
    if (requestTimeout > 0)
    {
      configBuilder.setConnectionRequestTimeout(requestTimeout * TIMEOUT_MILLIS);
      log.debug("request timeout '{}' seconds", requestTimeout);
    }
    return configBuilder.build();
  }

  /**
   * this method will send a simple GET request to the given url
   *
   * @param url the url to send the request to
   * @return the response from the server
   * @throws SSLHandshakeException in case that the server represented a certificate that is not trusted
   * @throws ConnectException happens if the server does not respond (wrong URL wrong port or something else)
   * @throws ConnectTimeoutException if the server took too long to establish a connection
   * @throws SocketTimeoutException if the server took too long to send the response
   * @throws IOException base exception that represents all previous mentioned exceptions
   */
  public HttpResponse sendGet(String url)
  {
    return sendGet(url, null);
  }

  /**
   * this method will send a simple GET request to the given url
   *
   * @param url the url to send the request to
   * @return the response from the server
   * @throws SSLHandshakeRuntimeException in case that the server represented a certificate that is not trusted
   * @throws ConnectRuntimeException happens if the server does not respond (wrong URL wrong port or something
   *           else)
   * @throws ConnectTimeoutRuntimeException if the server took too long to establish a connection
   * @throws SocketTimeoutRuntimeException if the server took too long to send the response
   * @throws IORuntimeException base exception that represents all previous mentioned exceptions
   */
  public HttpResponse sendGet(String url, ParameterBuilder parameterBuilder, Header... headers)
  {
    URL urlAddress = getUrl(url, parameterBuilder);
    HttpGet httpGet = new HttpGet(urlAddress.toString());
    if (headers != null)
    {
      for ( Header header : headers )
      {
        httpGet.addHeader(header);
      }
    }
    return sendRequest(httpGet);
  }

  /**
   * sends a post request with the given body and the given content type to the given url
   *
   * @param url the url to which the post request should be send
   * @param postBody the body content of the post request
   * @param contentType the content type represented by the post body
   * @return the response from the server
   * @throws SSLHandshakeRuntimeException in case that the server represented a certificate that is not trusted
   * @throws ConnectRuntimeException happens if the server does not respond (wrong URL wrong port or something
   *           else)
   * @throws ConnectTimeoutRuntimeException if the server took too long to establish a connection
   * @throws SocketTimeoutRuntimeException if the server took too long to send the response
   * @throws IORuntimeException base exception that represents all previous mentioned exceptions
   */
  public HttpResponse sendPost(String url, String postBody, ContentType contentType)
  {
    URL urlAddress = getUrl(url);
    HttpPost httpPost = new HttpPost(urlAddress.toString());
    if (StringUtils.isNotBlank(postBody))
    {
      httpPost.setEntity(new StringEntity(postBody, contentType));
    }
    return sendRequest(httpPost);
  }

  /**
   * sends a post request with Content-Type 'application/x-www-form-urlencoded' and the given parameters
   *
   * @param url the url to which the post request should be send
   * @param parameterBuilder the parameters to put into the post body
   * @return the response from the server
   * @throws SSLHandshakeRuntimeException in case that the server represented a certificate that is not trusted
   * @throws ConnectRuntimeException happens if the server does not respond (wrong URL wrong port or something
   *           else)
   * @throws ConnectTimeoutRuntimeException if the server took too long to establish a connection
   * @throws SocketTimeoutRuntimeException if the server took too long to send the response
   * @throws IORuntimeException base exception that represents all previous mentioned exceptions
   */
  public HttpResponse sendPost(String url, ParameterBuilder parameterBuilder, Header... headers)
  {
    URL urlAddress = getUrl(url);
    HttpPost httpPost = new HttpPost(urlAddress.toString());
    if (headers != null)
    {
      for ( Header header : headers )
      {
        httpPost.addHeader(header);
      }
    }
    if (parameterBuilder != null)
    {
      List<NameValuePair> params = new ArrayList<>();
      parameterBuilder.build().forEach((name, value) -> params.add(new BasicNameValuePair(name, value)));
      try
      {
        httpPost.setEntity(new UrlEncodedFormEntity(params));
      }
      catch (UnsupportedEncodingException e)
      {
        throw new IORuntimeException(e.getMessage(), e);
      }
    }
    return sendRequest(httpPost);
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
    try (CloseableHttpClient httpClient = getHttpClient();
      CloseableHttpResponse response = httpClient.execute(uriRequest))
    {
      return toResponse(response);
    }
    catch (SSLHandshakeException ex)
    {
      throw new SSLHandshakeRuntimeException(ex.getMessage(), ex);
    }
    catch (ConnectException ex)
    {
      throw new ConnectRuntimeException(ex.getMessage(), ex);
    }
    catch (ConnectTimeoutException ex)
    {
      throw new ConnectTimeoutRuntimeException(ex.getMessage(), ex);
    }
    catch (SocketTimeoutException ex)
    {
      throw new SocketTimeoutRuntimeException(ex.getMessage(), ex);
    }
    catch (IOException ex)
    {
      throw new IORuntimeException(ex.getMessage(), ex);
    }
  }

}
