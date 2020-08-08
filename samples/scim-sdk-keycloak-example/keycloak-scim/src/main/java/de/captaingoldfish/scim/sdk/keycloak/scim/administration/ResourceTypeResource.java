package de.captaingoldfish.scim.sdk.keycloak.scim.administration;

import java.util.Optional;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractEndpoint;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.ParseableResourceType;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimResourceTypeService;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;


/**
 * @author Pascal Knueppel
 * @since 08.08.2020
 */
public class ResourceTypeResource extends AbstractEndpoint
{

  public ResourceTypeResource(KeycloakSession keycloakSession)
  {
    super(keycloakSession);
  }

  /**
   * updates the resource type with the given name
   *
   * @param resourceTypeName the name of the resource type that should be updated
   * @param content the new representation of the resource type
   * @return the updates resource type representation
   */
  @PUT
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateResourceType(@PathParam("name") String resourceTypeName, final String content)
  {
    Optional<ResourceType> resourceTypeOptional = getResourceEndpoint().getResourceTypeByName(resourceTypeName);
    if (!resourceTypeOptional.isPresent())
    {
      return Response.status(HttpStatus.BAD_REQUEST).entity("resource type cannot be updated").build();
    }

    ScimResourceTypeService resourceTypeService = new ScimResourceTypeService(getKeycloakSession());
    ParseableResourceType parseableResourceType = JsonHelper.readJsonDocument(content, ParseableResourceType.class);

    Optional<ScimResourceTypeEntity> scimResourceTypeEntity = resourceTypeService.updateDatabaseEntry(parseableResourceType);
    if (!scimResourceTypeEntity.isPresent())
    {
      return Response.status(HttpStatus.BAD_REQUEST).entity("resource type cannot be updated").build();
    }

    ResourceType resourceType = resourceTypeOptional.get();
    resourceTypeService.updateResourceType(resourceType, scimResourceTypeEntity.get());
    return Response.status(HttpStatus.OK).entity(resourceType.toString()).build();
  }

  /**
   * overridden to grant access to unit test scope
   */
  @Override
  protected ResourceEndpoint getResourceEndpoint()
  {
    return super.getResourceEndpoint();
  }
}
