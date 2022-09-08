package de.captaingoldfish.scim.sdk.server.endpoints.bulkget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import lombok.Builder;


/**
 * @author Pascal Knueppel
 * @since 30.08.2022
 */
class ResourceReferenceExtractor
{

  /**
   * this operation contains the parent whose children shall be extracted
   */
  private final ObjectNode resource;

  /**
   * the resources definition
   */
  private final ResourceType resourceType;

  /**
   * the factory is necessary to get the resource definitions of the transitive types to be able to analyze them
   * correctly
   */
  private final ResourceTypeFactory resourceTypeFactory;


  @Builder
  public ResourceReferenceExtractor(ObjectNode resource,
                                    ResourceType resourceType,
                                    ResourceTypeFactory resourceTypeFactory)
  {
    this.resource = resource;
    this.resourceType = resourceType;
    this.resourceTypeFactory = resourceTypeFactory;
  }

  /**
   * retrieves all child-references from the given {@link #resource}
   */
  public List<ResourceReference> getResourceReferences()
  {
    List<ResourceReference> resourceReferences = extractSimpleResourceReferences();
    resourceReferences.addAll(extractComplexResourceReferences());
    return resourceReferences;
  }

  /**
   * retrieves all child-references from the given {@link #resource} that are set with simple resource reference
   * fields
   */
  private List<ResourceReference> extractSimpleResourceReferences()
  {
    return Stream.concat(resourceType.getMainSchema()
                                     .getSimpleBulkIdCandidates()
                                     .stream()
                                     .flatMap(schemaAttribute -> toSimpleResourceReference(schemaAttribute,
                                                                                           false).stream()),
                         resourceType.getAllSchemaExtensions()
                                     .stream()
                                     .flatMap(schema -> schema.getSimpleBulkIdCandidates().stream())
                                     .flatMap(schemaAttribute -> toSimpleResourceReference(schemaAttribute,
                                                                                           true).stream()))
                 .collect(Collectors.toList());
  }

  /**
   * retrieves all child-references from the given {@link #resource} that are set with complex resource
   * reference fields
   */
  private List<ResourceReference> extractComplexResourceReferences()
  {
    return Stream.concat(resourceType.getMainSchema()
                                     .getComplexBulkIdCandidates()
                                     .stream()
                                     .flatMap(schemaAttribute -> toComplexResourceReference(schemaAttribute,
                                                                                            false).stream()),
                         resourceType.getAllSchemaExtensions()
                                     .stream()
                                     .flatMap(schema -> schema.getComplexBulkIdCandidates().stream())
                                     .flatMap(schemaAttribute -> toComplexResourceReference(schemaAttribute,
                                                                                            true).stream()))
                 .filter(Objects::nonNull)
                 .collect(Collectors.toList());
  }

  /**
   * retrieves complex resource references from either an extension or the main resource
   *
   * @param schemaAttribute the attributes definition
   * @param isExtensionAttribute if the attribute is from an extension or from the main schema
   * @return the retrieved child resource references
   */
  private List<ResourceReference> toComplexResourceReference(SchemaAttribute schemaAttribute,
                                                             boolean isExtensionAttribute)
  {
    if (isExtensionAttribute)
    {
      return getComplexAttributeReferenceFromExtension(schemaAttribute);
    }
    return getComplexAttributeReferenceFromResource(resource, schemaAttribute);
  }

  /**
   * tries to retrieve the given resource reference attribute from an extension within the resource
   *
   * @param schemaAttribute the resource reference attribute definition
   * @return the resource reference if it does exist or an empty if the extension or the attribute is not
   *         present
   */
  private List<ResourceReference> getComplexAttributeReferenceFromExtension(SchemaAttribute schemaAttribute)
  {
    String extensionUri = schemaAttribute.getSchema().getNonNullId();
    JsonNode extension = resource.get(extensionUri);
    if (extension == null)
    {
      return Collections.emptyList();
    }
    return getComplexAttributeReferenceFromResource((ObjectNode)extension, schemaAttribute);
  }

  /**
   * retrieves the child references from a complex node that might either be a single complex node or a
   * multivalued complex node
   *
   * @param objectNode the resource that should be either the main resource or an extension of the main resource
   * @param schemaAttribute the current attributes definition
   * @return the retrieved child references of the {@code schemaAttribute}
   */
  private List<ResourceReference> getComplexAttributeReferenceFromResource(ObjectNode objectNode,
                                                                           SchemaAttribute schemaAttribute)
  {
    JsonNode resource = objectNode.get(schemaAttribute.getName());
    if (resource == null)
    {
      return Collections.emptyList();
    }

    if (schemaAttribute.isMultiValued() && resource.isArray())
    {
      List<ResourceReference> resourceReferences = new ArrayList<>();
      for ( JsonNode multivaluedComplexNode : resource )
      {
        toResourceReference(schemaAttribute.getScimNodeName(),
                            (ObjectNode)multivaluedComplexNode).ifPresent(resourceReferences::add);
      }
      return resourceReferences;
    }
    else
    {
      return toResourceReference(schemaAttribute.getScimNodeName(),
                                 (ObjectNode)resource).map(Collections::singletonList).orElse(Collections.emptyList());
    }
  }

  /**
   * tries to resolve the complex type into a resolvable resource reference
   *
   * @param nodeName the name of the referenced node
   * @param complexNode the complex resource reference node to resolve
   * @return the resource reference if the resource type was resolvable or an empty of no resource type was
   *         found that matches the referenced data
   */
  private Optional<ResourceReference> toResourceReference(String nodeName, ObjectNode complexNode)
  {
    ResourceReference resourceReference = new BulkResourceReferenceComplex(resourceTypeFactory, nodeName, complexNode);
    if (resourceReference.getResourceType() == null)
    {
      // this case happens if the complex type does not contain a resolvable $ref or type attribute
      return Optional.empty();
    }
    return Optional.of(resourceReference);
  }

  /**
   * retrieves simple resource references from either an extension or the main resource
   *
   * @param schemaAttribute the attributes definition
   * @param isExtensionAttribute if the attribute is from an extension or from the main schema
   * @return the retrieved child resource references
   */
  private List<ResourceReference> toSimpleResourceReference(SchemaAttribute schemaAttribute,
                                                            boolean isExtensionAttribute)
  {
    if (isExtensionAttribute)
    {
      return getSimpleAttributeReferenceFromExtension(schemaAttribute);
    }
    return getSimpleAttributeReferenceFromResource(resource, schemaAttribute);
  }

  /**
   * retrieves simple resource references from an extension of the main resource
   *
   * @param schemaAttribute the current attributes definition
   * @return the list of found resource references
   */
  private List<ResourceReference> getSimpleAttributeReferenceFromExtension(SchemaAttribute schemaAttribute)
  {
    String extensionUri = schemaAttribute.getSchema().getNonNullId();
    JsonNode extension = resource.get(extensionUri);
    if (extension == null)
    {
      return Collections.emptyList();
    }
    return getSimpleAttributeReferenceFromResource((ObjectNode)extension, schemaAttribute);
  }

  /**
   * gets the resource references from an extension or the main resource
   *
   * @param objectNode an object node that is either the main resource or one of its extensions
   * @param schemaAttribute the current attributes definition
   * @return the found resource references
   */
  private List<ResourceReference> getSimpleAttributeReferenceFromResource(ObjectNode objectNode,
                                                                          SchemaAttribute schemaAttribute)
  {
    boolean hasParent = schemaAttribute.getParent() != null;

    final JsonNode resource;
    if (hasParent)
    {
      resource = objectNode.get(schemaAttribute.getParent().getName());
      if (schemaAttribute.getParent().isMultiValued() && resource != null && resource.isArray())
      {
        return getArrayNodeElementReferences((ArrayNode)resource, schemaAttribute);
      }
    }
    else
    {
      resource = objectNode;
    }

    if (resource == null)
    {
      return Collections.emptyList();
    }

    JsonNode attribute = resource.get(schemaAttribute.getName());
    if (attribute == null)
    {
      return Collections.emptyList();
    }

    if (schemaAttribute.isMultiValued() && attribute.isArray())
    {
      return getArrayNodeElementReferences((ArrayNode)attribute, schemaAttribute);
    }

    return Collections.singletonList(new BulkResourceReferenceSimple(resourceTypeFactory, resourceType,
                                                                     schemaAttribute.getScimNodeName(), attribute));
  }

  /**
   * retrieves the resource references from an array which elements are either complex nodes or simple nodes
   *
   * @param resource the array node that contains either complex or simple nodes
   * @param schemaAttribute the current attributes definition
   * @return the found resource references within the array
   */
  private List<ResourceReference> getArrayNodeElementReferences(ArrayNode resource, SchemaAttribute schemaAttribute)
  {
    List<ResourceReference> resourceReferences = new ArrayList<>();
    for ( JsonNode arrayElement : resource )
    {
      if (arrayElement.isObject()) // multivalued complex type
      {
        JsonNode attribute = arrayElement.get(schemaAttribute.getName());
        if (schemaAttribute.isMultiValued() && attribute.isArray())
        {
          resourceReferences.addAll(getArrayNodeElementReferences((ArrayNode)attribute, schemaAttribute));
        }
        else
        {
          resourceReferences.add(new BulkResourceReferenceSimple(resourceTypeFactory, resourceType,
                                                                 schemaAttribute.getScimNodeName(), attribute));
        }
      }
      else // simple multivalued type
      {
        resourceReferences.add(new BulkResourceReferenceSimple(resourceTypeFactory, resourceType,
                                                               schemaAttribute.getScimNodeName(), arrayElement));
      }
    }
    return resourceReferences;
  }
}
