package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;


/**
 * @author Pascal Knueppel
 * @since 10.04.2021
 */
public class ValidationSelector
{

  /**
   * decides how an attribute must be validated, meaning that it checks if the attribute is a simple attribute,
   * a simple multivalued attribute, a complex attribute or a multivalued complex attribute and lets the
   * attribute be validated by the corresponding implementation
   * 
   * @param schemaAttribute the attributes definition
   * @param attribute the attribute to be validated
   * @param contextValidator the validation context must validate the attribute different by one of the
   *          following contexts: [REQUEST, RESPONSE, META_VALIDATION]
   * @return the json node if validated successfully and an empty if the attribute should be ignored
   * @throws AttributeValidationException if the attribute does not match its definition
   */
  public static Optional<JsonNode> validateNode(SchemaAttribute schemaAttribute,
                                                JsonNode attribute,
                                                ContextValidator contextValidator)
  {
    final boolean isContextValidation = contextValidator.validateContext(schemaAttribute, attribute);
    if (!isContextValidation)
    {
      return Optional.empty();
    }
    boolean isComplexType = Type.COMPLEX.equals(schemaAttribute.getType());
    if (schemaAttribute.isMultiValued())
    {
      if (isComplexType)
      {
        JsonNode validatedAttribute = MultivaluedComplexAttributeValidator.parseNodeType(schemaAttribute,
                                                                                         attribute,
                                                                                         contextValidator);
        return Optional.ofNullable(validatedAttribute);
      }
      else
      {
        JsonNode validatedAttribute = SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, attribute);
        return Optional.ofNullable(validatedAttribute);
      }
    }
    else
    {
      if (isComplexType)
      {
        JsonNode validatedAttribute = ComplexAttributeValidator.parseNodeType(schemaAttribute,
                                                                              attribute,
                                                                              contextValidator);
        return Optional.ofNullable(validatedAttribute);
      }
      else
      {
        JsonNode validatedAttribute = SimpleAttributeValidator.parseNodeType(schemaAttribute, attribute);
        return Optional.of(validatedAttribute);
      }
    }
  }


}
