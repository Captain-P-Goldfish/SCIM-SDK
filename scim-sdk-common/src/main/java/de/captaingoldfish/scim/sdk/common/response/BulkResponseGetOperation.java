package de.captaingoldfish.scim.sdk.common.response;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * this class will represent a bulk-get response node that contains the data of a specific resource together
 * with its children. This data structure can be transitively nested and thus become very large if not used
 * with care
 *
 * @author Pascal Knueppel
 * @since 29.08.2022
 */
@NoArgsConstructor
public class BulkResponseGetOperation extends ScimObjectNode
{

  @Builder
  public BulkResponseGetOperation(String nodePath,
                                  String resourceId,
                                  Integer status,
                                  String resourceType,
                                  ScimResponse resource,
                                  List<BulkResponseGetOperation> children)
  {
    setNodePath(nodePath);
    setResourceId(resourceId);
    setStatus(status);
    setResourceType(resourceType);
    setResource(resource);
    setChildren(children);
  }

  /**
   * the id of the resource that is referenced
   */
  public String getResourceId()
  {
    return getStringAttribute(AttributeNames.Custom.RESOURCE_ID).orElse(null);
  }

  /**
   * the id of the resource that is referenced
   */
  public void setResourceId(String resourceId)
  {
    setAttribute(AttributeNames.Custom.RESOURCE_ID, resourceId);
  }

  /**
   * the node path of this resource that references the position of this resource within the parent. If null
   * this resource is the one requested within the bulk-request
   */
  public String getNodePath()
  {
    return getStringAttribute(AttributeNames.Custom.NODE_PATH).orElse(null);
  }

  /**
   * the node path of this resource that references the position of this resource within the parent. If null
   * this resource is the one requested within the bulk-request
   */
  public void setNodePath(String nodePath)
  {
    setAttribute(AttributeNames.Custom.NODE_PATH, nodePath);
  }

  /**
   * the http response status for this resource. It might happen that a transitive resource cannot be retrieved
   * because of missing access rights and in such a case the response object for this resource would contain a
   * 403 status code
   */
  public Integer getStatus()
  {
    return getIntegerAttribute(AttributeNames.RFC7643.STATUS).orElse(0);
  }

  /**
   * the http response status for this resource. It might happen that a transitive resource cannot be retrieved
   * because of missing access rights and in such a case the response object for this resource would contain a
   * 403 status code
   */
  public void setStatus(Integer status)
  {
    setAttribute(AttributeNames.RFC7643.STATUS, status);
  }

  /**
   * the name of the resource type that is referenced with this resource. In case that a group is retrieved and
   * this node represents one of its members it might be either a User or a Group and this field will help
   * determining which type of resource is represented
   */
  public String getResourceType()
  {
    return getStringAttribute(AttributeNames.RFC7643.RESOURCE_TYPE).orElse(null);
  }

  /**
   * the name of the resource type that is referenced with this resource. In case that a group is retrieved and
   * this node represents one of its members it might be either a User or a Group and this field will help
   * determining which type of resource is represented
   */
  public void setResourceType(String resourceType)
  {
    setAttribute(AttributeNames.RFC7643.RESOURCE_TYPE, resourceType);
  }

  /**
   * @return the resource that represents this node
   */
  public <T extends ResourceNode> T getResource(Class<T> type)
  {
    JsonNode resourceNode = get(AttributeNames.Custom.RESOURCE);
    if (resourceNode == null)
    {
      return null;
    }
    if (resourceNode.isTextual())
    {
      return JsonHelper.readJsonDocument(resourceNode.toString(), type);
    }
    else
    {
      return JsonHelper.copyResourceToObject(resourceNode, type);
    }
  }

  /**
   * if the http status code is unequal to 200 this method can be called to get the error response
   */
  public ErrorResponse getErrorResponse()
  {
    JsonNode resourceNode = get(AttributeNames.Custom.RESOURCE);
    if (resourceNode == null)
    {
      return null;
    }
    if (resourceNode.isTextual())
    {
      return JsonHelper.readJsonDocument(resourceNode.toString(), ErrorResponse.class);
    }
    else
    {
      return JsonHelper.copyResourceToObject(resourceNode, ErrorResponse.class);
    }
  }

  /**
   * the resource that represents this node
   */
  public <T extends ScimResponse> void setResource(T resource)
  {
    setAttribute(AttributeNames.Custom.RESOURCE, resource);
  }

  /**
   * the children of this resource if any are present
   */
  public List<BulkResponseGetOperation> getChildren()
  {
    return getArrayAttribute(AttributeNames.Custom.CHILDREN, BulkResponseGetOperation.class);
  }

  /**
   * the children of this resource if any are present
   */
  public void setChildren(List<BulkResponseGetOperation> children)
  {
    setAttribute(AttributeNames.Custom.CHILDREN, children);
  }

  /**
   * override lombok builder with public constructor
   */
  public static class BulkResponseGetOperationBuilder
  {

    public BulkResponseGetOperationBuilder()
    {}
  }
}
