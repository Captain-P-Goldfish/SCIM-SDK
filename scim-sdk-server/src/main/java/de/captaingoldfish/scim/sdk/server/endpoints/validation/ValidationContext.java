package de.captaingoldfish.scim.sdk.server.endpoints.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.Getter;
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
  private final List<String> errors;

  /**
   * contains all error messages that are bound to a specific field
   */
  private final Map<String, List<String>> fieldErrors;

  /**
   * the endpoint definition of the resource to validate
   */
  @Getter
  private final ResourceType resourceType;

  public ValidationContext(ResourceType resourceType)
  {
    this.errors = new ArrayList<>();
    this.fieldErrors = new HashMap<>();
    this.resourceType = resourceType;
  }

  /**
   * @return true if any errors have been set, false else
   */
  protected boolean hasErrors()
  {
    boolean hasError;
    hasError = !errors.isEmpty() || !fieldErrors.isEmpty();
    return hasError;
  }

  /**
   * logs all reported errors on debug level
   */
  protected void logErrors()
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
      log.debug("Not adding empty messages to error context.");
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
    SchemaAttribute schemaAttribute = resourceType.getMainSchema().getSchemaAttribute(fieldName);
    if (schemaAttribute == null)
    {
      String error = String.format("Cannot bind field with name '%s' on error constraint because no such field "
                                   + "exists for resource '%s'",
                                   fieldName,
                                   resourceType.getMainSchema().getNonNullId());
      throw new InternalServerException(error);
    }
    List<String> fieldErrorList = fieldErrors.computeIfAbsent(fieldName, k -> new ArrayList<>());
    fieldErrorList.add(errorMessage);
  }

}
