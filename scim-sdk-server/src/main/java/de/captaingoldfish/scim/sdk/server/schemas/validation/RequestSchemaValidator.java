package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.extern.slf4j.Slf4j;


/**
 * validates a request document against the schema of the current {@link ResourceType}
 * 
 * @author Pascal Knueppel
 * @since 24.02.2021
 */
@Slf4j
public class RequestSchemaValidator extends AbstractSchemaValidator
{

  /**
   * the current request type which is either one of [POST, PUT or PATCH]. The validation must be handled
   * differently in case of POST requests if an attribute is required and has a mutability of writeOnly or
   * immutable
   */
  private HttpMethod httpMethod;

  public RequestSchemaValidator(ResourceType resourceType, HttpMethod httpMethod)
  {
    super(resourceType);
    this.httpMethod = httpMethod;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getHttpStatusCode()
  {
    return HttpStatus.BAD_REQUEST;
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
