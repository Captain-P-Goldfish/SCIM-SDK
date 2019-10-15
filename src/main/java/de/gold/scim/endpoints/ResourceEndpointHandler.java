package de.gold.scim.endpoints;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.InternalServerException;
import de.gold.scim.exceptions.ResourceNotFoundException;
import de.gold.scim.exceptions.ScimException;
import de.gold.scim.resources.ResourceNode;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.response.CreateResponse;
import de.gold.scim.response.DeleteResponse;
import de.gold.scim.response.ErrorResponse;
import de.gold.scim.response.GetResponse;
import de.gold.scim.response.ScimResponse;
import de.gold.scim.response.UpdateResponse;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.schemas.SchemaValidator;
import de.gold.scim.utils.JsonHelper;
import de.gold.scim.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:28 <br>
 * <br>
 * The main class of this framework. The resource endpoint can be used to register all resource types that
 * will then be used to delegate to the different resource implementations
 */
@Slf4j
public final class ResourceEndpointHandler
{

  /**
   * this is used to prevent application context pollution in unit tests
   */
  private ResourceTypeFactory resourceTypeFactory;

  public ResourceEndpointHandler(EndpointDefinition... endpointDefinitions)
  {
    this(ResourceTypeFactory.getInstance(), endpointDefinitions);
  }

  /**
   * this constructor was introduced for unit tests to add a specific resourceTypeFactory instance which will
   * prevent application context pollution within unit tests
   */
  ResourceEndpointHandler(ResourceTypeFactory resourceTypeFactory, EndpointDefinition... endpointDefinitions)
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

  /**
   * checks if a resource type exists under the given endpoint and validates the request if it does by the
   * corresponding meta schema. If the validation succeeds the single json nodes expanded with its meta
   * information will be given to the developer custom implementation. The returned object for the response will
   * be validated again and then returned as a SCIM response
   *
   * @param endpoint the resource endpoint that was called
   * @param resourceDocument the resource document
   * @return the scim response for the client
   */
  public ScimResponse createResource(String endpoint, String resourceDocument)
  {
    return createResource(endpoint, resourceDocument, null, null, null);
  }

  /**
   * checks if a resource type exists under the given endpoint and validates the request if it does by the
   * corresponding meta schema. If the validation succeeds the single json nodes expanded with its meta
   * information will be given to the developer custom implementation. The returned object for the response will
   * be validated again and then returned as a SCIM response
   *
   * @param endpoint the resource endpoint that was called
   * @param resourceDocument the resource document
   * @param baseUrlSupplier this supplier is an optional attribute that should be used to supply the information
   *          of the base URL of this application e.g.: https://example.com/scim/v2. This return value will be
   *          used to create the location URL of the resources like 'https://example.com/scim/v2/Users/123456'.
   *          If this parameter is not present the application will try to read a hardcoded URL from the service
   *          provider configuration that is also an optional attribute. If both ways fail an exception will be
   *          thrown
   * @return the scim response for the client
   */
  public ScimResponse createResource(String endpoint, String resourceDocument, Supplier<String> baseUrlSupplier)
  {
    return createResource(endpoint, resourceDocument, null, null, baseUrlSupplier);
  }

  /**
   * checks if a resource type exists under the given endpoint and validates the request if it does by the
   * corresponding meta schema. If the validation succeeds the single json nodes expanded with its meta
   * information will be given to the developer custom implementation. The returned object for the response will
   * be validated again and then returned as a SCIM response
   *
   * @param endpoint the resource endpoint that was called
   * @param resourceDocument the resource document
   * @param attributes When specified, the default list of attributes SHALL be overridden, and each resource
   *          returned MUST contain the minimum set of resource attributes and any attributes or sub-attributes
   *          explicitly requested by the "attributes" parameter. The query parameter attributes value is a
   *          comma-separated list of resource attribute names in standard attribute notation (Section 3.10)
   *          form (e.g., userName, name, emails).
   * @param excludedAttributes When specified, each resource returned MUST contain the minimum set of resource
   *          attributes. Additionally, the default set of attributes minus those attributes listed in
   *          "excludedAttributes" is returned. The query parameter attributes value is a comma-separated list
   *          of resource attribute names in standard attribute notation (Section 3.10) form (e.g., userName,
   *          name, emails).
   * @param baseUrlSupplier this supplier is an optional attribute that should be used to supply the information
   *          of the base URL of this application e.g.: https://example.com/scim/v2. This return value will be
   *          used to create the location URL of the resources like 'https://example.com/scim/v2/Users/123456'.
   *          If this parameter is not present the application will try to read a hardcoded URL from the service
   *          provider configuration that is also an optional attribute. If both ways fail an exception will be
   *          thrown
   * @return the scim response for the client
   */
  public ScimResponse createResource(String endpoint,
                                     String resourceDocument,
                                     String attributes,
                                     String excludedAttributes,
                                     Supplier<String> baseUrlSupplier)
  {
    try
    {
      RequestUtils.validateAttributesAndExcludedAttributes(attributes, excludedAttributes);
      ResourceType resourceType = getResourceType(endpoint);
      JsonNode resource = JsonHelper.readJsonDocument(resourceDocument);
      resource = SchemaValidator.validateDocumentForRequest(resourceTypeFactory,
                                                            resourceType,
                                                            resource,
                                                            SchemaValidator.HttpMethod.POST);
      ResourceHandler resourceHandler = resourceType.getResourceHandlerImpl();
      ResourceNode resourceNode = (ResourceNode)JsonHelper.copyResourceToObject(resource, resourceHandler.getType());
      resourceNode = resourceHandler.createResource(resourceNode);
      final String location = getLocation(resourceType, resourceNode.getId().get(), baseUrlSupplier);
      resourceNode.setMeta(getMeta(resourceType, location));
      JsonNode responseResource = SchemaValidator.validateDocumentForResponse(resourceTypeFactory,
                                                                              resourceType,
                                                                              resourceNode,
                                                                              resource,
                                                                              attributes,
                                                                              excludedAttributes);
      return new CreateResponse(responseResource, location);
    }
    catch (ScimException ex)
    {
      return new ErrorResponse(ex);
    }
    catch (Exception ex)
    {
      return new ErrorResponse(new InternalServerException(ex.getMessage(), ex, null));
    }
  }

  /**
   * checks if a resource type exists under the given endpoint and will then give the id to the developers
   * custom implementation stored under the found resource type. The returned {@link ResourceNode} will then be
   * validated and eventually returned to the client
   *
   * @param endpoint the resource endpoint that was called
   * @param id the id of the resource that was requested
   * @return the scim response for the client
   */
  public ScimResponse getResource(String endpoint, String id)
  {
    return getResource(endpoint, id, null, null, null);
  }

  /**
   * checks if a resource type exists under the given endpoint and will then give the id to the developers
   * custom implementation stored under the found resource type. The returned {@link ResourceNode} will then be
   * validated and eventually returned to the client
   *
   * @param endpoint the resource endpoint that was called
   * @param id the id of the resource that was requested
   * @param baseUrlSupplier this supplier is an optional attribute that should be used to supply the information
   *          of the base URL of this application e.g.: https://example.com/scim/v2. This return value will be
   *          used to create the location URL of the resources like 'https://example.com/scim/v2/Users/123456'.
   *          If this parameter is not present the application will try to read a hardcoded URL from the service
   *          provider configuration that is also an optional attribute. If both ways fail an exception will be
   *          thrown
   * @return the scim response for the client
   */
  public ScimResponse getResource(String endpoint, String id, Supplier<String> baseUrlSupplier)
  {
    return getResource(endpoint, id, null, null, baseUrlSupplier);
  }

  /**
   * checks if a resource type exists under the given endpoint and will then give the id to the developers
   * custom implementation stored under the found resource type. The returned {@link ResourceNode} will then be
   * validated and eventually returned to the client
   *
   * @param endpoint the resource endpoint that was called
   * @param id the id of the resource that was requested
   * @param attributes When specified, the default list of attributes SHALL be overridden, and each resource
   *          returned MUST contain the minimum set of resource attributes and any attributes or sub-attributes
   *          explicitly requested by the "attributes" parameter. The query parameter attributes value is a
   *          comma-separated list of resource attribute names in standard attribute notation (Section 3.10)
   *          form (e.g., userName, name, emails).
   * @param excludedAttributes When specified, each resource returned MUST contain the minimum set of resource
   *          attributes. Additionally, the default set of attributes minus those attributes listed in
   *          "excludedAttributes" is returned. The query parameter attributes value is a comma-separated list
   *          of resource attribute names in standard attribute notation (Section 3.10) form (e.g., userName,
   *          name, emails).
   * @param baseUrlSupplier this supplier is an optional attribute that should be used to supply the information
   *          of the base URL of this application e.g.: https://example.com/scim/v2. This return value will be
   *          used to create the location URL of the resources like 'https://example.com/scim/v2/Users/123456'.
   *          If this parameter is not present the application will try to read a hardcoded URL from the service
   *          provider configuration that is also an optional attribute. If both ways fail an exception will be
   *          thrown
   * @return the scim response for the client
   */
  public ScimResponse getResource(String endpoint,
                                  String id,
                                  String attributes,
                                  String excludedAttributes,
                                  Supplier<String> baseUrlSupplier)
  {
    try
    {
      RequestUtils.validateAttributesAndExcludedAttributes(attributes, excludedAttributes);
      ResourceType resourceType = getResourceType(endpoint);
      ResourceHandler resourceHandler = resourceType.getResourceHandlerImpl();
      ResourceNode resourceNode = resourceHandler.getResource(id);
      if (resourceNode == null)
      {
        throw new ResourceNotFoundException("the '" + resourceType.getName() + "' resource with id '" + id + "' does "
                                            + "not exist", null, null);
      }
      String resourceId = resourceNode.getId().orElse(null);
      if (StringUtils.isBlank(resourceId) || !resourceId.equals(id))
      {
        throw new InternalServerException("the id of the returned resource does not match the "
                                          + "requested id: requestedId: '" + id + "', returnedId: '" + resourceId + "'",
                                          null, null);
      }
      final String location = getLocation(resourceType, id, baseUrlSupplier);
      resourceNode.setMeta(getMeta(resourceType, location));
      JsonNode responseResource = SchemaValidator.validateDocumentForResponse(resourceTypeFactory,
                                                                              resourceType,
                                                                              resourceNode,
                                                                              null,
                                                                              attributes,
                                                                              excludedAttributes);
      return new GetResponse(responseResource, location);
    }
    catch (ScimException ex)
    {
      return new ErrorResponse(ex);
    }
    catch (Exception ex)
    {
      return new ErrorResponse(new InternalServerException(ex.getMessage(), ex, null));
    }
  }


  /**
   * checks if a resource type exists under the given endpoint and validates the request if it does by the
   * corresponding meta schema. If the validation succeeds the single json nodes expanded with its meta
   * information will be given to the developer custom implementation. The returned object for the response will
   * be validated again and then returned as a SCIM response
   *
   * @param endpoint the resource endpoint that was called
   * @param id the id of the resource that was requested
   * @param resourceDocument the resource document
   * @return the scim response for the client
   */
  public ScimResponse updateResource(String endpoint, String id, String resourceDocument)
  {
    return updateResource(endpoint, id, resourceDocument, null, null, null);
  }

  /**
   * checks if a resource type exists under the given endpoint and validates the request if it does by the
   * corresponding meta schema. If the validation succeeds the single json nodes expanded with its meta
   * information will be given to the developer custom implementation. The returned object for the response will
   * be validated again and then returned as a SCIM response
   *
   * @param endpoint the resource endpoint that was called
   * @param id the id of the resource that was requested
   * @param resourceDocument the resource document
   * @param baseUrlSupplier this supplier is an optional attribute that should be used to supply the information
   *          of the base URL of this application e.g.: https://example.com/scim/v2. This return value will be
   *          used to create the location URL of the resources like 'https://example.com/scim/v2/Users/123456'.
   *          If this parameter is not present the application will try to read a hardcoded URL from the service
   *          provider configuration that is also an optional attribute. If both ways fail an exception will be
   *          thrown
   * @return the scim response for the client
   */
  public ScimResponse updateResource(String endpoint,
                                     String id,
                                     String resourceDocument,
                                     Supplier<String> baseUrlSupplier)
  {
    return updateResource(endpoint, id, resourceDocument, null, null, baseUrlSupplier);
  }

  /**
   * checks if a resource type exists under the given endpoint and validates the request if it does by the
   * corresponding meta schema. If the validation succeeds the single json nodes expanded with its meta
   * information will be given to the developer custom implementation. The returned object for the response will
   * be validated again and then returned as a SCIM response
   *
   * @param endpoint the resource endpoint that was called
   * @param id the id of the resource that was requested
   * @param resourceDocument the resource document
   * @param attributes When specified, the default list of attributes SHALL be overridden, and each resource
   *          returned MUST contain the minimum set of resource attributes and any attributes or sub-attributes
   *          explicitly requested by the "attributes" parameter. The query parameter attributes value is a
   *          comma-separated list of resource attribute names in standard attribute notation (Section 3.10)
   *          form (e.g., userName, name, emails).
   * @param excludedAttributes When specified, each resource returned MUST contain the minimum set of resource
   *          attributes. Additionally, the default set of attributes minus those attributes listed in
   *          "excludedAttributes" is returned. The query parameter attributes value is a comma-separated list
   *          of resource attribute names in standard attribute notation (Section 3.10) form (e.g., userName,
   *          name, emails).
   * @param baseUrlSupplier this supplier is an optional attribute that should be used to supply the information
   *          of the base URL of this application e.g.: https://example.com/scim/v2. This return value will be
   *          used to create the location URL of the resources like 'https://example.com/scim/v2/Users/123456'.
   *          If this parameter is not present the application will try to read a hardcoded URL from the service
   *          provider configuration that is also an optional attribute. If both ways fail an exception will be
   *          thrown
   * @return the scim response for the client
   */
  public ScimResponse updateResource(String endpoint,
                                     String id,
                                     String resourceDocument,
                                     String attributes,
                                     String excludedAttributes,
                                     Supplier<String> baseUrlSupplier)
  {
    try
    {
      RequestUtils.validateAttributesAndExcludedAttributes(attributes, excludedAttributes);
      ResourceType resourceType = getResourceType(endpoint);
      JsonNode resource = JsonHelper.readJsonDocument(resourceDocument);
      resource = SchemaValidator.validateDocumentForRequest(resourceTypeFactory,
                                                            resourceType,
                                                            resource,
                                                            SchemaValidator.HttpMethod.PUT);
      ResourceHandler resourceHandler = resourceType.getResourceHandlerImpl();
      if (resource == null)
      {
        throw new BadRequestException("the request body does not contain any writable parameters", null,
                                      ScimType.UNPARSABLE_REQUEST);
      }
      ResourceNode resourceNode = (ResourceNode)JsonHelper.copyResourceToObject(resource, resourceHandler.getType());
      resourceNode.setId(id);
      final String location = getLocation(resourceType, id, baseUrlSupplier);
      resourceNode.setMeta(getMeta(resourceType, location));
      resourceNode = resourceHandler.updateResource(resourceNode);
      if (resourceNode == null)
      {
        throw new ResourceNotFoundException("the '" + resourceType.getName() + "' resource with id '" + id + "' does "
                                            + "not exist", null, null);
      }
      String resourceId = resourceNode.getId().orElse(null);
      if (StringUtils.isBlank(resourceId) || !resourceId.equals(id))
      {
        throw new InternalServerException("the id of the returned resource does not match the "
                                          + "requested id: requestedId: '" + id + "', returnedId: '" + resourceId + "'",
                                          null, null);
      }
      JsonNode responseResource = SchemaValidator.validateDocumentForResponse(resourceTypeFactory,
                                                                              resourceType,
                                                                              resourceNode,
                                                                              resource,
                                                                              attributes,
                                                                              excludedAttributes);

      return new UpdateResponse(responseResource, location);
    }
    catch (ScimException ex)
    {
      return new ErrorResponse(ex);
    }
    catch (Exception ex)
    {
      return new ErrorResponse(new InternalServerException(ex.getMessage(), ex, null));
    }
  }

  /**
   * checks if a resource type exists under the given endpoint and will then give the id to the developers
   * custom implementation stored under the found resource type. If no exception occurred the client will be
   * informed of a successful request
   *
   * @param endpoint the resource endpoint that was called
   * @param id the id of the resource that was requested
   * @return an empty response that does not create a response body
   */
  public ScimResponse deleteResource(String endpoint, String id)
  {
    try
    {
      ResourceType resourceType = getResourceType(endpoint);
      ResourceHandler resourceHandler = resourceType.getResourceHandlerImpl();
      resourceHandler.deleteResource(id);
      return new DeleteResponse();
    }
    catch (ScimException ex)
    {
      return new ErrorResponse(ex);
    }
    catch (Exception ex)
    {
      return new ErrorResponse(new InternalServerException(ex.getMessage(), ex, null));
    }
  }

  /**
   * tries to extract the resource type by its endpoint path suffix e.g. "/Users" or "/Groups"
   *
   * @param endpoint the endpoint path suffix
   * @return the resource type registered for the given path
   */
  private ResourceType getResourceType(String endpoint)
  {
    Supplier<String> errorMessage = () -> "no resource found for endpoint '" + endpoint + "'";
    return Optional.ofNullable(resourceTypeFactory.getResourceType(endpoint))
                   .orElseThrow(() -> new BadRequestException(errorMessage.get(), null, ScimType.UNKNOWN_RESOURCE));
  }

  /**
   * creates the meta representation for the response
   *
   * @param resourceType the resource type that holds necessary data like the name of the resource
   * @param location
   * @return the meta json representation
   */
  private Meta getMeta(ResourceType resourceType, String location)
  {
    LocalDateTime now = LocalDateTime.now();
    return Meta.builder()
               // TODO how should the setting of created attribute be enforced in request?
               .created(now)
               .lastModified(now)
               .location(location)
               .resourceType(resourceType.getName())
               .build();
  }

  /**
   * builds the location attribute for the meta-object
   *
   * @param resourceType holds the endpoint definition
   * @param resourceId the id of the resource to which the location should be determined
   * @param getBaseUrlSupplier this supplier is an optional attribute that should be used to supply the
   *          information of the base URL of this application e.g.: https://example.com/scim/v2. This return
   *          value will be used to create the location URL of the resources like
   *          'https://example.com/scim/v2/Users/123456'. If this parameter is not present the application will
   *          try to read a hardcoded URL from the service provider configuration that is also an optional
   *          attribute. If both ways fail an exception will be thrown
   * @return the current location
   */
  private String getLocation(ResourceType resourceType, String resourceId, Supplier<String> getBaseUrlSupplier)
  {
    String baseUrl = getBaseUrlSupplier == null ? null : getBaseUrlSupplier.get();
    if (StringUtils.isBlank(baseUrl))
    {
      // TODO get url from service provider configuration
      log.warn("TODO");
      return "";
    }
    if (!baseUrl.endsWith("/"))
    {
      baseUrl += "/";
    }
    return baseUrl + resourceType.getEndpoint() + "/" + resourceId;
  }
}
