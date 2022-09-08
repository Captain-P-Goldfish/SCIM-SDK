package de.captaingoldfish.scim.sdk.server.endpoints.bulkget;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 30.08.2022
 */
@Getter
class BulkResourceReferenceComplex implements ResourceReference
{

  /**
   * the direct id of the resource that is being referenced
   */
  private final String resourceId;

  /**
   * the resource type name that will let us know which resource is referenced
   */
  private final ResourceType resourceType;

  /**
   * the node path from the root of the parent
   */
  private final String nodePath;

  public BulkResourceReferenceComplex(ResourceTypeFactory resourceTypeFactory, String nodePath, JsonNode complexNode)
  {
    this.nodePath = nodePath;
    JsonNode valueNode = complexNode.get(AttributeNames.RFC7643.VALUE);
    this.resourceId = Optional.ofNullable(valueNode).map(JsonNode::textValue).orElseGet(() -> {
      String[] urlParts = getUrlPartsFromRef(complexNode).orElse(null);
      if (urlParts == null)
      {
        return null;
      }
      // a resource-reference-id should be at position length-1
      // e.g. https://example.com/v2/Groups/71ddacd2-a8e7-49b8-a5db-ae50d0a5bfd7
      return urlParts[urlParts.length - 1];
    });
    JsonNode typeNode = complexNode.get(AttributeNames.RFC7643.TYPE);
    this.resourceType = Optional.ofNullable(typeNode)
                                .map(JsonNode::textValue)
                                .flatMap(resourceTypeFactory::getResourceTypeByName)
                                .orElseGet(() -> getResourceTypeFromRefValue(resourceTypeFactory,
                                                                             complexNode).orElse(null));
  }

  /**
   * @return true if a resource type could be determined and an id is present
   */
  @Override
  public boolean isResourceRetrievable()
  {
    return resourceType != null && StringUtils.isNotBlank(resourceId);
  }

  /**
   * tries to retrieve the name of the resource that is referenced by analyzing the $ref attribute
   *
   * @param complexNode a complex node that is expected to have a "value", a "$ref" and a "type" attribute
   * @return the resource type if resolvable
   */
  private Optional<ResourceType> getResourceTypeFromRefValue(ResourceTypeFactory resourceTypeFactory,
                                                             JsonNode complexNode)
  {
    String[] urlParts = getUrlPartsFromRef(complexNode).orElse(null);
    if (urlParts == null)
    {
      return Optional.empty();
    }
    // a ref attribute references the resource with the id so the resourceTypeName should be at position length-2
    // e.g. https://example.com/v2/Groups/71ddacd2-a8e7-49b8-a5db-ae50d0a5bfd7
    String resourceEndpoint = String.format("/%s", urlParts[urlParts.length - 2]);
    ResourceType resourceType = resourceTypeFactory.getResourceType(resourceEndpoint);
    return Optional.ofNullable(resourceType);
  }

  /**
   * splits the value within the $ref node by "/" and returns the different parts as array
   */
  private Optional<String[]> getUrlPartsFromRef(JsonNode complexNode)
  {
    JsonNode refNode = complexNode.get(AttributeNames.RFC7643.REF);
    String url = Optional.ofNullable(refNode).map(JsonNode::textValue).orElse(null);
    if (StringUtils.isBlank(url) || !isUrl(url))
    {
      return Optional.empty();
    }
    return Optional.of(url.split("/"));
  }

  /**
   * checks if the given string is a url or not
   */
  private boolean isUrl(String url)
  {
    try
    {
      new URL(url);
      return true;
    }
    catch (MalformedURLException e)
    {
      return false;
    }
  }
}
