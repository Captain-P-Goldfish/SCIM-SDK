package de.captaingoldfish.scim.sdk.server.endpoints.bulkget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import de.captaingoldfish.scim.sdk.common.response.BulkResponseGetOperation;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import lombok.Builder;


/**
 * The bulk-get resolver will do get-requests on transitive resources if the requested resource has any
 * transitive references to other resources
 *
 * @author Pascal Knueppel
 * @since 29.08.2022
 */
public class BulkGetResolver
{

  /**
   * the maximum number of recursions that are allowed to retrieve the children. The minimum value is 1 the
   * maximum value depends on the service providers' configuration.
   */
  private final int maxResourceLevel;

  /**
   * this operation contains the parent whose children shall be extracted
   */
  private final ScimResponse parentResourceResponse;

  /**
   * the resources definition
   */
  private final ResourceType resourceType;

  /**
   * the factory is necessary to get the resource definitions of the transitive types to be able to analyze them
   * correctly
   */
  private final ResourceTypeFactory resourceTypeFactory;

  private final BiFunction<String, ResourceType, ScimResponse> callResourceEndpoint;

  @Builder
  public BulkGetResolver(int maxResourceLevel,
                         ScimResponse parentResourceResponse,
                         ResourceType resourceType,
                         ResourceTypeFactory resourceTypeFactory,
                         BiFunction<String, ResourceType, ScimResponse> callResourceEndpoint)
  {
    this.maxResourceLevel = Math.max(1, maxResourceLevel);
    this.parentResourceResponse = parentResourceResponse;
    this.resourceType = resourceType;
    this.resourceTypeFactory = resourceTypeFactory;
    this.callResourceEndpoint = callResourceEndpoint;
  }

  /**
   * retrieves the referenced resources of the given {@link #parentResourceResponse} e.g. a manager of the
   * enterprise user or the members of a group
   *
   * @return all transitive resources until the given {@link #maxResourceLevel} is reached
   */
  public List<BulkResponseGetOperation> getTransitiveResources()
  {
    return getChildrenOfResource(parentResourceResponse, resourceType, 0);
  }

  /**
   * retrieves the children of the given resource
   *
   * @param scimResponse the resource from which the children should be retrieved
   * @param resourceType the resource type definition of the given resource (scimResponse)
   * @param currentDepth the current depth level to prevent the resources from being retrieved from a level that
   *          is too deep (e.g. infinite loop with resources that reference each other)
   * @return the children of the given resource if any
   */
  private List<BulkResponseGetOperation> getChildrenOfResource(ScimResponse scimResponse,
                                                               ResourceType resourceType,
                                                               int currentDepth)
  {
    if (currentDepth == maxResourceLevel)
    {
      return Collections.emptyList();
    }
    ResourceReferenceExtractor resourceReferenceExtractor = new ResourceReferenceExtractor(scimResponse, resourceType,
                                                                                           resourceTypeFactory);
    List<ResourceReference> resourceReferences = resourceReferenceExtractor.getResourceReferences();
    return resourceReferences.stream().map((ResourceReference resourceReference) -> {
      return getChildResource(resourceReference, currentDepth);
    }).collect(Collectors.toList());
  }

  /**
   * retrieves a single child resource
   *
   * @param resourceReference the reference to a child resource
   * @param currentDepth the current depth level to prevent the resources from being retrieved from a level that
   *          is too deep (e.g. infinite loop with resources that reference each other)
   * @return the retrieved child resource
   */
  private BulkResponseGetOperation getChildResource(ResourceReference resourceReference, int currentDepth)
  {
    final String resourceId = resourceReference.getResourceId();
    final ResourceType childResourceType = resourceReference.getResourceType();
    final String nodePath = resourceReference.getNodePath();

    ScimResponse scimResponse = callResourceEndpoint.apply(resourceId, childResourceType);

    List<BulkResponseGetOperation> children = new ArrayList<>();
    if (currentDepth < maxResourceLevel)
    {
      children = getChildrenOfResource(scimResponse, childResourceType, currentDepth + 1);
    }

    return BulkResponseGetOperation.builder()
                                   .resource(scimResponse)
                                   .resourceId(resourceId)
                                   .status(scimResponse.getHttpStatus())
                                   .resourceType(childResourceType.getName())
                                   .nodePath(nodePath)
                                   .children(children)
                                   .build();
  }
}
