package de.captaingoldfish.scim.sdk.jboss.sample;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;


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
  public Response handleScimRequest(@Context HttpServerRequest request, String body)
  {
    ResourceEndpoint resourceEndpoint = scimConfig.getResourceEndpoint();

    ScimAuthentication scimAuthentication = new ScimAuthentication();
    de.captaingoldfish.scim.sdk.server.endpoints.Context context =
    // @formatter:off
                  new de.captaingoldfish.scim.sdk.server.endpoints.Context(scimAuthentication);
    // @formatter:on
    ScimResponse scimResponse = resourceEndpoint.handleRequest(request.absoluteURI(),
                                                               HttpMethod.valueOf(request.method().name()),
                                                               body,
                                                               getHttpHeaders(request),
                                                               context);
    return scimResponse.buildResponse();
  }

  /**
   * extracts the http headers from the request and puts them into a map
   *
   * @param request the request object
   * @return a map with the http-headers
   */
  public Map<String, String> getHttpHeaders(HttpServerRequest request)
  {
    Map<String, String> httpHeaders = new HashMap<>();
    MultiMap headerMap = request.headers();
    for ( Map.Entry<String, String> headerEntry : headerMap )
    {
      httpHeaders.put(headerEntry.getKey(), headerEntry.getValue());

    }
    return httpHeaders;
  }
}
