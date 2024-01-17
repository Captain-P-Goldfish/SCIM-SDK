package de.captaingoldfish.scim.sdk.server.endpoints.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 07.04.2021
 */
@Slf4j
public class ValidationContext
{

  /**
   * contains errors that are not bound to any specific fields
   */
  @Getter
  private final List<String> errors;

  /**
   * contains all error messages that are bound to a specific field
   */
  @Getter
  private final Map<String, List<String>> fieldErrors;

  /**
   * the endpoint definition of the resource to validate
   */
  @Getter
  private final ResourceType resourceType;

  /**
   * additional headers that may be returned in case of validation error
   */
  @Getter
  private final Map<String, String> responseHttpHeaders;

  /**
   * the response status that should be returned to the client. Is bad request (400) by default
   */
  @Getter
  @Setter
  private int httpResponseStatus;

  public ValidationContext(ResourceType resourceType)
  {
    this.errors = new ArrayList<>();
    this.fieldErrors = new HashMap<>();
    this.resourceType = resourceType;
    this.httpResponseStatus = HttpStatus.BAD_REQUEST;
    this.responseHttpHeaders = new HashMap<>();
  }

  /**
   * @return true if any errors have been set, false else
   */
  public boolean hasErrors()
  {
    boolean hasError;
    hasError = !errors.isEmpty() || !fieldErrors.isEmpty();
    return hasError;
  }

  /**
   * logs all reported errors on debug level
   */
  public void logErrors()
  {
    errors.forEach(log::debug);
    fieldErrors.forEach((fieldName, errorList) -> {
      errorList.forEach(errorMessage -> log.debug("{}: {}", fieldName, errorMessage));
    });
  }

  /**
   * adds an error that is not bound to a specific resource field
   *
   * @param errorMessage the error message
   */
  public void addError(String errorMessage)
  {
    if (StringUtils.isNotBlank(errorMessage))
    {
      errors.add(errorMessage);
    }
    else
    {
      log.trace("Not adding empty messages to error context.");
    }
  }

  /**
   * adds an error that is bound to a specific resource field
   *
   * @param fieldName the name of the field to which the error is bound
   * @param errorMessage the error message
   */
  public void addError(String fieldName, String errorMessage)
  {
    Optional<SchemaAttribute> schemaAttributeList = resourceType.getAllSchemas()
                                                                .stream()
                                                                .map(schema -> schema.getSchemaAttribute(fieldName))
                                                                .filter(Objects::nonNull)
                                                                .findAny();
    boolean attributeNotFound = !schemaAttributeList.isPresent();
    if (attributeNotFound)
    {
      String error = String.format("Cannot bind field with name '%s' on error constraint because no such field "
                                   + "exists for resource '%s'",
                                   fieldName,
                                   resourceType.getMainSchema().getNonNullId());
      throw new InternalServerException(error);
    }
    List<String> fieldErrorList = fieldErrors.computeIfAbsent(fieldName, getOrCreateList -> new ArrayList<>());
    fieldErrorList.add(errorMessage);
  }

  /**
   * adds specific field errors to the validation context
   *
   * @param ex the definition of the error that occurred
   */
  public void addExceptionMessages(AttributeValidationException ex)
  {
    String fieldName;
    Throwable cause = ex;
    while (cause != null)
    {
      if (cause instanceof AttributeValidationException)
      {
        AttributeValidationException e = (AttributeValidationException)cause;
        fieldName = e.getSchemaAttribute().getScimNodeName();
      }
      else
      {
        fieldName = ex.getSchemaAttribute().getScimNodeName();
      }
      addError(fieldName, cause.getMessage());
      cause = cause.getCause();
    }
    log.debug(ex.getMessage(), ex);
  }

  /**
   * adds other more unspecific error messages to the context that are not directly related to any fields
   *
   * @param ex the definition of the error that occurred
   */
  public void addExceptionMessages(ScimException ex)
  {
    Throwable cause = ex;
    while (cause != null)
    {
      addError(cause.getMessage());
      cause = cause.getCause();
    }
  }

  /**
   * adds the current errors of this validation context to the given error response
   */
  public void writeToErrorResponse(ErrorResponse errorResponse)
  {
    Optional<ArrayNode> errorMessagesArray = addUnspecificErrorMessages();
    Optional<ObjectNode> fieldErrorsObject = addFieldSpecificErrorMessages();

    if (errorMessagesArray.isPresent())
    {
      errorResponse.setDetail(errorMessagesArray.get().get(0).textValue());
    }
    else
    {
      String firstErrorMessage = fieldErrors.get(fieldErrors.keySet().iterator().next()).get(0);
      errorResponse.setDetail(firstErrorMessage);
    }

    ObjectNode errorNode = new ObjectNode(JsonNodeFactory.instance);
    errorMessagesArray.ifPresent(array -> errorNode.set(AttributeNames.Custom.ERROR_MESSAGES, array));
    fieldErrorsObject.ifPresent(object -> errorNode.set(AttributeNames.Custom.FIELD_ERRORS, object));
    errorResponse.set(AttributeNames.Custom.ERRORS, errorNode);
    errorResponse.setStatus(httpResponseStatus);
    responseHttpHeaders.forEach((headerKey, headerValue) -> {
      errorResponse.getHttpHeaders().put(headerKey, headerValue);
    });
  }

  /**
   * if unspecific errors are present an array node will be created with the error messages
   */
  private Optional<ArrayNode> addUnspecificErrorMessages()
  {
    if (errors.isEmpty())
    {
      return Optional.empty();
    }
    ArrayNode errorMessages = new ArrayNode(JsonNodeFactory.instance);
    errors.forEach(errorMessages::add);
    return Optional.of(errorMessages);
  }

  /**
   * if field errors are present an object node will be created and the field errors will be added into the
   * specific object node
   *
   * @return an empty if no field errors are present or an object node that represents the field errors
   */
  private Optional<ObjectNode> addFieldSpecificErrorMessages()
  {
    if (fieldErrors.isEmpty())
    {
      return Optional.empty();
    }
    ObjectNode fieldErrorNode = new ObjectNode(JsonNodeFactory.instance);
    fieldErrors.forEach((fieldName, errorMessageList) -> {
      ArrayNode errorMessages = new ArrayNode(JsonNodeFactory.instance);
      errorMessageList.forEach(errorMessages::add);
      fieldErrorNode.set(fieldName, errorMessages);
    });
    return Optional.of(fieldErrorNode);
  }
}
