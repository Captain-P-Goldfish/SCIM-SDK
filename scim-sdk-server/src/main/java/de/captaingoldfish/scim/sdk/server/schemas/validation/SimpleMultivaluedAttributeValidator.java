package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 10.04.2021
 */
@Slf4j
class SimpleMultivaluedAttributeValidator
{

  /**
   * will validate that an array attribute contains only simple values of the specified type in
   * {@code schemaAttribute}
   *
   * @param schemaAttribute the attribute definition of the array
   * @param attribute the attribute to validate
   * @return the validated object that is returned as {@link ScimArrayNode} with scim node representations
   */
  public static ArrayNode parseNodeTypeAndValidate(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    log.trace("Validating simple multivalued attribute '{}'", schemaAttribute.getScimNodeName());
    ArrayNode arrayNode = toArrayNode(attribute).orElseThrow(() -> {
      String errorMessage = String.format("Attribute '%s' is expected to be an array but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          attribute);
      return new AttributeValidationException(schemaAttribute, errorMessage);
    });

    ScimArrayNode scimArrayNode = new ScimArrayNode(schemaAttribute);
    if (arrayNode.size() > 0)
    {
      List<String> uniqueValueList = new ArrayList<>();
      for ( JsonNode jsonNode : arrayNode )
      {
        if (!Uniqueness.NONE.equals(schemaAttribute.getUniqueness()))
        {
          if (uniqueValueList.contains(jsonNode.textValue()))
          {
            String errorMessage = String.format("Array with uniqueness '%s' contains duplicate values '%s'",
                                                schemaAttribute.getUniqueness().getValue(),
                                                arrayNode);
            throw new AttributeValidationException(schemaAttribute, errorMessage);
          }
          uniqueValueList.add(jsonNode.textValue());
        }

        try
        {
          JsonNode parsedSimpleNode = SimpleAttributeValidator.parseNodeTypeAndValidate(schemaAttribute, jsonNode);
          scimArrayNode.add(parsedSimpleNode);
          CustomAttributeValidator.validateSimpleNode(schemaAttribute, parsedSimpleNode);
        }
        catch (AttributeValidationException ex)
        {
          String errorMessage = String.format("Found unsupported value in multivalued attribute '%s'", arrayNode);
          throw new AttributeValidationException(schemaAttribute, errorMessage, ex);
        }
      }
    }
    return scimArrayNode.isEmpty() ? null : scimArrayNode;
  }

  /**
   * parses an incoming attribute to an array. If the attribute is a simple attribute not of type array this
   * method will add this single attribute into an array and return this array
   *
   * @param attribute the attribute that should either be an array or a simple attribute
   * @return an array or an empty if the attribute was neither an array nor a simple attribute
   */
  private static Optional<ArrayNode> toArrayNode(JsonNode attribute)
  {
    if (SimpleAttributeValidator.isSimpleNode(attribute))
    {
      ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
      arrayNode.add(attribute);
      return Optional.of(arrayNode);
    }

    if (attribute.isArray())
    {
      return Optional.of((ArrayNode)attribute);
    }
    return Optional.empty();
  }

}
