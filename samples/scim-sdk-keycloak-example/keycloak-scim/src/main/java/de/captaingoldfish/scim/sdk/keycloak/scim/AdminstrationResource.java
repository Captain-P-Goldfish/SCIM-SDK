package de.captaingoldfish.scim.sdk.keycloak.scim;

import java.time.Instant;
import java.util.Optional;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.auth.Authentication;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderService;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import lombok.extern.slf4j.Slf4j;


/**
 * this endpoint is used to change the SCIM configuration. It requires the role
 * {@link de.captaingoldfish.scim.sdk.keycloak.provider.RealmRoleInitializer#SCIM_ADMIN_ROLE} to get access to
 * the endpoints
 * 
 * @author Pascal Knueppel
 * @since 27.07.2020
 */
@Slf4j
public class AdminstrationResource extends AbstractEndpoint
{


  public AdminstrationResource(KeycloakSession keycloakSession)
  {
    super(keycloakSession);
    Authentication.authenticateAsScimAdmin(keycloakSession);
  }

  /**
   * updates the current service provider configuration
   * 
   * @param content the request body from the admin-console
   * @return changes the service provider configuration of the current realm
   */
  @PUT
  @Path("/serviceProviderConfig")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateServiceProviderConfig(final String content)
  {
    ServiceProvider newServiceProvider = JsonHelper.readJsonDocument(content, ServiceProvider.class);
    ScimServiceProviderService scimServiceProviderService = new ScimServiceProviderService(getKeycloakSession());
    newServiceProvider = scimServiceProviderService.updateServiceProvider(newServiceProvider);

    // now override the current service provider configuration
    {
      ResourceEndpoint resourceEndpoint = ScimConfiguration.getScimEndpoint(getKeycloakSession());
      ServiceProvider oldServiceProvider = resourceEndpoint.getServiceProvider();
      oldServiceProvider.setFilterConfig(newServiceProvider.getFilterConfig());
      oldServiceProvider.setSortConfig(newServiceProvider.getSortConfig());
      oldServiceProvider.setPatchConfig(newServiceProvider.getPatchConfig());
      oldServiceProvider.setETagConfig(newServiceProvider.getETagConfig());
      oldServiceProvider.setChangePasswordConfig(newServiceProvider.getChangePasswordConfig());
      oldServiceProvider.setBulkConfig(newServiceProvider.getBulkConfig());
      final Instant lastModified = newServiceProvider.getMeta().flatMap(Meta::getLastModified).orElse(null);
      final ETag version = newServiceProvider.getMeta().flatMap(Meta::getVersion).orElse(null);
      Optional<Meta> metaOptional = oldServiceProvider.getMeta();
      metaOptional.ifPresent(meta -> {
        // lastModified will never be null
        meta.setLastModified(lastModified);
        meta.setVersion(version);
      });
    }

    return Response.ok().entity(newServiceProvider.toString()).build();
  }

  /**
   * updates the resource type with the given name
   * 
   * @param resourceTypeName the name of the resource type that should be updated
   * @param content the new representation of the resource type
   * @return the updates resource type representation
   */
  @PUT
  @Path("/resourceType/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateResourceType(@PathParam("name") String resourceTypeName, final String content)
  {
    return Response.status(HttpStatus.NOT_IMPLEMENTED).build();
  }


}
