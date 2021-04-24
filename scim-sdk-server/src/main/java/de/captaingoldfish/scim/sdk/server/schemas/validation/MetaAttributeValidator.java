package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;


/**
 * @author Pascal Knueppel
 * @since 24.04.2021
 */
public class MetaAttributeValidator
{

  /**
   * will validate an attribute in the context of meta-attribute validation. In such a context attributes like
   * "returned", "mutability" etc. will have no effect
   * 
   * @param schemaAttribute the attributes definition
   * @param attribute the attribute to validate
   * @return the validated node or an empty if the node can be ignored
   */
  public static Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    ContextValidator metaContextValidator = getContextValidator();
    Optional<JsonNode> validatedNode = ValidationSelector.validateNode(schemaAttribute,
                                                                       attribute,
                                                                       metaContextValidator);
    // checking once more for required is necessary for complex attributes and multivalued complex attributes
    // that have been evaluated to an empty.
    if (Type.COMPLEX.equals(schemaAttribute.getType()))
    {
      try
      {
        validateRequiredAttribute(schemaAttribute, !validatedNode.isPresent());
      }
      catch (AttributeValidationException ex)
      {
        String errorMessage = String.format("The required attribute '%s' was evaluated to an empty during "
                                            + "schema validation but the attribute is required '%s'",
                                            schemaAttribute.getFullResourceName(),
                                            attribute);
        throw new AttributeValidationException(schemaAttribute, errorMessage, ex);
      }
    }
    return validatedNode;
  }

  /**
   * checks if the given current attribute is required and throws an exception if the the attribute is required
   * and not present
   * 
   * @param schemaAttribute the attributes definition
   * @param isNodeNull if the attribute is null or present
   */
  private static void validateRequiredAttribute(SchemaAttribute schemaAttribute, boolean isNodeNull)
  {
    if (!schemaAttribute.isRequired())
    {
      return;
    }
    if (isNodeNull)
    {
      String errorMessage = String.format("The required attribue '%s' is missing",
                                          schemaAttribute.getFullResourceName());
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }
  }

  /**
   * @return the meta attribute context validator that will only check if the attribute is required or not
   */
  private static ContextValidator getContextValidator()
  {
    return (schemaAttribute, attribute) -> {
      boolean isNodeNull = attribute == null || attribute.isNull();
      validateRequiredAttribute(schemaAttribute, isNodeNull);
      return !isNodeNull;
    };
  }
}
