package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.utils.UriInfos;


/**
 * author Pascal Knueppel <br>
 * created at: 21.08.2022 - 09:24 <br>
 * <br>
 * this class is used to resolve the bulkIds of different resources of a bulk request and to detect circular
 * references
 */
public class BulkIdResolver
{

  /**
   * this map is used to map the ids of newly created resources to bulkIds
   */
  private final Map<String, String> resolvedBulkIds = new HashMap<>();

  /**
   * contains the bulkId reference details of the different request operations<br>
   * <br>
   * key: bulkId of the request operation <br>
   * value: details of the resolved operations
   */
  private final Map<String, BulkIdResolverAbstract> bulkIdResourceResolverMap = new HashMap<>();

  /**
   * used to check for circular references within the request operation
   */
  private final CircularReferenceDetector circularReferenceDetector = new CircularReferenceDetector();

  /**
   * will replace the bulkId references of all registered operations that do match the given bulkId with the
   * given value
   *
   * @param bulkId the bulkId references that should be resolved
   * @param resourceId the value that will replace the bulkId references
   */
  public void addResolvedBulkId(String bulkId, String resourceId)
  {
    if (StringUtils.isBlank(bulkId))
    {
      return;
    }
    resolvedBulkIds.put(bulkId, resourceId);

    List<String> completelyResolvedOperations = new ArrayList<>();
    bulkIdResourceResolverMap.values().forEach(resolver -> {
      resolver.replaceBulkIdNode(bulkId, resourceId);
      if (!resolver.hasAnyBulkIdReferences())
      {
        completelyResolvedOperations.add(bulkId);
      }
    });
    completelyResolvedOperations.forEach(bulkIdResourceResolverMap::remove);
  }

  /**
   * gets an already created bulkId resolver for the given bulkId if present
   */
  public Optional<BulkIdResolverAbstract> getBulkIdResolver(String bulkId)
  {
    return Optional.ofNullable(bulkIdResourceResolverMap.get(bulkId));
  }

  /**
   * analyzes the given resource for bulkId references and stores them and will also resolve them immediately if
   * already possible
   *
   * @param operationBulkId the bulkId of the bulk operation that represents the given resource
   * @param operationUriInfo the uri information of the currently accessed resource-type
   * @param resourceString the resource itself
   */
  public BulkIdResolverAbstract createNewBulkIdResolver(String operationBulkId,
                                                        UriInfos operationUriInfo,
                                                        String resourceString)
  {
    // if the abstractBulkIdResolver is not null the resource was already analyzed, and therefore we do not need
    // to check again for bulkIds and illegal references
    final ScimObjectNode resource;
    if (HttpMethod.PATCH.equals(operationUriInfo.getHttpMethod()))
    {
      resource = JsonHelper.readJsonDocument(resourceString, PatchOpRequest.class);
    }
    else
    {
      resource = JsonHelper.readJsonDocument(resourceString, ScimObjectNode.class);
    }

    BulkIdResolverAbstract bulkIdResolverAbstract;
    if (resource instanceof PatchOpRequest)
    {
      bulkIdResolverAbstract = new BulkIdResolverPatch(operationBulkId, operationUriInfo, (PatchOpRequest)resource);
    }
    else
    {
      bulkIdResolverAbstract = new BulkIdResolverResource(operationBulkId, operationUriInfo, resource);
    }

    bulkIdResolverAbstract.findAllBulkIdReferences();
    checkForSelfReference(bulkIdResolverAbstract);
    checkForCircularReferences(bulkIdResolverAbstract);
    bulkIdResourceResolverMap.put(operationBulkId, bulkIdResolverAbstract);

    if (bulkIdResolverAbstract.hasAnyBulkIdReferences())
    {
      resolvedBulkIds.forEach(bulkIdResolverAbstract::replaceBulkIdNode);
    }
    return bulkIdResolverAbstract;
  }

  /**
   * checks if the given resource contains a reference to itself
   *
   * @param operationBulkId the bulkId operation that represents the resource within the BulkIdResourceResolver
   * @param bulkIdResourceResolver the BulkIdResourceResolver that contains the analyzed resource
   */
  private void checkForSelfReference(BulkIdResolverAbstract bulkIdResourceResolver)
  {
    boolean hasSelfReference = bulkIdResourceResolver.hasSelfReference();
    if (hasSelfReference)
    {
      String errorMessage = String.format("the bulkId '%s' is a self-reference. Self-references will not be resolved",
                                          bulkIdResourceResolver.getOperationBulkId());
      throw new BadRequestException(errorMessage, null, ScimType.RFC7644.INVALID_VALUE);
    }
  }

  /**
   * initiates the circular reference detection with the newly just analyzed bulkIdResourceResolver
   */
  private void checkForCircularReferences(BulkIdResolverAbstract bulkIdResourceResolver)
  {
    circularReferenceDetector.checkForCircles(bulkIdResourceResolver);
  }

  /**
   * @return if there are still some bulkId references that are yet to be resolved
   */
  public boolean isOpenBulkIdReferences()
  {
    return bulkIdResourceResolverMap.values().stream().anyMatch(BulkIdResolverAbstract::hasAnyBulkIdReferences);
  }

  /**
   * checks if the given bulkId was already resolved. If this happens two operations from the bulk-request share
   * the same bulkId
   */
  public boolean isDuplicateBulkId(String bulkId)
  {
    return resolvedBulkIds.containsKey(bulkId);
  }
}
