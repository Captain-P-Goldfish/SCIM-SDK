package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    ObjectNode effectiveAttribute = getEffectiveAttributeNode(schemaAttribute, attribute);

    ScimObjectNode scimObjectNode = new ScimObjectNode(schemaAttribute);

    // we are gathering the attribute validation errors in the list and throw them later. This is done due to a
    // very special use-case that will be explained in the following:
    // If we get an object like this: { "manager": { "value": null } }. We need to interpret this object
    // as if it would look like this: { "manager": null }.
    // The following code parts that directly refer to this use-case are marked with "empty-object-use-case"
    List<AttributeValidationException> errorValidations = new ArrayList<>();
    boolean allNodesAreNullNodes = true;

    for ( SchemaAttribute subAttribute : schemaAttribute.getSubAttributes() )
    {
      JsonNode subNode = effectiveAttribute.get(subAttribute.getName());
      if (subNode != null && !NullNode.getInstance().equals(subNode))
      {
        allNodesAreNullNodes = false;
      }
      else
      {
        // empty-object-use-case
        effectiveAttribute.remove(subAttribute.getName());
      }

      Optional<JsonNode> validatedNode;
      try
      {
        validatedNode = ValidationSelector.validateNode(subAttribute, subNode, contextValidator);
      }
      catch (AttributeValidationException ex)
      {
        // empty-object-use-case
        errorValidations.add(ex);
        continue;
      }

      if (validatedNode.isPresent())
      {
        scimObjectNode.set(subAttribute.getName(), validatedNode.get());
      }
    }
    if (scimObjectNode.isEmpty())
    {
      log.trace("Evaluated complex node '{}' to an empty object.", schemaAttribute.getFullResourceName());
    }
    if (allNodesAreNullNodes && !effectiveAttribute.isEmpty())
    {
      // empty-object-use-case
      allNodesAreNullNodes = false;
    }
    if (!errorValidations.isEmpty() && !allNodesAreNullNodes)
    {
      // empty-object-use-case
      throw errorValidations.get(0);
    }
    return scimObjectNode.isEmpty() ? null : scimObjectNode;
  }

  /**
   * retrieves the attribute node from the given attribute-parameter. We require a JSON object but the attribute
   * might be a string-representation or an array-representation, but still these representations can be
   * interpreted as JSON objects if the array contains a single element or the string-representation represents
   * a JSON object
   *
   * @param schemaAttribute the definition of the object
   * @param originalAttribute the attributes representation
   * @return the attribute itself or an underlying object-node that was nested in a one-element array or was
   *         simply a string representation
   */
  private static ObjectNode getEffectiveAttributeNode(SchemaAttribute schemaAttribute, JsonNode originalAttribute)
  {
    JsonNode attribute = originalAttribute;
    if (attribute.isObject())
    {
      return (ObjectNode)attribute;
    }

    if (attribute.isArray() && attribute.size() == 1)
    {
      if (attribute.get(0).isObject())
      {
        return (ObjectNode)attribute.get(0);

      }
      else if (attribute.get(0) instanceof TextNode)
      {
        attribute = JsonHelper.readJsonDocument(attribute.get(0).textValue());
        if (attribute.isObject())
        {
          return (ObjectNode)attribute;
        }
      }
    }
    String errorMessage = String.format("Attribute '%s' must be of type object but is '%s'",
                                        schemaAttribute.getFullResourceName(),
                                        attribute);
    throw new AttributeValidationException(schemaAttribute, errorMessage);
  }
}
