package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.ValidationContext;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.AccessLevel;
import lombok.Getter;


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
  private final HttpMethod httpMethod;

  /**
   * the current validation context for the request. If any error occurs the execution must be aborted before
   * the {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler} implementation is called
   */
  @Getter(AccessLevel.PROTECTED)
  private final ValidationContext validationContext;

  public RequestSchemaValidator(ServiceProvider serviceProvider, Class resourceNodeType, HttpMethod httpMethod)
  {
    this(serviceProvider, resourceNodeType, httpMethod, null);
  }

  public RequestSchemaValidator(ServiceProvider serviceProvider,
                                Class resourceNodeType,
                                HttpMethod httpMethod,
                                ValidationContext validationContext)
  {
    super(serviceProvider, resourceNodeType);
    this.httpMethod = httpMethod;
    this.validationContext = validationContext;
  }

  /**
   * validates the attribute in a request context
   */
  @Override
  protected Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    try
    {
      return RequestAttributeValidator.validateAttribute(getServiceProvider(), schemaAttribute, attribute, httpMethod);
    }
    catch (AttributeValidationException ex)
    {
      if (validationContext == null)
      {
        throw ex;
      }
      validationContext.addExceptionMessages(ex);
      return Optional.empty();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getHttpStatusCode()
  {
    return HttpStatus.BAD_REQUEST;
  }
}
