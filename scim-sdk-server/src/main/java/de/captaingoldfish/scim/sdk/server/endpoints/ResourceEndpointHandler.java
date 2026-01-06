package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.IOException;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.SearchRequest;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;
import de.captaingoldfish.scim.sdk.common.response.DeleteResponse;
import de.captaingoldfish.scim.sdk.common.response.EmptyPatchResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.GetResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.response.UpdateResponse;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.EncodingUtils;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.base.ResourceTypeEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.SchemaEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.ServiceProviderEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.features.EndpointType;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.RequestContextException;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.RequestValidatorHandler;
import de.captaingoldfish.scim.sdk.server.etag.ETagHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.filter.resources.FilterResourceResolver;
import de.captaingoldfish.scim.sdk.server.interceptor.Interceptor;
import de.captaingoldfish.scim.sdk.server.patch.PatchRequestHandler;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.msazure.MsAzurePatchComplexValueRebuilder;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.msazure.MsAzurePatchRemoveRebuilder;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.msazure.MsAzurePatchValueSubAttributeRebuilder;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import de.captaingoldfish.scim.sdk.server.schemas.validation.AbstractResourceValidator;
import de.captaingoldfish.scim.sdk.server.schemas.validation.RequestResourceValidator;
import de.captaingoldfish.scim.sdk.server.schemas.validation.RequestSchemaValidator;
import de.captaingoldfish.scim.sdk.server.sort.ResourceNodeComparator;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:28 <br>
 * <br>
 * This class is used to execute the SCIM operations on the specific endpoints
 */
@Slf4j
class ResourceEndpointHandler
{

  /**
   * each created {@link ResourceEndpointHandler} must get hold of a single {@link ServiceProvider} instance
   * which holds the configuration of this service provider implementation
   */
  @Getter
  private final ServiceProvider serviceProvider;

  /**
   * contains a number of patch-workarounds that can be executed on patch-requests to fix illegal
   * operation-setups
   */
  @Getter
  private final List<Supplier<PatchWorkaround>> patchWorkarounds = new ArrayList<>();

  /**
   * this is used to prevent application context pollution in unit tests
   */
  @Getter(AccessLevel.PROTECTED)
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * this constructor was introduced for unit tests to add a specific resourceTypeFactory instance which will
   * prevent application context pollution within unit tests
   */
  protected ResourceEndpointHandler(ServiceProvider serviceProvider, EndpointDefinition... endpointDefinitions)
  {
    this.resourceTypeFactory = new ResourceTypeFactory();
    this.serviceProvider = serviceProvider;
    List<EndpointDefinition> endpointDefinitionList = new ArrayList<>(Arrays.asList(endpointDefinitions));

    registerEndpoint(new ServiceProviderEndpointDefinition(serviceProvider));
    registerEndpoint(new ResourceTypeEndpointDefinition(resourceTypeFactory));
    registerEndpoint(new SchemaEndpointDefinition(resourceTypeFactory));
    endpointDefinitionList.forEach(this::registerEndpoint);
    addDefaultPatchWorkarounds();
  }

  /**
   * adds the default patch-workaround-handlers to the given serviceprovider configuration
   */
  public void addDefaultPatchWorkarounds()
  {
    patchWorkarounds.add(MsAzurePatchRemoveRebuilder::new);
    patchWorkarounds.add(MsAzurePatchValueSubAttributeRebuilder::new);
    patchWorkarounds.add(MsAzurePatchComplexValueRebuilder::new);
  }

  /**
   * registers a new endpoint
   *
   * @param endpointDefinition the endpoint to register that will override an existing one if one is already
   *          present
   */
  public ResourceType registerEndpoint(EndpointDefinition endpointDefinition)
  {
    ResourceType resourceType = resourceTypeFactory.registerResourceType(endpointDefinition.getResourceHandler(),
                                                                         endpointDefinition.getResourceType(),
                                                                         endpointDefinition.getResourceSchema(),
                                                                         endpointDefinition.getResourceSchemaExtensions()
                                                                                           .toArray(new JsonNode[0]));
    ResourceHandler resourceHandler = resourceType.getResourceHandlerImpl();
    resourceHandler.setServiceProvider(serviceProvider);
    resourceHandler.setResourceType(resourceType);
    Schema mainSchema = resourceType.getMainSchema();
    resourceHandler.setSchema(mainSchema);
    resourceHandler.setSchemaExtensions(resourceType.getAllSchemas()
                                                    .stream()
                                                    .filter(schema -> !schema.getId()
                                                                             .get()
                                                                             .equals(mainSchema.getId().get()))
                                                    .collect(Collectors.toList()));
    resourceHandler.setChangePasswordSupported(() -> serviceProvider.getChangePasswordConfig().isSupported());
    resourceHandler.setMaxResults(() -> serviceProvider.getFilterConfig().getMaxResults());
    resourceHandler.setGetResourceTypeByRef(resourceTypeRefValue -> {
      return getResourceTypeByName((String)resourceTypeRefValue).orElseGet(() -> {
        if (resourceTypeRefValue == null)
        {
          return null;
        }
        final String[] urlParts = ((String)resourceTypeRefValue).split("/");
        final String idOrResourceTypeEndpoint = "/" + urlParts[urlParts.length - 1];
        if (urlParts.length < 2)
        {
          return resourceTypeFactory.getResourceType(idOrResourceTypeEndpoint);
        }
        final String resourceTypeEndpointOrV2 = "/" + urlParts[urlParts.length - 2];
        return Optional.ofNullable(resourceTypeFactory.getResourceType(resourceTypeEndpointOrV2))
                       .orElseGet(() -> resourceTypeFactory.getResourceType(idOrResourceTypeEndpoint));
      });
    });
    resourceHandler.postConstruct(resourceType);
    return resourceType;
  }

  /**
   * will get a resource type definition by its name
   *
   * @param name the name of the resource type e.g. User, Group, ServiceProviderConfig, ResourceType, Schema
   * @return the resource type if one is registered under the given id
   */
  public Optional<ResourceType> getResourceTypeByName(String name)
  {
    return resourceTypeFactory.getResourceTypeByName(name);
  }

  /**
   * @return the names of all resource types that have been registered
   */
  public Set<String> getRegisteredResourceTypeNames()
  {
    return resourceTypeFactory.getAllResourceTypes().stream().map(ResourceType::getName).collect(Collectors.toSet());
  }

  /**
   * @return all registered resource types
   */
  public Set<ResourceType> getRegisteredResourceTypes()
  {
    return new HashSet<>(resourceTypeFactory.getAllResourceTypes());
  }

  /**
   * @return all resource schemata that have been registered on this SCIM provider
   */
  public Set<Schema> getRegisteredSchemas()
  {
    return resourceTypeFactory.getSchemaFactory().getAllResourceSchemas();
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
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   * @return the scim response for the client
   */
  protected ScimResponse createResource(String endpoint,
                                        String resourceDocument,
                                        Supplier<String> baseUrlSupplier,
                                        Context context)
  {
    ResourceHandler resourceHandler = null;
    ResourceNode resourceNode = null;
    ResourceNode resourceNodeCreated = null;
    try
    {
      if (StringUtils.isBlank(resourceDocument))
      {
        throw new BadRequestException("the request body is empty", null, ScimType.Custom.INVALID_PARAMETERS);
      }
      ResourceType resourceType = getResourceType(endpoint);
      JsonNode resource;
      try
      {
        resource = JsonHelper.readJsonDocument(resourceDocument);
      }
      catch (IOException ex)
      {
        throw new BadRequestException(ex.getMessage(), ex, ScimType.Custom.UNPARSEABLE_REQUEST);
      }
      resourceHandler = resourceType.getResourceHandlerImpl();
      RequestResourceValidator resourceValidator = new RequestResourceValidator(serviceProvider, resourceType,
                                                                                HttpMethod.POST);
      resourceNode = (ResourceNode)resourceValidator.validateDocument(resource);
      Meta meta = resourceNode.getMeta().orElse(Meta.builder().build());
      meta.setResourceType(resourceType.getName());
      resourceNode.remove(AttributeNames.RFC7643.META);
      resourceNode.setMeta(meta);
      new RequestValidatorHandler(resourceHandler, resourceValidator, context).validateCreate(resourceNode);
      Interceptor interceptor = resourceHandler.getInterceptor(EndpointType.CREATE);
      ResourceNode finalResourceNode = resourceNode;
      resourceNodeCreated = interceptor.doAround(() -> {
        return resourceType.getResourceHandlerImpl().createResource(finalResourceNode, context);
      }, context);
      if (resourceNodeCreated == null)
      {
        throw new NotImplementedException("create was not implemented for resourceType '" + resourceType.getName()
                                          + "'");
      }
      String resourceId = resourceNodeCreated.getId().orElseThrow(() -> {
        String errorMessage = "ID attribute not set on created resource";
        return new InternalServerException(errorMessage, null, null);
      });
      final String location = getLocation(resourceType, resourceId, baseUrlSupplier);
      Meta createdMeta = resourceNodeCreated.getMeta().orElseThrow(() -> {
        String metaErrorMessage = "Meta attribute not set on created resource";
        return new InternalServerException(metaErrorMessage, null, null);
      });
      if (!createdMeta.getLastModified().isPresent())
      {
        createdMeta.setLastModified(createdMeta.getCreated().orElse(null));
      }
      if (meta.getLocation().isPresent())
      {
        createdMeta.setLocation(meta.getLocation().get());
      }
      else
      {
        createdMeta.setLocation(location);
      }
      createdMeta.setResourceType(resourceType.getName());
      ETagHandler.getResourceVersion(serviceProvider, resourceType, resourceNodeCreated)
                 .ifPresent(createdMeta::setVersion);
      Optional<AbstractResourceValidator> responseValidator = //
        resourceHandler.getResponseValidator(null, null, resource, getReferenceUrlSupplier(baseUrlSupplier));
      JsonNode responseResource = resourceNodeCreated;
      if (responseValidator.isPresent())
      {
        responseResource = responseValidator.get().validateDocument(resourceNodeCreated);
      }
      return new CreateResponse(responseResource, location, createdMeta);
    }
    catch (RequestContextException ex)
    {
      if (resourceHandler != null && resourceNode != null)
      {
        resourceHandler.rollbackCreate(Optional.ofNullable(resourceNodeCreated).orElse(resourceNode), context, ex);
      }
      ErrorResponse errorResponse = new ErrorResponse(ex);
      ex.getValidationContext().writeToErrorResponse(errorResponse);
      return errorResponse;
    }
    catch (ScimException ex)
    {
      if (resourceHandler != null && resourceNode != null)
      {
        resourceHandler.rollbackCreate(Optional.ofNullable(resourceNodeCreated).orElse(resourceNode), context, ex);
      }
      return new ErrorResponse(ex);
    }
    catch (Exception ex)
    {
      if (resourceHandler != null && resourceNode != null)
      {
        resourceHandler.rollbackCreate(Optional.ofNullable(resourceNodeCreated).orElse(resourceNode), context, ex);
      }
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
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   * @return the scim response for the client
   */
  protected ScimResponse getResource(String endpoint,
                                     String id,
                                     String attributes,
                                     String excludedAttributes,
                                     Supplier<String> baseUrlSupplier,
                                     Context context)
  {
    try
    {
      ResourceType resourceType = getResourceType(endpoint);
      ResourceHandler resourceHandler = resourceType.getResourceHandlerImpl();
      final List<SchemaAttribute> attributesList = RequestUtils.getAttributes(resourceType, attributes);
      final List<SchemaAttribute> excludedAttributesList = RequestUtils.getAttributes(resourceType, excludedAttributes);
      Interceptor interceptor = resourceHandler.getInterceptor(EndpointType.GET);
      ResourceNode resourceNode = interceptor.doAround(() -> {
        return resourceHandler.getResource(id, attributesList, excludedAttributesList, context);
      }, context);
      if (resourceNode == null)
      {
        throw new ResourceNotFoundException("the '" + resourceType.getName() + "' resource with id '" + id + "' does "
                                            + "not exist", null, null);
      }
      ETagHandler.validateVersion(serviceProvider,
                                  resourceType,
                                  () -> resourceNode,
                                  context.getUriInfos().getHttpHeaders());
      String resourceId = resourceNode.getId().orElse(null);
      if (resourceId != null && !resourceId.equals(id))
      {
        ResourceTypeFeatures resourceTypeFeatures = resourceType.getFeatures();
        if (resourceTypeFeatures != null && !resourceTypeFeatures.isSingletonEndpoint())
        {
          throw new InternalServerException("the id of the returned resource does not match the "
                                            + "requested id: requestedId: '" + id + "', returnedId: '" + resourceId
                                            + "'", null, null);
        }
      }
      final String location = getLocation(resourceType, resourceId, baseUrlSupplier);
      resourceNode.getMeta().ifPresent(meta -> {
        if (!meta.getLastModified().isPresent())
        {
          meta.setLastModified(meta.getCreated().orElse(null));
        }
        if (!meta.getLocation().isPresent())
        {
          meta.setLocation(location);
        }
        meta.setResourceType(resourceType.getName());
        ETagHandler.getResourceVersion(serviceProvider, resourceType, resourceNode).ifPresent(meta::setVersion);
      });

      Optional<AbstractResourceValidator> responseValidator = //
        resourceHandler.getResponseValidator(attributesList,
                                             excludedAttributesList,
                                             null,
                                             getReferenceUrlSupplier(baseUrlSupplier));
      JsonNode responseResource = resourceNode;
      if (responseValidator.isPresent())
      {
        responseResource = responseValidator.get().validateDocument(resourceNode);
      }
      return new GetResponse(responseResource, location, resourceNode.getMeta().orElse(null));
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
   * Clients MAY execute queries without passing parameters on the URL by using the HTTP POST verb combined with
   * the "/.search" path extension. The inclusion of "/.search" on the end of a valid SCIM endpoint SHALL be
   * used to indicate that the HTTP POST verb is intended to be a query operation.
   *
   * @param endpoint the resource endpoint that was called. This string should only contain the
   *          resources-endpoint not the "/.search" extension e.g. "/Users" or "Users".
   * @param searchRequest the JSON request body of the search request if the request was sent over POST
   * @param baseUrlSupplier this supplier is an optional attribute that should be used to supply the information
   *          of the base URL of this application e.g.: https://example.com/scim/v2. This return value will be
   *          used to create the location URL of the resources like 'https://example.com/scim/v2/Users/123456'.
   *          If this parameter is not present the application will try to read a hardcoded URL from the service
   *          provider configuration that is also an optional attribute. If both ways fail an exception will be
   *          thrown
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   * @return a {@link ListResponse} with all returned resources or an {@link ErrorResponse}
   */
  protected ScimResponse listResources(String endpoint,
                                       String searchRequest,
                                       Supplier<String> baseUrlSupplier,
                                       Context context)
  {
    return listResources(endpoint,
                         StringUtils.isBlank(searchRequest) ? SearchRequest.builder().build()
                           : JsonHelper.readJsonDocument(searchRequest, SearchRequest.class),
                         baseUrlSupplier,
                         context);
  }

  /**
   * Clients MAY execute queries without passing parameters on the URL by using the HTTP POST verb combined with
   * the "/.search" path extension. The inclusion of "/.search" on the end of a valid SCIM endpoint SHALL be
   * used to indicate that the HTTP POST verb is intended to be a query operation.
   *
   * @param endpoint the resource endpoint that was called. This string should only contain the
   *          resources-endpoint * not the "/.search" extension e.g. "/Users" or "Users".
   * @param searchRequest the JSON request body of the search request if the request was sent over POST
   * @param baseUrlSupplier this supplier is an optional attribute that should be used to supply the information
   *          of the base URL of this application e.g.: https://example.com/scim/v2. This return value will be
   *          used to create the location URL of the resources like 'https://example.com/scim/v2/Users/123456'.
   *          If this parameter is not present the application will try to read a hardcoded URL from the service
   *          provider configuration that is also an optional attribute. If both ways fail an exception will be
   *          thrown
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   * @return a {@link ListResponse} with all returned resources or an {@link ErrorResponse}
   */
  protected ScimResponse listResources(String endpoint,
                                       SearchRequest searchRequest,
                                       Supplier<String> baseUrlSupplier,
                                       Context context)
  {
    return listResources(endpoint,
                         searchRequest.getStartIndex().orElse(null),
                         searchRequest.getCount().orElse(null),
                         searchRequest.getFilter().orElse(null),
                         searchRequest.getSortBy().orElse(null),
                         searchRequest.getSortOrder().orElse(null),
                         searchRequest.getAttributes(),
                         searchRequest.getExcludedAttributes(),
                         baseUrlSupplier,
                         context);
  }

  /**
   * Clients MAY execute queries without passing parameters on the URL by using the HTTP POST verb combined with
   * the "/.search" path extension. The inclusion of "/.search" on the end of a valid SCIM endpoint SHALL be
   * used to indicate that the HTTP POST verb is intended to be a query operation.
   *
   * @param endpoint the resource endpoint that was called e.g. "/Users" or "Users".
   * @param startIndex The 1-based index of the first query result. A value less than 1 SHALL be interpreted as
   *          1.<br>
   *          <b>DEFAULT:</b> 1
   * @param count Non-negative integer. Specifies the desired maximum number of query results per page, e.g.,
   *          10. A negative value SHALL be interpreted as "0". A value of "0" indicates that no resource
   *          results are to be returned except for "totalResults". <br>
   *          <b>DEFAULT:</b> None<br>
   *          When specified, the service provider MUST NOT return more results than specified, although it MAY
   *          return fewer results. If unspecified, the maximum number of results is set by the service
   *          provider.
   * @param filter Filtering is an OPTIONAL parameter for SCIM service providers. Clients MAY discover service
   *          provider filter capabilities by looking at the "filter" attribute of the "ServiceProviderConfig"
   *          endpoint. Clients MAY request a subset of resources by specifying the "filter" query parameter
   *          containing a filter expression. When specified, only those resources matching the filter
   *          expression SHALL be returned. The expression language that is used with the filter parameter
   *          supports references to attributes and literals.
   * @param sortBy The "sortBy" parameter specifies the attribute whose value SHALL be used to order the
   *          returned responses. If the "sortBy" attribute corresponds to a singular attribute, resources are
   *          sorted according to that attribute's value; if it's a multi-valued attribute, resources are sorted
   *          by the value of the primary attribute (see Section 2.4 of [RFC7643]), if any, or else the first
   *          value in the list, if any. If the attribute is complex, the attribute name must be a path to a
   *          sub-attribute in standard attribute notation (Section 3.10), e.g., "sortBy=name.givenName". For
   *          all attribute types, if there is no data for the specified "sortBy" value, they are sorted via the
   *          "sortOrder" parameter, i.e., they are ordered last if ascending and first if descending.
   * @param sortOrder The order in which the "sortBy" parameter is applied. Allowed values are "ascending" and
   *          "descending". If a value for "sortBy" is provided and no "sortOrder" is specified, "sortOrder"
   *          SHALL default to ascending. String type attributes are case insensitive by default, unless the
   *          attribute type is defined as a case-exact string. "sortOrder" MUST sort according to the attribute
   *          type; i.e., for case-insensitive attributes, sort the result using case-insensitive Unicode
   *          alphabetic sort order with no specific locale implied, and for case-exact attribute types, sort
   *          the result using case-sensitive Unicode alphabetic sort order.
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
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   * @return a {@link ListResponse} with all returned resources or an {@link ErrorResponse}
   */
  protected <T extends ResourceNode> ScimResponse listResources(String endpoint,
                                                                Long startIndex,
                                                                Integer count,
                                                                String filter,
                                                                String sortBy,
                                                                String sortOrder,
                                                                List<String> attributes,
                                                                List<String> excludedAttributes,
                                                                Supplier<String> baseUrlSupplier,
                                                                Context context)
  {
    try
    {
      final ResourceType resourceType = getResourceType(endpoint);
      final long effectiveStartIndex = RequestUtils.getEffectiveStartIndex(startIndex);
      final int effectiveCount = RequestUtils.getEffectiveCount(serviceProvider, count);
      final FilterNode filterNode = getFilterNode(resourceType, filter);
      final boolean autoFiltering = resourceType.getFeatures().isAutoFiltering();
      final SchemaAttribute sortByAttribute = getSortByAttribute(resourceType, sortBy);
      final SortOrder sortOrdering = getSortOrdering(sortOrder, sortByAttribute);
      final boolean autoSorting = resourceType.getFeatures().isAutoSorting();
      final List<SchemaAttribute> attributesList = RequestUtils.getAttributes(resourceType, attributes);
      final List<SchemaAttribute> excludedAttributesList = RequestUtils.getAttributes(resourceType, excludedAttributes);

      ResourceHandler<T> resourceHandler = resourceType.getResourceHandlerImpl();
      Interceptor interceptor = resourceHandler.getInterceptor(EndpointType.LIST);
      PartialListResponse<T> resources = interceptor.doAround(() -> {
        return resourceHandler.listResources(effectiveStartIndex,
                                             effectiveCount,
                                             autoFiltering ? null : filterNode,
                                             autoSorting ? null : sortByAttribute,
                                             autoSorting ? null : sortOrdering,
                                             attributesList,
                                             excludedAttributesList,
                                             context);
      }, context);
      if (resources == null)
      {
        throw new NotImplementedException("listResources was not implemented for resourceType '"
                                          + resourceType.getName() + "'");
      }

      List<T> resourceList = resources.getResources();
      List<T> filteredResources = filterResources(filterNode, resourceList, resourceType);
      filteredResources = sortResources(filteredResources, sortByAttribute, sortOrdering, resourceType);

      long totalResults = resourceList.size() != filteredResources.size() ? filteredResources.size()
        : (resources.getTotalResults() == 0 ? filteredResources.size() : resources.getTotalResults());

      // override filteredResources only in case of auto-filtering since we expect the implementation to handle
      // everything if auto-filtering is deactivated
      if (autoFiltering)
      {
        // this if-block will assert that no more results will be returned than the countValue allows.
        if (effectiveStartIndex <= filteredResources.size())
        {
          filteredResources = filteredResources.subList((int)Math.min(effectiveStartIndex - 1,
                                                                      filteredResources.size() - 1),
                                                        (int)Math.min(effectiveStartIndex - 1 + effectiveCount,
                                                                      filteredResources.size()));
        }
        else
        {
          log.debug("startIndex '{}' is > than number of entries available '{}'. Returning empty list",
                    effectiveStartIndex,
                    filteredResources.size());
          filteredResources = Collections.emptyList();
        }
      }
      if (filteredResources.size() > effectiveCount)
      {
        log.warn("The service provider tried to return more results than allowed. Tried to return '{}' results. "
                 + "The list will be reduced to '{}' results",
                 filteredResources.size(),
                 effectiveCount);
        filteredResources = filteredResources.subList(0, effectiveCount);
      }

      List<JsonNode> validatedResourceList = new ArrayList<>();
      for ( ResourceNode resourceNode : filteredResources )
      {
        final String location = getLocation(resourceType, resourceNode.getId().orElse(null), baseUrlSupplier);
        log.trace("Determined resource location at '{}'", location);
        resourceNode.getMeta().ifPresent(meta -> {
          if (!meta.getLastModified().isPresent())
          {
            meta.setLastModified(meta.getCreated().orElse(null));
          }
          if (!meta.getLocation().isPresent())
          {
            meta.setLocation(location);
          }
          meta.setResourceType(resourceType.getName());
          ETagHandler.getResourceVersion(serviceProvider, resourceType, resourceNode).ifPresent(meta::setVersion);
        });

        Optional<AbstractResourceValidator> responseValidator = //
          resourceHandler.getResponseValidator(attributesList,
                                               excludedAttributesList,
                                               null,
                                               getReferenceUrlSupplier(baseUrlSupplier));
        JsonNode responseResource = resourceNode;
        if (responseValidator.isPresent())
        {
          responseResource = responseValidator.get().validateDocument(resourceNode);
        }

        validatedResourceList.add(responseResource);
      }

      return new ListResponse<T>(validatedResourceList, totalResults, validatedResourceList.size(),
                                 effectiveStartIndex);
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
   * this method will sort the resources based on the given attribute and the ordering
   *
   * @param filteredResources the resources that might have already been filtered
   * @param sortByAttribute the sortby attribute that tells us which attribute should be used for sorting
   * @param sortOrdering the sort order to use
   * @return the ordered resources
   */
  private <T extends ResourceNode> List<T> sortResources(List<T> filteredResources,
                                                         SchemaAttribute sortByAttribute,
                                                         SortOrder sortOrdering,
                                                         ResourceType resourceType)
  {
    if (!serviceProvider.getSortConfig().isSupported() || sortByAttribute == null
        || !resourceType.getFeatures().isAutoSorting())
    {
      log.trace("Auto-sorting skipped for auto-sorting is not supported or missing sortBy attribute");
      return filteredResources;
    }
    log.trace("Starting auto sorting resources by attribute '{}' in order '{}'",
              sortByAttribute.getFullResourceName(),
              sortOrdering);
    try
    {
      return serviceProvider.getThreadPool()
                            .submit(() -> filteredResources.parallelStream()
                                                           .sorted(new ResourceNodeComparator(sortByAttribute,
                                                                                              sortOrdering))
                                                           .collect(Collectors.toList()))
                            .get();
    }
    catch (InterruptedException | ExecutionException e)
    {
      throw new InternalServerException(e);
    }
  }

  /**
   * this method executes filtering on the given resource list
   *
   * @param filterNode the filter expression from the client. Might be null if filtering is disabled
   * @param resourceList the list that should be filtered
   * @param resourceType the resource type must have filtering enabled. If filtering is not explicitly enabled
   *          the developer must do the filtering manually
   * @return the filtered list or the {@code resourceList}
   */
  protected <T extends ResourceNode> List<T> filterResources(FilterNode filterNode,
                                                             List<T> resourceList,
                                                             ResourceType resourceType)
  {
    boolean isApplicationFilteringEnabled = resourceType.getFeatures().isAutoFiltering();
    List<T> filteredResourceType;
    if (isApplicationFilteringEnabled && filterNode != null)
    {
      log.trace("Starting with auto filtering resources");
      filteredResourceType = FilterResourceResolver.filterResources(serviceProvider, resourceList, filterNode);
    }
    else
    {
      log.trace("No filtering performed");
      filteredResourceType = resourceList;
    }
    return filteredResourceType;
  }

  /**
   * checks if the given filter expression must be evaluated or not. If the filtering is disabled in the
   * {@link ServiceProvider} instance the filter expression is ignored and a debug message is printed
   *
   * @param resourceType the resource type on which the filter expression should be evaluated
   * @param filter the filter expression that should be evaluated
   * @return a filter tree structure that makes resolving the filter very easy
   */
  private FilterNode getFilterNode(ResourceType resourceType, String filter)
  {
    if (StringUtils.isBlank(filter))
    {
      log.trace("No filter expression found in request");
      return null;
    }
    if (serviceProvider.getFilterConfig().isSupported())
    {
      log.trace("Evaluating filter expression '{}' for resourceType '{}'", filter, resourceType.getName());
      return RequestUtils.parseFilter(resourceType, filter);
    }
    log.debug("Filter expression '{}' is not evaluated because filter support is disabled", filter);
    return null;
  }

  /**
   * will parse the sortBy value to a {@link SchemaAttribute} from the {@link ResourceType} if sorting is
   * enabled and the sortBy attribute is provided
   *
   * @param resourceType the resourceType to which the sortBy value must apply
   * @param sortBy the sortBy value that will be evaluated
   * @return either the {@link SchemaAttribute} that applies to the sortBy value or null
   */
  private SchemaAttribute getSortByAttribute(ResourceType resourceType, String sortBy)
  {
    if (StringUtils.isBlank(sortBy))
    {
      log.trace("No sortBy attribute found in request");
      return null;
    }
    if (serviceProvider.getSortConfig().isSupported())
    {
      log.trace("Evaluating sortBy attribute '{}' for resourceType '{}'", sortBy, resourceType.getName());
      return RequestUtils.getSchemaAttributeByAttributeName(resourceType, sortBy);
    }
    log.debug("sortBy value '{}' is ignored because sorting support is disabled", sortBy);
    return null;
  }

  /**
   * will parse the sortOrdering value to a {@link SortOrder} instance if sorting is enabled and the sortOrder
   * value is provided. If the sortBy value is not null and was evaluated before but the sortOrder value is not
   * present it will default to value {@link SortOrder#ASCENDING}
   *
   * @param sortOrder the sortOrder value specified by the client
   * @param sortBy the sortBy value specified by the client
   * @return the sortOrder value if the value was specified and sorting is enabled or null
   */
  private SortOrder getSortOrdering(String sortOrder, SchemaAttribute sortBy)
  {
    if (StringUtils.isBlank(sortOrder))
    {
      if (sortBy != null)
      {
        log.trace("No sortBy attribute found in request. Using default '{}'", SortOrder.ASCENDING);
        // If a value for "sortBy" is provided and no "sortOrder" is specified, "sortOrder" SHALL default to ascending
        return SortOrder.ASCENDING;
      }
    }
    if (serviceProvider.getSortConfig().isSupported())
    {
      log.trace("Evaluating sortOrder attribute '{}'", sortOrder);
      return SortOrder.getByValue(sortOrder);
    }
    log.debug("SortOrder value '{}' is ignored because sorting support is disabled", sortOrder);
    return null;
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
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   * @return the scim response for the client
   */
  protected ScimResponse updateResource(String endpoint,
                                        String id,
                                        String resourceDocument,
                                        Supplier<String> baseUrlSupplier,
                                        Context context)
  {
    ResourceHandler resourceHandler = null;
    ResourceNode resourceNode = null;
    try
    {
      if (StringUtils.isBlank(resourceDocument))
      {
        throw new BadRequestException("the request body is empty", null, ScimType.Custom.INVALID_PARAMETERS);
      }
      ResourceType resourceType = getResourceType(endpoint);
      resourceHandler = resourceType.getResourceHandlerImpl();
      JsonNode resource;
      try
      {
        resource = JsonHelper.readJsonDocument(resourceDocument);
      }
      catch (IOException ex)
      {
        throw new BadRequestException(ex.getMessage(), ex, ScimType.Custom.UNPARSEABLE_REQUEST);
      }
      RequestResourceValidator requestResourceValidator = new RequestResourceValidator(serviceProvider, resourceType,
                                                                                       HttpMethod.PUT);
      ResourceNode resourceNodeForUpdate = (ResourceNode)requestResourceValidator.validateDocument(resource);

      if (resource == null)
      {
        throw new BadRequestException("the request body does not contain any writable parameters", null,
                                      ScimType.Custom.UNPARSEABLE_REQUEST);
      }
      if (resourceNodeForUpdate == null)
      {
        throw new ResourceNotFoundException("the '" + resourceType.getName() + "' resource with id '" + id + "' does "
                                            + "not exist", null, null);
      }
      resourceNodeForUpdate.setId(id);
      final String location = getLocation(resourceType, id, baseUrlSupplier);
      Meta meta = resourceNodeForUpdate.getMeta().orElse(Meta.builder().build());
      resourceNodeForUpdate.remove(AttributeNames.RFC7643.META);
      meta.setLocation(location);
      meta.setResourceType(resourceType.getName());
      resourceNodeForUpdate.setMeta(meta);

      AtomicReference<ResourceNode> oldResourceNode = new AtomicReference<>();
      Supplier<ResourceNode> oldResourceSupplier = () -> {
        ResourceNode oldResource = resourceType.getResourceHandlerImpl()
                                               .getResourceForUpdate(id, null, null, context, EndpointType.UPDATE);
        oldResourceNode.compareAndSet(null, oldResource);
        return oldResourceNode.get();
      };
      Interceptor interceptor = resourceHandler.getInterceptor(EndpointType.UPDATE);
      resourceNode = interceptor.doAround(() -> {
        validateResourceVersion(id, resourceType, oldResourceSupplier, context.getUriInfos().getHttpHeaders());
        ResourceHandler rh = resourceType.getResourceHandlerImpl();
        new RequestValidatorHandler(rh, requestResourceValidator, context).validateUpdate(oldResourceSupplier,
                                                                                          resourceNodeForUpdate);
        return rh.updateResource(resourceNodeForUpdate, context);
      }, context);
      if (resourceNode == null)
      {
        throw new ResourceNotFoundException("the '" + resourceType.getName() + "' resource with id '" + id + "' does "
                                            + "not exist", null, null);
      }
      Supplier<String> metaErrorMessage = () -> "Meta attribute not set on created resource";
      Meta createdMeta = resourceNode.getMeta()
                                     .orElseThrow(() -> new InternalServerException(metaErrorMessage.get(), null,
                                                                                    null));
      if (!createdMeta.getLastModified().isPresent())
      {
        createdMeta.setLastModified(createdMeta.getCreated().orElse(null));
      }
      if (!createdMeta.getLocation().isPresent())
      {
        createdMeta.setLocation(location);
      }
      createdMeta.setResourceType(resourceType.getName());
      ETagHandler.getResourceVersion(serviceProvider, resourceType, resourceNode).ifPresent(createdMeta::setVersion);
      Supplier<String> errorMessage = () -> "ID attribute not set on updated resource";
      String resourceId = resourceNode.getId()
                                      .orElseThrow(() -> new InternalServerException(errorMessage.get(), null, null));
      if (!resourceId.equals(id) && !resourceType.getFeatures().isSingletonEndpoint())
      {
        throw new InternalServerException("the id of the returned resource does not match the "
                                          + "requested id: requestedId: '" + id + "', returnedId: '" + resourceId + "'",
                                          null, null);
      }
      Optional<AbstractResourceValidator> responseValidator = //
        resourceHandler.getResponseValidator(null, null, resourceNode, getReferenceUrlSupplier(baseUrlSupplier));
      JsonNode responseResource = resourceNode;
      if (responseValidator.isPresent())
      {
        responseResource = responseValidator.get().validateDocument(resourceNode);
      }

      return new UpdateResponse(responseResource, location, meta);
    }
    catch (RequestContextException ex)
    {
      if (resourceHandler != null && resourceNode != null)
      {
        resourceHandler.rollbackUpdate(resourceNode, context, ex);
      }
      ErrorResponse errorResponse = new ErrorResponse(ex);
      ex.getValidationContext().writeToErrorResponse(errorResponse);
      return errorResponse;
    }
    catch (ScimException ex)
    {
      if (resourceHandler != null && resourceNode != null)
      {
        resourceHandler.rollbackUpdate(resourceNode, context, ex);
      }
      return new ErrorResponse(ex);
    }
    catch (Exception ex)
    {
      if (resourceHandler != null && resourceNode != null)
      {
        resourceHandler.rollbackUpdate(resourceNode, context, ex);
      }
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
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   * @return an empty response that does not create a response body
   */
  protected ScimResponse deleteResource(String endpoint, String id, Map<String, String> httpHeaders, Context context)
  {
    try
    {
      ResourceType resourceType = getResourceType(endpoint);
      ResourceHandler resourceHandler = resourceType.getResourceHandlerImpl();
      Interceptor interceptor = resourceHandler.getInterceptor(EndpointType.DELETE);
      return interceptor.doAround(() -> {
        Supplier<ResourceNode> oldResourceSupplier = () -> {
          return resourceHandler.getResourceForUpdate(id, null, null, context, EndpointType.DELETE);
        };
        validateResourceVersion(id, resourceType, oldResourceSupplier, httpHeaders);
        resourceHandler.deleteResource(id, context);
        return new DeleteResponse();
      }, context);
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
   * gets the resource that should be patched and will inject the patch operations into the returned resource.
   * After the patch operation has been processed the patched object will be given to the
   * {@link ResourceHandler#updateResource(ResourceNode, Context)} method
   *
   * @param endpoint the resource endpoint that was called
   * @param id the id of the resource that should be patched
   * @param requestBody the patch request body
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
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   * @return the updated resource or an error response
   */
  protected ScimResponse patchResource(String endpoint,
                                       String id,
                                       String requestBody,
                                       String attributes,
                                       String excludedAttributes,
                                       Supplier<String> baseUrlSupplier,
                                       Context context)
  {
    ResourceNode updatedResource = null;
    ResourceHandler resourceHandler = null;
    try
    {
      ResourceType resourceType = getResourceType(endpoint);
      resourceHandler = resourceType.getResourceHandlerImpl();
      if (!serviceProvider.getPatchConfig().isSupported())
      {
        throw new NotImplementedException("patch is not supported by this service provider");
      }
      Schema patchSchema = resourceTypeFactory.getSchemaFactory().getMetaSchema(SchemaUris.PATCH_OP);
      JsonNode patchDocument = JsonHelper.readJsonDocument(requestBody);
      if (patchDocument == null)
      {
        throw new BadRequestException("Missing patch request body");
      }
      patchDocument = new RequestSchemaValidator(serviceProvider, ScimObjectNode.class,
                                                 HttpMethod.PATCH).validateDocument(patchSchema, patchDocument);

      PatchOpRequest patchOpRequest = JsonHelper.copyResourceToObject(patchDocument, PatchOpRequest.class);

      PatchRequestHandler patchRequestHandler = new PatchRequestHandler(id, resourceHandler, getPatchWorkarounds(),
                                                                        context);

      final List<SchemaAttribute> attributesList = RequestUtils.getAttributes(resourceType, attributes);
      final List<SchemaAttribute> excludedAttributesList = RequestUtils.getAttributes(resourceType, excludedAttributes);
      Supplier<ResourceNode> oldResourceSupplier = patchRequestHandler.getOldResourceSupplier(id,
                                                                                              attributesList,
                                                                                              excludedAttributesList);

      Interceptor patchInterceptor = resourceHandler.getInterceptor(EndpointType.PATCH);
      updatedResource = patchInterceptor.doAround(() -> {
        ResourceNode resourceNode = null;
        if (serviceProvider.getETagConfig().isSupported())
        {
          resourceNode = oldResourceSupplier.get();
          ETagHandler.validateVersion(serviceProvider,
                                      resourceType,
                                      oldResourceSupplier,
                                      context.getUriInfos().getHttpHeaders());

          Meta meta = resourceNode.getMeta().orElseGet(Meta::new);
          resourceNode.remove(AttributeNames.RFC7643.META);
          final String location = context.getResourceReferenceUrl(id);
          meta.setLocation(location);
          meta.setResourceType(resourceType.getName());
          resourceNode.setMeta(meta);
          ETagHandler.getResourceVersion(serviceProvider, resourceType, resourceNode).ifPresent(meta::setVersion);
        }

        ResourceNode patchedResourceNode = patchRequestHandler.handlePatchRequest(patchOpRequest);
        patchedResourceNode.setId(id);
        Meta meta = patchedResourceNode.getMeta().orElseGet(Meta::new);
        Optional.ofNullable(resourceNode).flatMap(ResourceNode::getMeta).ifPresent(previousMeta -> {
          meta.setResourceType(resourceType.getName());
          meta.setLocation(previousMeta.getLocation().orElse(getLocation(resourceType, id, baseUrlSupplier)));
          meta.setVersion(previousMeta.getVersion().orElse(null));
        });
        return patchRequestHandler.getUpdatedResource(patchedResourceNode, attributesList, excludedAttributesList);
      }, context);

      if (updatedResource == null)
      // can only happen with custom implementations
      {
        return new EmptyPatchResponse(getLocation(resourceType, id, baseUrlSupplier));
      }

      Optional<AbstractResourceValidator> responseValidator = //
        resourceHandler.getResponseValidator(attributesList,
                                             excludedAttributesList,
                                             patchRequestHandler.getRequestedAttributes(),
                                             getReferenceUrlSupplier(baseUrlSupplier));
      JsonNode responseResource = updatedResource;
      if (responseValidator.isPresent())
      {
        responseResource = responseValidator.get().validateDocument(responseResource);
      }

      return new UpdateResponse(responseResource, getLocation(resourceType, id, baseUrlSupplier),
                                updatedResource.getMeta().orElseThrow(() -> {
                                  return new InternalServerException("Missing meta-attribute on patched resource");
                                }));
    }
    catch (RequestContextException ex)
    {
      if (resourceHandler != null && updatedResource != null)
      {
        resourceHandler.rollbackUpdate(updatedResource, context, ex);
      }
      ErrorResponse errorResponse = new ErrorResponse(ex);
      ex.getValidationContext().writeToErrorResponse(errorResponse);
      return errorResponse;
    }
    catch (ScimException ex)
    {
      if (resourceHandler != null && updatedResource != null)
      {
        resourceHandler.rollbackUpdate(updatedResource, context, ex);
      }
      return new ErrorResponse(ex);
    }
    catch (Exception ex)
    {
      if (resourceHandler != null && updatedResource != null)
      {
        resourceHandler.rollbackUpdate(updatedResource, context, ex);
      }
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
    ResourceType resourceType = Optional.ofNullable(resourceTypeFactory.getResourceType(endpoint))
                                        .orElseThrow(() -> new BadRequestException(errorMessage.get(), null,
                                                                                   ScimType.Custom.UNKNOWN_RESOURCE));
    log.trace("Determined resource type '{}'", resourceType.getName());
    return resourceType;
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
    String escapedResourceId = EncodingUtils.urlEncode(resourceId);
    if (StringUtils.isBlank(baseUrl))
    {
      return StringUtils.stripToEmpty(System.getProperty("SCIM_BASE_URL")) + resourceType.getEndpoint()
             + (StringUtils.isBlank(escapedResourceId) ? "" : "/" + escapedResourceId);
    }
    if (baseUrl.endsWith("/"))
    {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl + resourceType.getEndpoint()
           + (StringUtils.isBlank(escapedResourceId) ? "" : "/" + escapedResourceId);
  }

  /**
   * resolves the given baseUrl to a reference url based on the given resourceName and resourceId. This is used
   * to set absolute urls in resource attribute nodes that define the "$ref" attribute but did not set it
   * previously
   *
   * @param baseUrl the base url of this resource endpoint
   * @return the fully qualified url to the given resource or null
   */
  BiFunction<String, String, String> getReferenceUrlSupplier(Supplier<String> baseUrl)
  {
    return (resourceId, resourceName) -> {
      Optional<ResourceType> resourceType = resourceTypeFactory.getResourceTypeByName(resourceName);
      String id = StringUtils.isBlank(resourceId) ? "" : "/" + EncodingUtils.urlEncode(resourceId);
      return resourceType.map(jsonNodes -> baseUrl.get() + jsonNodes.getEndpoint() + id).orElse(null);
    };
  }

  private void validateResourceVersion(String id,
                                       ResourceType resourceType,
                                       Supplier<ResourceNode> oldResourceSupplier,
                                       Map<String, String> httpHeaders)
  {
    try
    {
      ETagHandler.validateVersion(serviceProvider, resourceType, oldResourceSupplier, httpHeaders);
    }
    catch (ResourceNotFoundException ex)
    {
      throw new ResourceNotFoundException("the '" + resourceType.getName() + "' resource with id '" + id + "' does "
                                          + "not exist", ex, null);
    }
  }
}
