package de.captaingoldfish.scim.sdk.server.endpoints.validation;

import java.util.function.Consumer;
import java.util.function.Supplier;

import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;


/**
 * Initiates the custom validation of the resources
 * 
 * @author Pascal Knueppel
 * @since 07.04.2021
 */
public class RequestValidatorHandler
{

  /**
   * the validation context that holds the error messages
   */
  private final ValidationContext validationContext;

  /**
   * the validation implementation. Might be null
   */
  private final RequestValidator requestValidator;

  public RequestValidatorHandler(ResourceType resourceType, RequestValidator requestValidator)
  {
    this.requestValidator = requestValidator;
    this.validationContext = new ValidationContext(resourceType);
  }

  /**
   * execute the validation for creating a resource
   * 
   * @param resourceNode the resource to create
   */
  public void validateCreate(ResourceNode resourceNode)
  {
    validateRequest(validator -> validator.validateCreate(resourceNode, validationContext));
  }

  /**
   * execute the validation for getting a resource
   * 
   * @param id the id of the resource to retrieve
   */
  public void validateGet(String id)
  {
    validateRequest(validator -> validator.validateGet(id, validationContext));
  }

  /**
   * execute the validation for updating a resource
   * 
   * @param oldResourceSupplier extracts the old resource representation by calling the SCIM get-endpoint when *
   *          not already done by the ETag implementation
   * @param resourceNode the new resource representation
   */
  public void validateUpdate(Supplier<ResourceNode> oldResourceSupplier, ResourceNode newResource)
  {
    validateRequest(validator -> validator.validateUpdate(oldResourceSupplier, newResource, validationContext));
  }

  /**
   * execute the validation for deleting a resource
   * 
   * @param id the id of the resource to delete
   */
  public void validateDelete(String id)
  {
    validateRequest(validator -> validator.validateDelete(id, validationContext));
  }

  /**
   * a wrapper method for the validation methods. The validation methods will not be called of no validation
   * implementation is present. And the execution will be aborted if the validation returns with any errors
   * 
   * @param validate the validation call
   */
  private void validateRequest(Consumer<RequestValidator> validate)
  {
    if (requestValidator == null)
    {
      return;
    }
    validate.accept(requestValidator);
    if (validationContext.hasErrors())
    {
      validationContext.logErrors();
      // todo bind validation context errors to error response
      throw new BadRequestException("Validation of resource has failed", ScimType.Custom.INVALID_PARAMETERS);
    }
  }
}
