package de.captaingoldfish.scim.sdk.server.endpoints.bulkget;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 05.09.2022
 */
@Getter
class BulkResourceReferenceSimple implements ResourceReference
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

  public BulkResourceReferenceSimple(ResourceTypeFactory resourceTypeFactory,
                                     ResourceType parentResourceType,
                                     String nodePath,
                                     JsonNode nodeReference)
  {
    this.resourceId = Optional.ofNullable(nodeReference).map(JsonNode::textValue).orElse(null);
    this.nodePath = nodePath;
    this.resourceType = parentResourceType.getAllSchemas()
                                          .stream()
                                          .map(schema -> schema.getSchemaAttribute(nodePath))
                                          .filter(Objects::nonNull)
                                          .findFirst()
                                          .flatMap(SchemaAttribute::getResourceTypeReferenceName)
                                          .flatMap(resourceTypeFactory::getResourceTypeByName)
                                          .orElse(null);
  }

  /**
   * @return true if a resource type could be determined and an id is present
   */
  @Override
  public boolean isResourceRetrievable()
  {
    return resourceType != null && StringUtils.isNotBlank(resourceId);
  }
}
