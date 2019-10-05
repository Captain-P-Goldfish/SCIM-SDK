package de.gold.scim.endpoints;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;
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
    if (endpointDefinitions == null)
    {
      return;
    }
    for ( EndpointDefinition endpointDefinition : endpointDefinitions )
    {
      resourceTypeFactory.registerResourceType(endpointDefinition.getResourceType(),
                                               endpointDefinition.getResourceSchema(),
                                               endpointDefinition.getResourceSchemaExtensions()
                                                                 .toArray(new JsonNode[0]));
    }
  }

  public ScimResponse createResource(String resourceDocument)
  {
    JsonNode resource = JsonHelper.readJsonDocument(resourceDocument);
    ResourceType resourceType = findResourceType(resource);
    SchemaValidator.validateSchemaForRequest(resourceType, resource, SchemaValidator.HttpMethod.POST);

    // TODO execute developer implementation

    // TODO validate developer return type by schema validation for response

    // TODO return a scim response
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

  private ResourceType findResourceType(JsonNode resource)
  {
    String errorMessage = getAttributeMissingMessage(AttributeNames.SCHEMAS);
    List<String> schemas = JsonHelper.getSimpleAttributeArray(resource, AttributeNames.SCHEMAS)
                                     .orElseThrow(() -> getBadRequestException(errorMessage, ScimType.REQUIRED));
    List<ResourceType> resourceTypeList = new ArrayList<>();
    for ( String schema : schemas )
    {
      resourceTypeList.add(resourceTypeFactory.getResourceType(schema));
    }
    if (resourceTypeList.size() == 0)
    {
      throw getBadRequestException(errorMessage, ScimType.REQUIRED);
    }
    if (resourceTypeList.size() == 1)
    {
      return resourceTypeList.get(0);
    }
    return resolveResourceTypeFromMultipleResourcesFound(resourceTypeList);
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
