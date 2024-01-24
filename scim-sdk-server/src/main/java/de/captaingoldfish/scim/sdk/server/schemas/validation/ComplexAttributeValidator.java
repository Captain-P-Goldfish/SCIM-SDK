package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 10.04.2021
 */
@Slf4j
public class ComplexAttributeValidator
{

  /**
   * validates a complex attribute by validating the complex attribute itself and all its children by calling
   * the {@link ValidationSelector} with the appropriate {@code contextValidator} for each of these children
   *
   * @param schemaAttribute the complex attributes definition
   * @param attribute the complex attribute
   * @param contextValidator the context validation that must also be executed for any children of the complex
   *          attribute
   * @return the validated attribute or null if the validated attribute has no children
   * @throws AttributeValidationException if the complex attribute or one of its children do not match its
   *           attribute definition
   */
  static JsonNode parseNodeTypeAndValidate(SchemaAttribute schemaAttribute,
                                           JsonNode attribute,
                                           ContextValidator contextValidator)
  {
    log.trace("Validating complex attribute '{}'", schemaAttribute.getScimNodeName());
    JsonNode effectiveAttribute = attribute;
    errorIfBlock: if (!effectiveAttribute.isObject())
    {
      if (effectiveAttribute.isArray() && effectiveAttribute.size() == 1)
      {
        if (effectiveAttribute.get(0).isObject())
        {
          effectiveAttribute = effectiveAttribute.get(0);
          break errorIfBlock;
        }
        else if (effectiveAttribute.get(0) instanceof TextNode)
        {
          effectiveAttribute = JsonHelper.readJsonDocument(effectiveAttribute.get(0).textValue());
          if (effectiveAttribute.isObject())
          {
            break errorIfBlock;
          }
        }
      }
      String errorMessage = String.format("Attribute '%s' must be of type object but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          effectiveAttribute);
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }

    ScimObjectNode scimObjectNode = new ScimObjectNode(schemaAttribute);
    for ( SchemaAttribute subAttribute : schemaAttribute.getSubAttributes() )
    {
      JsonNode subNode = effectiveAttribute.get(subAttribute.getName());
      Optional<JsonNode> validatedNode = ValidationSelector.validateNode(subAttribute, subNode, contextValidator);
      if (validatedNode.isPresent())
      {
        scimObjectNode.set(subAttribute.getName(), validatedNode.get());
      }
    }
    if (scimObjectNode.isEmpty())
    {
      log.trace("Evaluated complex node '{}' to an empty object.", schemaAttribute.getFullResourceName());
    }
    return scimObjectNode.isEmpty() ? null : scimObjectNode;
  }
}
