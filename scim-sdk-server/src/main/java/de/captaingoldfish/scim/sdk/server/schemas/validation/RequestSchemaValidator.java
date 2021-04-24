package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * @author Pascal Knueppel
 * @since 24.04.2021
 */
public class RequestSchemaValidator extends AbstractSchemaValidator
{

  /**
   * the current request type which is either one of [POST, PUT or PATCH]. The validation must be handled
   * differently in case of POST requests if an attribute is required and has a mutability of writeOnly or
   * immutable
   */
  private HttpMethod httpMethod;

  public RequestSchemaValidator(Class resourceNodeType, HttpMethod httpMethod)
  {
    super(resourceNodeType);
    this.httpMethod = httpMethod;
  }

  /**
   * validates the attribute in a request context
   */
  @Override
  protected Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    return RequestAttributeValidator.validateAttribute(schemaAttribute, attribute, httpMethod);
  }
}
