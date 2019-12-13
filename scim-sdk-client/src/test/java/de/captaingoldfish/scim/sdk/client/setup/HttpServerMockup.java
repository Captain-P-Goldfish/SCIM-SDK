package de.captaingoldfish.scim.sdk.client.setup;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.captaingoldfish.scim.sdk.client.setup.scim.ScimConfig;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 25.09.2018 - 10:41 <br>
 * <br>
 * this class will mock an http server that can be used for
 */
@Slf4j
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public abstract class HttpServerMockup
{

  /**
   * the base path that must be present in the request
   */
  public static final String CONTEXT_PATH = "/scim/v2";

  /**
   * creates a new scim configuration with user and group endpoints
   */
  protected final static ScimConfig scimConfig = new ScimConfig();

  /**
   * this port will be used by the {@link #server}
   */
  private int serverPort;

  /**
   * this URL will be built by the method {@link #initializeAndStartHttpServer()}
   */
  @Getter(AccessLevel.PUBLIC)
  private String serverUrl;

  /**
   * this is the http server instance that will be started
   */
  private HttpServer server;

  /**
   * we use this error in case that an {@link AssertionError} or an {@link Exception} was thrown on the mocked
   * server
   */
  @Getter(AccessLevel.PUBLIC)
  private Throwable ex;

  /**
   * this field can be modified to ignore the error {@link #ex} on the IAM that would then be thrown in the
   * method {@link #checkMockServerError()}
   */
  @Setter(AccessLevel.PUBLIC)
  private boolean ignoreServerError = false;

  /**
   * can be used to validate the request attributes
   */
  @Setter(AccessLevel.PUBLIC)
  private Consumer<HttpExchange> verifyRequestAttributes = httpExchange -> {};

  /**
   * here we will check if an error occurred on the mocked server and if it did we will throw the error in the
   * test context
   */
  @AfterEach
  public void checkMockServerError() throws Throwable
  {
    if (!ignoreServerError && ex != null)
    {
      throw ex;
    }
  }

  /**
   * will start the mocked server on a free port on the current system
   */
  @BeforeEach
  public void initializeAndStartHttpServer() throws IOException
  {
    runHttpServer();
  }

  /**
   * will start the mocked server on a free port on the current system
   */
  private void runHttpServer() throws IOException
  {
    serverPort = getFreeLocalPort();
    serverUrl = "http://localhost:" + serverPort + CONTEXT_PATH;

    server = HttpServer.create(new InetSocketAddress(serverPort), 0);
    server.createContext("/", httpExchange -> {
      log.trace("handles request: {}", httpExchange.getLocalAddress());
      Optional<String> responseBodyOptional;
      try
      {
        verifyRequestAttributes.accept(httpExchange);
        responseBodyOptional = handleMockedServerRequest(httpExchange);
      }
      catch (Exception | AssertionError ex)
      {
        responseBodyOptional = handleMockedServerError(httpExchange, ex);
      }
      finally
      {
        log.trace("calling server behaviour changer");
      }
      responseBodyOptional.ifPresent(responseBody -> {
        try (OutputStream outputStream = httpExchange.getResponseBody())
        {
          outputStream.write(responseBody.getBytes());
        }
        catch (IOException e)
        {
          log.error(e.getMessage(), e);
        }
      });
      httpExchange.close();
      log.trace("finished handling server request");
    });

    server.setExecutor(null); // creates a default executor
    server.start();
  }

  /**
   * this method will handle the incoming request on the mocked server
   *
   * @param httpExchange the http object to handle request and response
   */
  private Optional<String> handleMockedServerRequest(HttpExchange httpExchange) throws IOException
  {
    log.trace("handling server request");
    log.trace("http-method: {}", httpExchange.getRequestMethod());
    log.trace("uri: {}", httpExchange.getRequestURI().toString());

    Map<String, String> requestHeaders = getRequestHeaders(httpExchange);
    ScimResponse scimResponse = scimConfig.getResourceEndpoint()
                                          .handleRequest(getRequestUri(httpExchange),
                                                         HttpMethod.valueOf(httpExchange.getRequestMethod()),
                                                         getRequestBody(httpExchange),
                                                         requestHeaders,
                                                         null);

    Map<String, List<String>> headerMap = new HashMap<>();
    scimResponse.getHttpHeaders().forEach((key, value) -> headerMap.put(key, Collections.singletonList(value)));
    Headers responseHeaders = httpExchange.getResponseHeaders();
    responseHeaders.putAll(headerMap);
    String responseBody = StringUtils.stripToNull(scimResponse.toString());
    responseBody = StringUtils.stripToNull(responseBody);
    httpExchange.sendResponseHeaders(scimResponse.getHttpStatus(), responseBody == null ? 0 : responseBody.length());
    log.trace("finished handling server request");
    return Optional.ofNullable(responseBody);
  }

  private String getRequestUri(HttpExchange httpExchange)
  {
    return "http:/" + httpExchange.getLocalAddress().toString() + httpExchange.getRequestURI().toString();
  }

  /**
   * extracts the request headers from the http request and puts them into a hash map
   * 
   * @param httpExchange the server request and response object
   * @return the map with the request headers
   */
  private Map<String, String> getRequestHeaders(HttpExchange httpExchange)
  {
    Map<String, String> requestHeaders = new HashMap<>();
    for ( Map.Entry<String, List<String>> stringListEntry : httpExchange.getRequestHeaders().entrySet() )
    {
      requestHeaders.put(stringListEntry.getKey(), String.join(",", stringListEntry.getValue()));
    }
    return requestHeaders;
  }

  /**
   * this method will handle errors that will occur on the mocked server by setting an appropriate response code
   * and the error into the response with content type text/plain
   *
   * @param httpExchange the http object to handle request and response
   * @param ex the error that occured that is either an {@link Exception} or an {@link AssertionError}
   */
  private Optional<String> handleMockedServerError(HttpExchange httpExchange, Throwable ex) throws IOException
  {
    log.error("handle server exception", ex);
    this.ex = ex;
    String message = StringUtils.stripToNull(ex.getMessage());
    log.error("setting server response status code to 500");
    httpExchange.sendResponseHeaders(HttpStatus.SC_INTERNAL_SERVER_ERROR, message == null ? 0 : message.length());
    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList("text/plain"));
    Headers responseHeaders = httpExchange.getResponseHeaders();
    responseHeaders.putAll(headerMap);
    log.trace("finished handling server error");
    return Optional.ofNullable(message);
  }

  /**
   * gets a port on the current machine that is unused
   *
   * @return an unused port
   */
  protected int getFreeLocalPort() throws IOException
  {
    ServerSocket serverSocket = new ServerSocket(0);
    int localPort = serverSocket.getLocalPort();
    serverSocket.close();
    return localPort;
  }

  /**
   * will shutdown the server again
   */
  @AfterEach
  public void shutdownServer()
  {
    server.stop(0);
  }

  /**
   * gets the query parameter of the given URI as a map
   * 
   * @param uri the uri that should be decoded
   * @return a map of the decoded parameters
   */
  public Map<String, String> splitQuery(URI uri)
  {
    Map<String, String> query_pairs = new LinkedHashMap<>();
    String query = uri.getQuery();
    String[] pairs = query.split("&");
    for ( String pair : pairs )
    {
      int idx = pair.indexOf("=");
      try
      {
        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                        URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
      }
      catch (UnsupportedEncodingException e)
      {
        throw new IllegalStateException(e);
      }
    }
    return query_pairs;
  }

  /**
   * reads the request body from the given http exchange object
   * 
   * @param httpExchange the http request object from the server
   * @return the request body
   */
  public String getRequestBody(HttpExchange httpExchange)
  {
    try
    {
      return IOUtils.toString(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
}
