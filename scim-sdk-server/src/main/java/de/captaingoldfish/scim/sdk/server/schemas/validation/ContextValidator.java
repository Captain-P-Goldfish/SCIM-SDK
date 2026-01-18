package de.captaingoldfish.scim.sdk.server.schemas.validation;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 11.04.2021
 */
@RequiredArgsConstructor
abstract class ContextValidator
{

  private final ServiceProvider serviceProvider;

  private final ValidationContextType validationContextType;

  /**
   * used to define the validation of an attribute in a specific context. The expect context implementations
   * should be [REQUEST, RESPONSE, META_VALIDATION] @param schemaAttribute the attributes definition @param
   * jsonNode the attribute to validate @return true if the validation was successful, false if the attribute is
   * ignorable and should not be validated. This might be in a request context due to the readOnly mutability
   * modifier.
   *
   * @throws AttributeValidationException if the attribute does not match its definition
   */
  public abstract boolean validateContext(SchemaAttribute schemaAttribute, JsonNode jsonNode)
    throws AttributeValidationException;

  public JsonNode handleDefaultValue(SchemaAttribute schemaAttribute, JsonNode jsonNode)
  {
    boolean handleOnRequest = ValidationContextType.REQUEST.equals(validationContextType)
                              && serviceProvider.isUseDefaultValuesOnRequest();
    boolean handleOnResponse = ValidationContextType.RESPONSE.equals(validationContextType)
                               && serviceProvider.isUseDefaultValuesOnResponse();
    if (handleOnRequest || handleOnResponse)
    {
      return DefaultValueHandler.getOrGetDefault(schemaAttribute, jsonNode);
    }
    return jsonNode;
  }


  public enum ValidationContextType
  {
    REQUEST, RESPONSE
  }
}
