package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.utils.UriInfos;


/**
 * author Pascal Knueppel <br>
 * created at: 21.08.2022 - 09:25 <br>
 * <br>
 * this class is used to find save and replace the bulkId-references of a single resource
 */
class BulkIdResolverResource extends BulkIdResolverAbstract<ObjectNode>
{

  public BulkIdResolverResource(String operationBulkId, UriInfos uriInfos, ObjectNode resource)
  {
    super(operationBulkId, uriInfos, resource);
  }

  /**
   * tries to retrieve all value-node sub-attributes from complex-types that are matching the bulkId reference
   * specification. A complex attribute matching the bulkId specification must look like this:
   *
   * <pre>
   *   {
   *     "name": "member",
   *     "type": "complex"
   *     "multivalued": true | false
   *     "subAttributes": [
   *       {
   *         "name": "value",
   *         "type": "string",
   *         "multivalued": false
   *         ...
   *       },
   *       {
   *         "name": "type",
   *         "type": "string",
   *         "multivalued": false
   *         ...
   *       },
   *       {
   *         "name": "$ref",
   *         "type": "reference",
   *         "referenceTypes": ["resource"]
   *         "multivalued": false
   *         ...
   *       }
   *     ]
   *   }
   * </pre>
   *
   * and it will look like this within a request in order to utilize the bulkId feature
   *
   * <pre>
   *   member: [
   *     {
   *       "value": "bulkId:1",
   *       "type": "User"
   *     }
   *   ]
   * </pre>
   *
   * or
   *
   * <pre>
   *   member: {
   *     "value": "bulkId:1",
   *     "type": "User"
   *   }
   * </pre>
   *
   * @return all present nodes from the resource that look like described above
   */
  @Override
  protected List<BulkIdReferenceWrapper> getComplexBulkIdNodes()
  {
    Schema mainSchema = uriInfos.getResourceType().getMainSchema();
    List<Schema> schemaExtensions = uriInfos.getResourceType().getAllSchemaExtensions();

    Function<SchemaAttribute, SchemaAttribute> toValueAttribute = schemaAttribute -> {
      return schemaAttribute.getSubAttributes()
                            .stream()
                            .filter(a -> a.getName().equals(AttributeNames.RFC7643.VALUE))
                            .findAny()
                            .get(); // the value-attribute is sure to be present within these nodes
    };
    List<SchemaAttribute> mainSchemaBulkIdNodes = mainSchema.getComplexBulkIdCandidates()
                                                            .stream()
                                                            .map(toValueAttribute)
                                                            .collect(Collectors.toList());
    List<SchemaAttribute> extensionBulkIdNodes = schemaExtensions.stream().flatMap((Schema jsonNodes) -> {
      return jsonNodes.getComplexBulkIdCandidates().stream().map(toValueAttribute);
    }).collect(Collectors.toList());

    if (mainSchemaBulkIdNodes.isEmpty() && extensionBulkIdNodes.isEmpty())
    {
      return Collections.emptyList();
    }

    List<BulkIdReferenceWrapper> bulkIdNodes = new ArrayList<>();
    for ( SchemaAttribute bulkIdCandidate : mainSchemaBulkIdNodes )
    {
      List<BulkIdReferenceWrapper> valueNodes = getComplexBulkIdNodes(bulkIdCandidate, false);
      bulkIdNodes.addAll(valueNodes);
    }
    for ( SchemaAttribute bulkIdCandidate : extensionBulkIdNodes )
    {
      List<BulkIdReferenceWrapper> valueNodes = getComplexBulkIdNodes(bulkIdCandidate, true);
      bulkIdNodes.addAll(valueNodes);
    }
    return bulkIdNodes;
  }

  /**
   * retrieves the bulkId references from the resource. For a more detailed explanation see
   * {@link #getComplexBulkIdNodes()}
   *
   * @param valueNodeDefinition the definition of a sub-attribute value-node within a complex-type attribute
   *          that matches the bulkId reference specification
   * @param fromExtension if the given attribute definition is expected to be within an extension or within the
   *          resource itself
   * @return the extracted bulkId references of the resource
   */
  private List<BulkIdReferenceWrapper> getComplexBulkIdNodes(SchemaAttribute valueNodeDefinition, boolean fromExtension)
  {

    // the parent hierarchy is always 1 or 2 elements in size
    List<SchemaAttribute> parentHierarchy = valueNodeDefinition.getParentHierarchy();
    JsonNode parentNode = fromExtension ? resource.get(valueNodeDefinition.getSchema().getNonNullId()) : resource;

    if (parentNode == null)
    {
      return Collections.emptyList();
    }

    SchemaAttribute parentSchemaAttribute = parentHierarchy.get(0);
    parentNode = parentNode.get(parentSchemaAttribute.getName());
    if (parentNode == null)
    {
      return Collections.emptyList();
    }
    else
    {
      if (parentNode.isArray())
      {
        List<BulkIdReferenceWrapper> retrievedAttributes = new ArrayList<>();
        for ( JsonNode jsonNode : parentNode )
        {
          JsonNode childNode = jsonNode.get(valueNodeDefinition.getName());
          boolean isBulkIdReference = isBulkIdReferenceAfterRfc7644(childNode);
          if (isBulkIdReference)
          {
            retrievedAttributes.add(new BulkIdReferenceResourceWrapper(jsonNode, childNode, valueNodeDefinition));
          }
        }
        return retrievedAttributes;
      }
      else
      {
        JsonNode childNode = parentNode.get(valueNodeDefinition.getName());
        boolean isBulkIdReference = isBulkIdReferenceAfterRfc7644(childNode);
        if (isBulkIdReference)
        {
          return Collections.singletonList(new BulkIdReferenceResourceWrapper(parentNode, childNode,
                                                                              valueNodeDefinition));
        }
      }
    }
    return Collections.emptyList();
  }

  /**
   * will gather all present simple nodes that represent a reference to a specific resource-type. The
   * attribute-definition of such a node must look like this:
   *
   * <pre>
   *   {
   *     "name": "userId",
   *     "type": "reference",
   *     "referenceTypes": [
   *       "resource"
   *     ]
   *     "resourceType": "User"
   *   }
   * </pre>
   *
   * the value in the resource document would then look like this:
   *
   * <pre>
   *   {
   *     ...
   *     "userId": "bulkId:1",
   *     ...
   *   }
   * </pre>
   *
   * or
   *
   * <pre>
   *   {
   *     ...
   *     "nested": {
   *       "userId": "bulkId:1",
   *     }
   *     ...
   *   }
   * </pre>
   *
   * @return the bulkId nodes that have a direct reference to another resource and are not bound to a complex
   *         type
   */
  @Override
  protected List<BulkIdReferenceWrapper> getDirectBulkIdNodes()
  {
    Schema mainSchema = uriInfos.getResourceType().getMainSchema();
    List<Schema> schemaExtensions = uriInfos.getResourceType().getAllSchemaExtensions();

    List<SchemaAttribute> mainSchemaBulkIdNodes = mainSchema.getSimpleBulkIdCandidates();
    List<SchemaAttribute> extensionBulkIdNodes = schemaExtensions.stream().flatMap((Schema jsonNodes) -> {
      return jsonNodes.getSimpleBulkIdCandidates().stream();
    }).collect(Collectors.toList());

    List<BulkIdReferenceWrapper> bulkIdNodes = new ArrayList<>();
    for ( SchemaAttribute schemaAttribute : mainSchemaBulkIdNodes )
    {
      List<BulkIdReferenceWrapper> bulkIdWrapper = getSimpleBulkIdWrapperNodes(resource, schemaAttribute);
      bulkIdNodes.addAll(bulkIdWrapper);
    }

    for ( SchemaAttribute schemaAttribute : extensionBulkIdNodes )
    {
      JsonNode extension = resource.get(schemaAttribute.getSchema().getNonNullId());
      if (extension != null)
      {
        List<BulkIdReferenceWrapper> bulkIdWrapper = getSimpleBulkIdWrapperNodes(extension, schemaAttribute);
        bulkIdNodes.addAll(bulkIdWrapper);
      }
    }

    return bulkIdNodes;
  }

  /**
   * tries to extract bulkId references from the resource after the custom setup for bulkIds as explained above
   * at: {@link #getDirectBulkIdNodes()}
   *
   * @param resource the resource from which the bulkId reference should be extracted
   * @param schemaAttribute the definition of the attribute that should be extracted
   * @return the list of bulkId references that were found under the given schema attribute
   */
  private List<BulkIdReferenceWrapper> getSimpleBulkIdWrapperNodes(JsonNode resource, SchemaAttribute schemaAttribute)
  {
    List<BulkIdReferenceWrapper> bulkIdWrapperNodes = new ArrayList<>();

    if (schemaAttribute.getParent() == null)
    {
      List<BulkIdReferenceWrapper> simpleBulkIdReferences = getSimpleBulkIdReferences(resource, schemaAttribute);
      bulkIdWrapperNodes.addAll(simpleBulkIdReferences);
    }
    else
    {
      JsonNode parentAttribute = resource.get(schemaAttribute.getParent().getName());
      if (parentAttribute == null)
      {
        return Collections.emptyList();
      }
      if (parentAttribute.isArray())
      {
        for ( JsonNode childObjectNode : parentAttribute )
        {
          List<BulkIdReferenceWrapper> bulkIdReferenceList = getSimpleBulkIdReferences(childObjectNode,
                                                                                       schemaAttribute);
          bulkIdWrapperNodes.addAll(bulkIdReferenceList);
        }
      }
      else
      {
        List<BulkIdReferenceWrapper> simpleBulkIdReferences = getSimpleBulkIdReferences(parentAttribute,
                                                                                        schemaAttribute);
        bulkIdWrapperNodes.addAll(simpleBulkIdReferences);
      }
    }

    return bulkIdWrapperNodes;
  }

  /**
   * expects the given resource to be an objectNode with only simple children that may also be arrays and tries
   * to extract bulkId references from this node
   *
   * @param resource an object node that contains only simple children
   * @param schemaAttribute the definition of the attribute that should be extracted
   * @return the list of bulkId references that were found under the given schema attribute
   */
  private List<BulkIdReferenceWrapper> getSimpleBulkIdReferences(JsonNode resource, SchemaAttribute schemaAttribute)
  {
    List<BulkIdReferenceWrapper> bulkIdWrapperNodes = new ArrayList<>();
    if (resource == null)
    {
      return Collections.emptyList();
    }
    JsonNode attribute = resource.get(schemaAttribute.getName());
    if (attribute == null)
    {
      return Collections.emptyList();
    }
    if (attribute.isArray())
    {
      for ( int i = 0 ; i < attribute.size() ; i++ )
      {
        JsonNode node = attribute.get(i);
        if (isBulkIdReferenceAfterCustomFeature(node))
        {
          bulkIdWrapperNodes.add(new BulkIdReferenceArrayWrapper((ArrayNode)attribute, i));
        }
      }
    }
    else
    {
      if (isBulkIdReferenceAfterCustomFeature(attribute))
      {
        bulkIdWrapperNodes.add(new BulkIdReferenceResourceWrapper(resource, attribute, schemaAttribute));
      }
    }
    return bulkIdWrapperNodes;
  }

  /**
   * checks if the given json node contains a bulkId reference
   */
  private boolean isBulkIdReferenceAfterRfc7644(JsonNode jsonNode)
  {
    boolean isBulkIdReference = jsonNode != null && jsonNode.isTextual()
                                && jsonNode.textValue()
                                           .startsWith(String.format("%s:", AttributeNames.RFC7643.BULK_ID));
    if (isBulkIdReference)
    {
      checkForBulkIdReferenceValidity(jsonNode.textValue());
    }
    return isBulkIdReference;
  }

  /**
   * checks if the given json node contains a bulkId reference
   */
  private boolean isBulkIdReferenceAfterCustomFeature(JsonNode jsonNode)
  {
    checkForBulkIdReferenceValidity(jsonNode.textValue());
    return jsonNode.textValue().startsWith(String.format("%s:", AttributeNames.RFC7643.BULK_ID));
  }

}
