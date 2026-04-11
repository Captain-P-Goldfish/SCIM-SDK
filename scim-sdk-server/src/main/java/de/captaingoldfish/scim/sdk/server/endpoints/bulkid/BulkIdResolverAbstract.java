package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Strings;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.utils.UriInfos;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 21.08.2022 - 13:47 <br>
 * <br>
 */
public abstract class BulkIdResolverAbstract<T extends JsonNode>
{


  /**
   * the bulkId that is representing this bulk operation details
   */
  @Getter
  protected final String operationBulkId;

  /**
   * the uri information of the currently accessed resource-type
   */
  @Getter
  protected final UriInfos uriInfos;

  /**
   * the resource object that might contain some bulkId references
   */
  @Getter
  protected final T resource;

  /**
   * contains all referenced bulkIds from this operation. Resolved elements will be removed from this set
   */
  @Getter
  protected final Set<String> referencedBulkIds = new HashSet<>();

  /**
   * contains all bulkId reference of the {@link #resource}. Since it might be possible that a bulkId reference
   * is used to reference the same resource in different places we use this map for linking these entries from
   * bulkId to found entries. Resolved elements will be removed from this map
   */
  protected final Map<String, List<BulkIdReferenceWrapper>> bulkIdReferences;

  /**
   * a bulk operation may be processed several times if bulkIds need to be resolved. In order to find
   * unresolvable bulkIds we need to investigate each operation and must try to resolve the contained bulkIds.
   * If an operation will be processed the second third or fourth time it must have set this boolean set to true
   * otherwise we will assume that this operation contains an unresolvable bulkId.
   */
  @Getter
  private boolean hadSuccessInLastRun;

  public BulkIdResolverAbstract(String operationBulkId, UriInfos uriInfos, T resource)
  {
    this.operationBulkId = operationBulkId;
    this.uriInfos = uriInfos;
    this.resource = resource;
    this.bulkIdReferences = new HashMap<>();
    resolveBulkIdInUri();
  }

  /**
   * this method will retrieve all bulkId references present within the {@link #resource} object
   */
  protected void findAllBulkIdReferences()
  {
    boolean containsBulkIdReference = containsBulkIdReference(resource);
    if (!containsBulkIdReference)
    {
      return;
    }
    List<BulkIdReferenceWrapper> complexBulkIdReferences = getComplexBulkIdNodes();
    List<BulkIdReferenceWrapper> directBulkIdReferences = getDirectBulkIdNodes();

    for ( BulkIdReferenceWrapper complexBulkIdReference : complexBulkIdReferences )
    {
      referencedBulkIds.add(complexBulkIdReference.getBulkId());
      List<BulkIdReferenceWrapper> bulkIdReferenceSet = bulkIdReferences.computeIfAbsent(complexBulkIdReference.getBulkId(),
                                                                                         k -> new ArrayList<>());
      bulkIdReferenceSet.add(complexBulkIdReference);
    }
    for ( BulkIdReferenceWrapper directBulkIdReference : directBulkIdReferences )
    {
      referencedBulkIds.add(directBulkIdReference.getBulkId());
      List<BulkIdReferenceWrapper> bulkIdReferenceSet = bulkIdReferences.computeIfAbsent(directBulkIdReference.getBulkId(),
                                                                                         k -> new ArrayList<>());
      bulkIdReferenceSet.add(directBulkIdReference);
    }
  }

  /**
   * will check the current context uri of a bulk operation and will add a resolver to the bulkIdReferenceSet if
   * a bulkId-reference was found within the uri
   *
   * @param uriInfos the uriInfos of the bulk operation
   */
  private void resolveBulkIdInUri()
  {
    String possibleBulkIdReference = uriInfos.getResourceId();
    if (possibleBulkIdReference != null
        && possibleBulkIdReference.matches(String.format("%s:[\\w\\d[^/]]*$", AttributeNames.RFC7643.BULK_ID)))
    {
      checkForBulkIdReferenceValidity(possibleBulkIdReference);
      String bulkId = possibleBulkIdReference.split(":")[1];
      referencedBulkIds.add(bulkId);
      List<BulkIdReferenceWrapper> bulkIdReferenceSet = bulkIdReferences.computeIfAbsent(bulkId,
                                                                                         k -> new ArrayList<>());
      bulkIdReferenceSet.add(new BulkIdReferenceUriWrapper(uriInfos, bulkId));
    }
  }

  /**
   * this method must return the bulkId references from the resource that are based on the complex-node bulkId
   * specification
   *
   * @return the bulkId references that were found within the resource
   */
  protected abstract List<BulkIdReferenceWrapper> getComplexBulkIdNodes();

  /**
   * this method must return the bulkId references from the resource that are based on the simple-node bulkId
   * specification
   *
   * @return the bulkId references that were found within the resource
   */
  protected abstract List<BulkIdReferenceWrapper> getDirectBulkIdNodes();

  /**
   * will replace the values marked with the given bulkId by the given value
   *
   * @param bulkId the bulkId that should be replaced
   * @param value the value that should replace the bulkId reference
   */
  public final void replaceBulkIdNode(String bulkId, String value)
  {
    List<BulkIdReferenceWrapper> bulkIdResourceReferenceWrappers = bulkIdReferences.get(bulkId);
    if (bulkIdResourceReferenceWrappers == null)
    {
      hadSuccessInLastRun = false;
      return;
    }

    // if at least one element is processed we know that one bulkId was resolved, so we got a success in this run
    hadSuccessInLastRun = bulkIdResourceReferenceWrappers.size() > 0;
    bulkIdResourceReferenceWrappers.forEach(reference -> reference.replaceValueNode(value));
    bulkIdReferences.remove(bulkId);
    referencedBulkIds.remove(bulkId);
  }

  /**
   * @return if any bulkId references have been found
   */
  public final boolean hasAnyBulkIdReferences()
  {
    return !bulkIdReferences.isEmpty();
  }

  /**
   * @return the bulkIds that have not been resolved yet
   */
  public final Set<String> getUnresolvedBulkIds()
  {
    return bulkIdReferences.keySet();
  }

  /**
   * checks if this operation contains a self-reference
   */
  public final boolean hasSelfReference()
  {
    return bulkIdReferences.containsKey(operationBulkId);
  }

  /**
   * verifies if the given node does even contain a bulkId
   */
  protected final boolean containsBulkIdReference(JsonNode jsonNode)
  {
    return Strings.CS.contains(jsonNode.toString(), String.format("\"%s:", AttributeNames.RFC7643.BULK_ID));
  }

  protected void checkForBulkIdReferenceValidity(String bulkIdReferenceValue)
  {
    int bulkIdReferencePartsSize = bulkIdReferenceValue.split(":").length;
    if (bulkIdReferencePartsSize != 2)
    {
      throw new BadRequestException(String.format("the value '%s' is not a valid bulkId reference",
                                                  bulkIdReferenceValue),
                                    null, ScimType.RFC7644.INVALID_VALUE);
    }
  }

  public <R extends ScimObjectNode> R getResource(Class<R> type)
  {
    return JsonHelper.copyResourceToObject(resource, type);
  }
}
