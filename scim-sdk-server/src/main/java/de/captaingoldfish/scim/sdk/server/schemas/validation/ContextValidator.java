package de.captaingoldfish.scim.sdk.server.schemas.validation;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;


/**
 * @author Pascal Knueppel
 * @since 11.04.2021
 */
@FunctionalInterface
interface ContextValidator
{

  /**
   * used to define the validation of an attribute in a specific context. The expect context implementations
   * should be [REQUEST, RESPONSE, META_VALIDATION] @param schemaAttribute the attributes definition @param
   * jsonNode the attribute to validate @return true if the validation was successful, false if the attribute is
   * ignorable and should not be validated. This might be in a request context due to the readOnly mutability
   * modifier.
   * 
   * @throws AttributeValidationException if the attribute does not match its definition
   */
  public boolean validateContext(SchemaAttribute schemaAttribute, JsonNode jsonNode)
    throws AttributeValidationException;

}
