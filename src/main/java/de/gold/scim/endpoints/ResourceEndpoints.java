package de.gold.scim.endpoints;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.InternalServerException;
import de.gold.scim.exceptions.ScimException;
import de.gold.scim.resources.ResourceNode;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.response.ScimResponse;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.schemas.SchemaValidator;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:28 <br>
 * <br>
 */
@Slf4j
public final class ResourceEndpoints
{

  /**
   * this is used to prevent application context pollution in unit tests
   */
  private ResourceTypeFactory resourceTypeFactory;

  public ResourceEndpoints(EndpointDefinition... endpointDefinitions)
  {
    this(ResourceTypeFactory.getInstance(), endpointDefinitions);
  }

  /**
   * this constructor was introduced for unit tests to add a specific resourceTypeFactory instance which will
   * prevent application context pollution within unit tests
   */
  ResourceEndpoints(ResourceTypeFactory resourceTypeFactory, EndpointDefinition... endpointDefinitions)
  {
    this.resourceTypeFactory = resourceTypeFactory;
    if (endpointDefinitions == null || endpointDefinitions.length == 0)
    {
      throw new InternalServerException("At least 1 endpoint must be registered!", null, null);
    }
    for ( EndpointDefinition endpointDefinition : endpointDefinitions )
    {
      resourceTypeFactory.registerResourceType(endpointDefinition.getResourceHandler(),
                                               endpointDefinition.getResourceType(),
                                               endpointDefinition.getResourceSchema(),
                                               endpointDefinition.getResourceSchemaExtensions()
                                                                 .toArray(new JsonNode[0]));
    }
  }

  public ScimResponse createResource(String endpoint, String resourceDocument)
  {
    Supplier<String> errorMessage = () -> "no resource found for endpoint '" + endpoint + "'";
    ResourceType resourceType = Optional.ofNullable(resourceTypeFactory.getResourceType(endpoint))
                                        .orElseThrow(() -> new BadRequestException(errorMessage.get(), null,
                                                                                   ScimType.UNKNOWN_RESOURCE));
    JsonNode resource = JsonHelper.readJsonDocument(resourceDocument);
    resource = SchemaValidator.validateDocumentForRequest(resourceTypeFactory,
                                                          resourceType,
                                                          resource,
                                                          SchemaValidator.HttpMethod.POST);
    ResourceHandler resourceHandler = resourceType.getResourceHandlerImpl();
    ResourceNode resourceNode = (ResourceNode)JsonHelper.copyResourceToObject(resource, resourceHandler.getType());
    resourceNode.setMeta(getMeta(resourceType));
    resourceNode = resourceHandler.createResource(resourceNode);
    JsonNode responseResource = SchemaValidator.validateDocumentForResponse(resourceTypeFactory,
                                                                            resourceType,
                                                                            resourceNode);

    return buildScimResponse(responseResource);
  }

  public ScimResponse getResource(String id)
  {
    return null;
  }

  public ScimResponse updateResource(String id, String resourceDocument)
  {
    return null;
  }

  public ScimResponse deleteResource(String id)
  {
    return null;
  }

  /**
   * creates the meta representation for the response
   *
   * @param resourceType the resource type that holds necessary data like the name of the resource
   * @return the meta json representation
   */
  private Meta getMeta(ResourceType resourceType)
  {
    LocalDateTime now = LocalDateTime.now();
    return Meta.builder()
               .created(now)
               .lastModified(now)
               .location(getLocation(resourceType))
               .resourceType(resourceType.getName())
               .build();
  }

  /**
   * builds the location attribute for the meta-object
   *
   * @param resourceType holds the endpoint definition
   * @return the current location
   */
  private String getLocation(ResourceType resourceType)
  {
    // TODO get the fully qualified url
    return resourceType.getEndpoint();
  }

  private ScimResponse buildScimResponse(JsonNode responseResource)
  {
    return null;
  }

  private String getAttributeMissingMessage(String attributeName)
  {
    return "missing '" + attributeName + "' attribute in request";
  }

  private ScimException getBadRequestException(String errorMessage, String scimType)
  {
    return new BadRequestException(errorMessage, null, scimType);
  }
}
