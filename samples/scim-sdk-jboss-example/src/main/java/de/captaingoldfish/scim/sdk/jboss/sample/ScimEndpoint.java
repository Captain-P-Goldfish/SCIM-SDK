package de.captaingoldfish.scim.sdk.jboss.sample;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;


/**
 * author Pascal Knueppel <br>
 * created at: 24.11.2019 - 19:12 <br>
 * <br>
 * this servlet will handle all SCIM requests
 */
@WebServlet("/scim/v2/*")
public class ScimEndpoint extends HttpServlet
{

  /**
   * the scim configuration that is used on this endpoint
   */
  @Inject
  private ScimConfig scimConfig;

  /**
   * handles all requests to this scim resource server
   */
  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException
  {
    String requestBody = getRequestBody(request);
    Map<String, String> httpHeaders = getHttpHeaders(request);
    String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
    ScimResponse scimResponse = scimConfig.getResourceEndpoint()
                                          .handleRequest(request.getRequestURL().toString() + query,
                                                         HttpMethod.valueOf(request.getMethod()),
                                                         requestBody,
                                                         httpHeaders);
    response.setContentType(HttpHeader.SCIM_CONTENT_TYPE);
    scimResponse.getHttpHeaders().forEach(response::setHeader);
    response.getWriter().append(scimResponse.toString());
    response.setStatus(scimResponse.getHttpStatus());
  }

  /**
   * reads the request body from the input stream of the request object
   *
   * @param request the request object
   * @return the request body as string
   */
  private String getRequestBody(HttpServletRequest request)
  {
    try (InputStream inputStream = request.getInputStream())
    {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /**
   * extracts the http headers from the request and puts them into a map
   *
   * @param request the request object
   * @return a map with the http-headers
   */
  private Map<String, String> getHttpHeaders(HttpServletRequest request)
  {
    Map<String, String> httpHeaders = new HashMap<>();
    Enumeration<String> enumeration = request.getHeaderNames();
    while (enumeration != null && enumeration.hasMoreElements())
    {
      String headerName = enumeration.nextElement();
      httpHeaders.put(headerName, request.getHeader(headerName));
    }
    return httpHeaders;
  }

}
