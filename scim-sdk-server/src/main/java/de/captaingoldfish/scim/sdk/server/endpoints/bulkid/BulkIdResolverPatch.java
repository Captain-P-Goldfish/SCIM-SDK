package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import de.captaingoldfish.scim.sdk.server.utils.UriInfos;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 21.08.2022 - 13:38 <br>
 * <br>
 */
@Slf4j
class BulkIdResolverPatch extends BulkIdResolverAbstract<PatchOpRequest>
{

  public BulkIdResolverPatch(String operationBulkId, UriInfos uriInfos, PatchOpRequest resource)
  {
    super(operationBulkId, uriInfos, resource);
  }

  @Override
  protected List<BulkIdReferenceWrapper> getComplexBulkIdNodes()
  {
    List<BulkIdReferenceWrapper> bulkIdReferenceWrapperList = new ArrayList<>();
    for ( PatchRequestOperation operation : resource.getOperations() )
    {
      boolean isAddOrReplaceOperation = PatchOp.ADD.equals(operation.getOp())
                                        || PatchOp.REPLACE.equals(operation.getOp());
      if (isAddOrReplaceOperation)
      {
        if (operation.getPath().isPresent())
        {
          List<BulkIdReferenceWrapper> referenceWrapperList = getBulkIdComplexNodesWithPath(operation,
                                                                                            uriInfos.getResourceType());
          bulkIdReferenceWrapperList.addAll(referenceWrapperList);
        }
        else
        {
          List<BulkIdReferenceWrapper> referenceWrapperList = getBulkIdComplexNodesFromResource(operation);
          bulkIdReferenceWrapperList.addAll(referenceWrapperList);
        }
      } // do nothing in else case. It is not possible to have bulkId references in remove operations
    }
    return bulkIdReferenceWrapperList;
  }

  @Override
  protected List<BulkIdReferenceWrapper> getDirectBulkIdNodes()
  {
    return Collections.emptyList();
  }

  /**
   * This method will try to extract bulkId references from Patch requests that have a path attribute and will
   * look like this:
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:BulkRequest" ],
   *   "Operations" : [ {
   *     "method" : "PATCH",
   *     "bulkId" : "0289cf0a-98f2-45c1-9db0-a70935d01ffc",
   *     "path" : "/Users/7fc3d8bf-b542-4d0a-b277-c84689105b2b",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *       "Operations" : [ {
   *         "path" : "manager",
   *         "op" : "replace",
   *         "value" : "[{\"value\":\"bulkId:dc508818-1500-47af-8bf3-3c810ce51049\"}]"
   *       } ]
   *     }
   *   }, {
   *     "method" : "POST",
   *     "bulkId" : "dc508818-1500-47af-8bf3-3c810ce51049",
   *     "path" : "/Users",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *       "userName" : "04a084a9-a8b2-4d92-a759-fb78807c118c"
   *     }
   *   } ]
   * }
   * </pre>
   *
   * @param operation the patch request operation to resolve
   * @param resourceType the definition of the resource that is being patched
   * @return the list of bulkId references if any.
   */
  private List<BulkIdReferenceWrapper> getBulkIdComplexNodesWithPath(PatchRequestOperation operation,
                                                                     ResourceType resourceType)
  {
    AttributePathRoot pathRoot = RequestUtils.parsePatchPath(resourceType, operation.getPath().get());
    SchemaAttribute schemaAttribute = pathRoot.getSchemaAttribute();

    boolean isValueAttribute = AttributeNames.RFC7643.VALUE.equals(schemaAttribute.getName());
    // means the patch operation path is e.g. enterpriseUser.manager
    boolean isComplexAttributeBulkCandidate = schemaAttribute.isComplexBulkCandidate();
    // means the patch operation path is e.g. enterpriseUser.manager.value
    boolean isValueAttributeBulkCandidate = isValueAttribute && schemaAttribute.getParent() != null
                                            && schemaAttribute.getParent().isComplexBulkCandidate();

    if (isComplexAttributeBulkCandidate || isValueAttributeBulkCandidate)
    {
      return resolveComplexAttributePathReference(operation);
    }
    else
    {
      return Collections.emptyList();
    }
  }

  /**
   * this method will resolve a bulkId reference within a patch request operation to a complex attribute that
   * looks like this:
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:BulkRequest" ],
   *   "Operations" : [ {
   *     "method" : "PATCH",
   *     "bulkId" : "e1f7ec0a-4f62-4832-af91-7d0d3cc411df",
   *     "path" : "/Users/201e999b-e64b-4ef6-a4d1-b42c7a003f5e",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *       "Operations" : [ {
   *         "path" : "manager",
   *         "op" : "add",
   *         "value" : ["{
   *                      'value': 'bulkId:150b272f-5dd5-4d34-83ff-905bd71046a4'
   *                    }"]
   *       } ]
   *     }
   *   }, {
   *     "method" : "POST",
   *     "bulkId" : "150b272f-5dd5-4d34-83ff-905bd71046a4",
   *     "path" : "/Users",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *       "userName" : "c2a097ae-91c7-427e-9145-a24c7e50c45a"
   *     }
   *   } ]
   * }
   * </pre>
   *
   * @param operation the patch request operation
   * @return a list of all found values if value attribute would contain a multivalued complex type
   *         representation otherwise a list with a single entry will be returned or an empty list if no bulkId
   *         reference was present
   */
  private List<BulkIdReferenceWrapper> resolveComplexAttributePathReference(PatchRequestOperation operation)
  {
    List<BulkIdReferenceWrapper> bulkIdReferenceWrappers = new ArrayList<>();
    List<String> values = operation.getValues();
    for ( int i = 0 ; i < values.size() ; i++ )
    {
      String operationValue = values.get(i);
      boolean containsBulkIdReference = StringUtils.contains(operationValue,
                                                             String.format("%s:", AttributeNames.RFC7643.BULK_ID));
      if (containsBulkIdReference)
      {
        BulkIdReferenceWrapper wrapper = new BulkIdReferencePatchNodeWrapper(operation, operationValue, i);
        checkForBulkIdReferenceValidity(String.format("%s:%s", AttributeNames.RFC7643.BULK_ID, wrapper.getBulkId()));
        bulkIdReferenceWrappers.add(wrapper);
      }
    }
    return bulkIdReferenceWrappers;
  }

  /**
   * will get the bulkIds from the resource itself. If a patch request does not contain a path-attribute, the
   * value node is expected to contain a single element that represents the resource itself e.g. a User
   * representation
   *
   * @param operation the patch request operation
   * @return the list of found bulkId references
   */
  private List<BulkIdReferenceWrapper> getBulkIdComplexNodesFromResource(PatchRequestOperation operation)
  {
    ArrayNode valueNode = operation.getValueNode().orElse(null);
    if (valueNode == null || valueNode.size() != 1)
    {
      if (valueNode != null && valueNode.size() > 1)
      {
        log.debug("Found error in patch request during bulkId reference resolving. "
                  + "Too many value nodes are present in patch-request without a path-attribute. "
                  + "The value node must be represented by the resource itself. @see RFC7644#3.5.2.1 - 3.5.2.3.");
      }
      return Collections.emptyList();
    }
    BulkIdResolverResource bulkIdResolverResource = new BulkIdResolverResource(null, uriInfos,
                                                                               (ObjectNode)valueNode.get(0));
    bulkIdResolverResource.findAllBulkIdReferences();
    List<BulkIdReferenceWrapper> referenceWrapperList = new ArrayList<>();
    referenceWrapperList.addAll(bulkIdResolverResource.getComplexBulkIdNodes());
    referenceWrapperList.addAll(bulkIdResolverResource.getDirectBulkIdNodes());
    return referenceWrapperList;
  }


}
