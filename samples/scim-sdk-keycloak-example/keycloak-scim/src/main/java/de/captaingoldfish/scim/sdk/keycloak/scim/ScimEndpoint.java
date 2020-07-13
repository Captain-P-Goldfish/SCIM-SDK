package de.captaingoldfish.scim.sdk.keycloak.scim;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.keycloak.auth.ScimAuthorization;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@Slf4j
public class ScimEndpoint
{

  /**
   * the keycloak session holds information about the current authentication and the realm that we are currently
   * in
   */
  private final KeycloakSession keycloakSession;

  /**
   * standard constructor
   */
  public ScimEndpoint(KeycloakSession keycloakSession)
  {
    this.keycloakSession = keycloakSession;
  }

  /**
   * handles all SCIM requests
   *
   * @param request the server request object
   * @return the jax-rs response
   */
  @POST
  @GET
  @PUT
  @PATCH
  @DELETE
  @Path("/v2/{s:.*}")
  @Produces(HttpHeader.SCIM_CONTENT_TYPE)
  public Response get(@Context HttpServletRequest request, @Context HttpServletResponse response)
  {
    RealmModel realmModel = keycloakSession.getContext().getRealm();
    ResourceEndpoint resourceEndpoint = ScimConfiguration.getScimEndpoint(realmModel);

    String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
    ScimResponse scimResponse = resourceEndpoint.handleRequest(request.getRequestURL().toString() + query,
                                                               HttpMethod.valueOf(request.getMethod()),
                                                               getRequestBody(request),
                                                               getHttpHeaders(request),
                                                               new ScimAuthorization(keycloakSession));
    try
    {
      keycloakSession.getTransactionManager().commit();
    }
    catch (Exception ex)
    {
      final boolean useDetailMessage = true;
      return new ErrorResponse(new InternalServerException(ex.getMessage()), useDetailMessage).buildResponse();
    }
    scimResponse.getHttpHeaders().forEach(response::addHeader);
    return scimResponse.buildResponse();
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
