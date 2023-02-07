package de.captaingoldfish.scim.sdk.springboot.sample.controller;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.springboot.sample.config.ScimAuthentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.11.2019 - 00:02 <br>
 * <br>
 * the SCIM endpoint controller
 */
@Slf4j
@RestController
@RequestMapping("/scim/v2")
public class ScimController
{

  /**
   * the resource endpoint that handles ALL SCIM requests
   */
  private ResourceEndpoint resourceEndpoint;

  /**
   * spring injection constructor
   */
  public ScimController(ResourceEndpoint resourceEndpoint)
  {
    this.resourceEndpoint = resourceEndpoint;
  }

  /**
   * the rest-endpoint for SCIM that is accessible under the path ${basepath}/scim/v2/**
   *
   * @param request the request object created by the underlying tomcat
   * @param requestBody the request body
   * @return the scim response that will automatically be converted to json by spring
   */
  @RequestMapping(value = "/**", method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.PUT,
                                           RequestMethod.PATCH,
                                           RequestMethod.DELETE}, produces = HttpHeader.SCIM_CONTENT_TYPE)
  public @ResponseBody String handleScimRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                @RequestBody(required = false) String requestBody)
  {
    Map<String, String> httpHeaders = getHttpHeaders(request);
    String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();

    ScimAuthentication scimAuthentication = new ScimAuthentication();

    ScimResponse scimResponse = resourceEndpoint.handleRequest(request.getRequestURL().toString() + query,
                                                               HttpMethod.valueOf(request.getMethod()),
                                                               requestBody,
                                                               httpHeaders,
                                                               new Context(scimAuthentication));
    response.setContentType(HttpHeader.SCIM_CONTENT_TYPE);
    scimResponse.getHttpHeaders().forEach(response::setHeader);
    response.setStatus(scimResponse.getHttpStatus());
    return scimResponse.toPrettyString();
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
