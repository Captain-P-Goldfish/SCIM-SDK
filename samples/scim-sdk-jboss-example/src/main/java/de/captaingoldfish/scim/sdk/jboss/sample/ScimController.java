package de.captaingoldfish.scim.sdk.jboss.sample;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;


/**
 * @author Pascal Knueppel
 * @since 22.01.2021
 */
@Path("/scim/v2")
public class ScimController
{

  @Inject
  private ScimConfig scimConfig;

  @POST
  @GET
  @PUT
  @PATCH
  @DELETE
  @Path("/{s:.*}")
  @Produces(HttpHeader.SCIM_CONTENT_TYPE)
  public Response handleScimRequest(@Context HttpServletRequest request)
  {
    ResourceEndpoint resourceEndpoint = scimConfig.getResourceEndpoint();

    String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();

    ScimAuthentication scimAuthentication = new ScimAuthentication();
    de.captaingoldfish.scim.sdk.server.endpoints.Context context =
    // @formatter:off
                  new de.captaingoldfish.scim.sdk.server.endpoints.Context(scimAuthentication);
    // @formatter:on
    ScimResponse scimResponse = resourceEndpoint.handleRequest(request.getRequestURL().toString() + query,
                                                               HttpMethod.valueOf(request.getMethod()),
                                                               getRequestBody(request),
                                                               getHttpHeaders(request),
                                                               context);
    return scimResponse.buildResponse();
  }

  /**
   * reads the request body from the input stream of the request object
   *
   * @param request the request object
   * @return the request body as string
   */
  public String getRequestBody(HttpServletRequest request)
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
  public Map<String, String> getHttpHeaders(HttpServletRequest request)
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
