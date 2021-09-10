package de.captaingoldfish.scim.sdk.server.endpoints.validation;

import java.util.function.Consumer;
import java.util.function.Supplier;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.schemas.validation.RequestResourceValidator;


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

  /**
   * the current request context
   */
  private final Context requestContext;

  public RequestValidatorHandler(ResourceHandler resourceHandler,
                                 RequestResourceValidator requestResourceValidator,
                                 Context requestContext)
  {
    this.requestValidator = resourceHandler.getRequestValidator();
    this.validationContext = requestResourceValidator.getValidationContext();
    this.requestContext = requestContext;
  }

  /**
   * execute the validation for creating a resource
   *
   * @param resourceNode the resource to create
   */
  public void validateCreate(ResourceNode resourceNode)
  {
    validateRequest(validator -> validator.validateCreate(resourceNode, validationContext, requestContext));
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
    validateRequest(validator -> validator.validateUpdate(oldResourceSupplier,
                                                          newResource,
                                                          validationContext,
                                                          requestContext));
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
      checkForErrors();
      return;
    }
    validate.accept(requestValidator);
    checkForErrors();
  }

  /**
   * checks whether an error is present within the validation context and throws an exception if errors are
   * present
   */
  private void checkForErrors()
  {
    if (validationContext.hasErrors())
    {
      validationContext.logErrors();
      throw new RequestContextException(validationContext);
    }
  }
}
