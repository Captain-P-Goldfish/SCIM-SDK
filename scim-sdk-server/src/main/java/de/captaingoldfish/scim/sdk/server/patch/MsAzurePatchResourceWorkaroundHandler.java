package de.captaingoldfish.scim.sdk.server.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.RequiredArgsConstructor;


/**
 * This is a workaround handler that shall handle invalid scim patch requests that are built by ms azure
 *
 * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/193
 * @author Pascal Knueppel
 * @since 24.09.2021
 */
@RequiredArgsConstructor
public class MsAzurePatchResourceWorkaroundHandler
{

  /**
   * the base resource type for which the workaround must be executed
   */
  private final ResourceType resourceType;


  /**
   * will rebuild invalid patch-requests from ms azure and will build a valid object representation out of them
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/193
   */
  public JsonNode rebuildResource(ResourceType.SchemaExtension extensionReference, String key, JsonNode value)
  {
    Schema extensionSchema = resourceType.getSchemaByUri(extensionReference.getSchema());
    SchemaAttribute schemaAttribute = extensionSchema.getSchemaAttribute(key);
    if (schemaAttribute == null)
    {
      throw new BadRequestException(String.format("Attribute '%s' is unknown to resource type '%s'",
                                                  key,
                                                  resourceType.getName()));
    }

    final boolean isComplex = Type.COMPLEX.equals(schemaAttribute.getType());
    final boolean isMultivalued = schemaAttribute.isMultiValued();
    if (isMultivalued)
    {
      throw new BadRequestException(String.format("Unsupported patch operation with key-reference: %s", key));
    }
    else
    {
      if (isComplex)
      {
        return rebuildObjectNode(schemaAttribute, value);
      }
      else
      {
        return rebuildSimpleNode(schemaAttribute, key, value);
      }
    }
  }

  private JsonNode rebuildSimpleNode(SchemaAttribute schemaAttribute, String key, JsonNode value)
  {
    final boolean isSubvalueOfComplex = schemaAttribute.getParent() != null
                                        && Type.COMPLEX.equals(schemaAttribute.getParent().getType())
                                        && !schemaAttribute.isMultiValued();
    if (isSubvalueOfComplex)
    {
      return resolveSimpleComplexSubAttribute(schemaAttribute, value);
    }
    if (value.isArray() || value.isObject())
    {
      throw new BadRequestException(String.format("Invalid value '%s' found for attribute '%s'", value, key));
    }
    ObjectNode extensionNode = new ObjectNode(JsonNodeFactory.instance);
    extensionNode.set(schemaAttribute.getName(), value);
    return extensionNode;
  }

  private JsonNode resolveSimpleComplexSubAttribute(SchemaAttribute schemaAttribute, JsonNode value)
  {
    ObjectNode extensionNode = new ObjectNode(JsonNodeFactory.instance);
    ObjectNode complexNode = new ObjectNode(JsonNodeFactory.instance);
    extensionNode.set(schemaAttribute.getParent().getName(), complexNode);
    complexNode.set(schemaAttribute.getName(), value);
    return extensionNode;
  }

  private JsonNode rebuildObjectNode(SchemaAttribute schemaAttribute, JsonNode value)
  {
    ObjectNode valueNode = new ObjectNode(JsonNodeFactory.instance);
    if (!value.isObject())
    {
      throw new BadRequestException(String.format("Value for attribute '%s' must be an object but was '%s'",
                                                  schemaAttribute.getFullResourceName(),
                                                  value));
    }
    valueNode.set(schemaAttribute.getName(), value);
    return valueNode;
  }

}
