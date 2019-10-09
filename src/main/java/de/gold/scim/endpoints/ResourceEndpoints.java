package de.gold.scim.endpoints;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.InternalServerException;
import de.gold.scim.exceptions.ScimException;
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
    JsonNode responseResource = resourceType.getResourceHandlerImpl().createResource(resource);
    responseResource = SchemaValidator.validateDocumentForResponse(resourceTypeFactory, resourceType, responseResource);
    return buildScimResponse(responseResource);
  }

  private ScimResponse buildScimResponse(JsonNode responseResource)
  {
    return null;
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
   * TODO
   *
   * @param resourceTypeList
   * @return
   */
  private ResourceType resolveResourceTypeFromMultipleResourcesFound(List<ResourceType> resourceTypeList)
  {
    throw new NotImplementedException("not yet implemented");
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
