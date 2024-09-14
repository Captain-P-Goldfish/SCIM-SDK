package de.captaingoldfish.scim.sdk.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * this class is used for partial or complete resource-comparison. This makes only sense if the comparison
 * should be reduced to specific set of attributes. In any other case the
 * {@link de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode#equals(Object)} or
 * {@link JsonHelper#isEqual(JsonNode, JsonNode)} method can be used
 *
 * @author Pascal Knueppel
 * @since 26.02.2024
 */
public class ResourceComparator
{

  /**
   * the main schema of the resource (the ResourceType object is not part of the common-module, so we need to
   * stick to schemas)
   */
  private final Schema mainSchema;

  /**
   * the extensions of the resource (the ResourceType object is not part of the common-module, so we need to
   * stick to schemas)
   */
  private final List<Schema> extensions;

  /**
   * the attributes to exclude or include for comparison
   */
  private final List<SchemaAttribute> attributes;

  /**
   * how to handle the attributes in {@link #attributes}
   */
  private final AttributeHandlingType attributeHandlingType;

  public ResourceComparator(Schema mainSchema,
                            List<Schema> extensions,
                            List<SchemaAttribute> attributes,
                            AttributeHandlingType attributeHandlingType)
  {
    this.mainSchema = Objects.requireNonNull(mainSchema);
    this.extensions = Optional.ofNullable(extensions).orElseGet(Collections::emptyList);
    this.attributes = Optional.ofNullable(attributes).map(ArrayList::new).orElseGet(ArrayList::new);
    this.attributeHandlingType = Optional.ofNullable(attributeHandlingType).orElse(AttributeHandlingType.EXCLUDE);
  }

  public void addAttributes(SchemaAttribute schemaAttribute, SchemaAttribute... schemaAttributes)
  {
    attributes.add(schemaAttribute);
    Optional.ofNullable(schemaAttributes).ifPresent(attributeArray -> {
      for ( SchemaAttribute attribute : attributeArray )
      {
        attributes.add(attribute);
      }
    });
  }

  /**
   * will compare the two resources of equality
   *
   * @return true if the nodes are equal based on the given attributes, false else
   */
  public boolean equals(ObjectNode resource1, ObjectNode resource2)
  {
    if (attributes == null || attributes.isEmpty())
    {
      return JsonHelper.isEqual(resource1, resource2);
    }
    if (resource1 == null && resource2 == null)
    {
      return true;
    }
    else if (resource1 == null)
    {
      return false;
    }
    else if (resource2 == null)
    {
      return false;
    }

    Set<SchemaAttribute> attributesToCheck = AttributeHandlingType.EXCLUDE.equals(attributeHandlingType)
      ? new HashSet<>(mainSchema.getAttributes())
      : attributes.stream()
                  .filter(s -> s.getSchema() == mainSchema)
                  .map(s -> s.getParent() == null ? s : s.getParent())
                  .collect(Collectors.toSet());

    for ( SchemaAttribute mainSchemaAttribute : attributesToCheck )
    {
      JsonNode node1 = resource1.get(mainSchemaAttribute.getName());
      JsonNode node2 = resource2.get(mainSchemaAttribute.getName());
      boolean isEqual = compareNodes(mainSchemaAttribute, node1, node2);
      if (!isEqual)
      {
        return false;
      }
    }
    for ( Schema extension : extensions )
    {
      JsonNode extensionResource1 = resource1.get(extension.getNonNullId());
      if (extensionResource1 == null)
      {
        extensionResource1 = resource1;
      }
      JsonNode extensionResource2 = resource2.get(extension.getNonNullId());
      if (extensionResource2 == null)
      {
        extensionResource2 = resource2;
      }

      attributesToCheck = AttributeHandlingType.EXCLUDE.equals(attributeHandlingType)
        ? new HashSet<>(extension.getAttributes())
        : attributes.stream()
                    .filter(s -> s.getSchema() == extension)
                    .map(s -> s.getParent() == null ? s : s.getParent())
                    .collect(Collectors.toSet());
      for ( SchemaAttribute extensionSchemaAttribute : attributesToCheck )
      {
        JsonNode node1 = extensionResource1.get(extensionSchemaAttribute.getName());
        JsonNode node2 = extensionResource2.get(extensionSchemaAttribute.getName());
        boolean isEqual = compareNodes(extensionSchemaAttribute, node1, node2);
        if (!isEqual)
        {
          return false;
        }
      }
    }

    return true;
  }

  private boolean compareNodes(SchemaAttribute schemaAttribute, JsonNode node1, JsonNode node2)
  {
    boolean ignoreAttribute = ignoreAttribute(schemaAttribute);
    if (ignoreAttribute)
    {
      return true; // comparison successful because this attribute is excluded from comparison
    }

    if (node1 == null && node2 == null)
    {
      return true;
    }
    else if (node1 == null)
    {
      return false;
    }
    else if (node2 == null)
    {
      return false;
    }

    if (schemaAttribute.isMultivaluedComplexAttribute())
    {
      boolean node1IsArray = node1 instanceof ArrayNode;
      boolean node2IsArray = node2 instanceof ArrayNode;
      if (!node1IsArray || !node2IsArray)
      {
        return false;
      }

      return compareArray(schemaAttribute, (ArrayNode)node1, (ArrayNode)node2);
    }
    else if (schemaAttribute.isComplexAttribute())
    {
      boolean node1IsObject = node1 instanceof ObjectNode;
      boolean node2IsObject = node2 instanceof ObjectNode;
      if (!node1IsObject || !node2IsObject)
      {
        return false;
      }

      for ( SchemaAttribute subAttribute : schemaAttribute.getSubAttributes() )
      {
        JsonNode subNode1 = node1.get(subAttribute.getName());
        JsonNode subNode2 = node2.get(subAttribute.getName());
        boolean isEqual = compareNodes(subAttribute, subNode1, subNode2);
        if (!isEqual)
        {
          return false;
        }
      }
      return true;
    }
    else
    {
      return JsonHelper.isEqual(node1, node2);
    }
  }

  private boolean ignoreAttribute(SchemaAttribute schemaAttribute)
  {
    if (attributeHandlingType.equals(AttributeHandlingType.EXCLUDE))
    {
      return attributes.contains(schemaAttribute);
    }
    else
    {
      boolean isAttributePresent = attributes.contains(schemaAttribute)
                                   || (schemaAttribute.isChildOfComplexAttribute()
                                       && attributes.contains(schemaAttribute.getParent()));
      boolean ignoreAttribute = !isAttributePresent;
      if (ignoreAttribute && schemaAttribute.isComplexAttribute())
      {
        if (schemaAttribute.getSubAttributes().stream().anyMatch(attributes::contains))
        {
          return false;
        }
      }
      return ignoreAttribute;
    }
  }

  private boolean compareArray(SchemaAttribute schemaAttribute, ArrayNode node1, ArrayNode node2)
  {
    if (node1.size() != node2.size())
    {
      return false;
    }

    List<ObjectNode> subNodes1 = IntStream.range(0, node1.size())
                                          .mapToObj(index -> new ObjectNode(JsonNodeFactory.instance))
                                          .collect(Collectors.toList());
    List<ObjectNode> subNodes2 = IntStream.range(0, node2.size())
                                          .mapToObj(index -> new ObjectNode(JsonNodeFactory.instance))
                                          .collect(Collectors.toList());

    for ( SchemaAttribute subAttribute : schemaAttribute.getSubAttributes() )
    {
      boolean ignoreAttribute = ignoreAttribute(subAttribute);
      if (ignoreAttribute)
      {
        continue;
      }
      for ( int i = 0 ; i < subNodes1.size() ; i++ )
      {
        ObjectNode objectNode1 = subNodes1.get(i);
        ObjectNode objectNode2 = subNodes2.get(i);

        ObjectNode complexIndexNode1 = (ObjectNode)node1.get(i);
        ObjectNode complexIndexNode2 = (ObjectNode)node2.get(i);

        objectNode1.set(subAttribute.getName(), complexIndexNode1.get(subAttribute.getName()));
        objectNode2.set(subAttribute.getName(), complexIndexNode2.get(subAttribute.getName()));
      }
    }

    for ( int i = subNodes1.size() - 1 ; i >= 0 ; i-- )
    {
      ObjectNode comparisonNode1 = subNodes1.get(i);
      boolean equalElement = false;
      for ( int j = subNodes2.size() - 1 ; j >= 0 ; j-- )
      {
        ObjectNode comparisonNode2 = subNodes2.get(j);
        boolean isEqual = JsonHelper.isEqual(comparisonNode1, comparisonNode2);
        if (isEqual)
        {
          subNodes1.remove(i);
          subNodes2.remove(j);
          equalElement = true;
          break;
        }
      }

      if (!equalElement)
      {
        return false;
      }
    }

    return true;
  }

  public enum AttributeHandlingType
  {
    EXCLUDE, INCLUDE
  }

}
