package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.ValidationContext;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.extern.slf4j.Slf4j;


/**
 * validates a request document against the schema of the current {@link ResourceType}
 *
 * @author Pascal Knueppel
 * @since 24.02.2021
 */
@Slf4j
public class RequestResourceValidator extends AbstractResourceValidator
{

  public RequestResourceValidator(ServiceProvider serviceProvider, ResourceType resourceType, HttpMethod httpMethod)
  {
    super(resourceType, new RequestSchemaValidator(serviceProvider, resourceType.getResourceHandlerImpl().getType(),
                                                   httpMethod, new ValidationContext(resourceType)));
  }

  /**
   * @return the validation context on the current schema validation
   */
  public ValidationContext getValidationContext()
  {
    return Optional.ofNullable(((RequestSchemaValidator)getSchemaValidator()))
                   .map(RequestSchemaValidator::getValidationContext)
                   .orElse(null);
  }

  /**
   * assures that the meta-attribute that is sent by the client is added into the validated document. This meta
   * information might be important to the {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler}
   * implementation
   *
   * @param resource the document that should be validated
   * @return the validated document with the original meta attribute
   */
  @Override
  public ScimObjectNode validateDocument(JsonNode resource)
  {
    if (log.isTraceEnabled())
    {
      log.trace("Validating resource '{}' for resourceType '{}'",
                resource.toPrettyString(),
                getResourceType().getName());
    }
    try
    {
      ScimObjectNode validatedResource = super.validateDocument(resource);
      if (resource.has(AttributeNames.RFC7643.META))
      {
        validatedResource.set(AttributeNames.RFC7643.META, resource.get(AttributeNames.RFC7643.META));
      }
      boolean containsOnlyAttributesSchemasAndMeta = validatedResource.size() == 2
                                                     && validatedResource.has(AttributeNames.RFC7643.SCHEMAS)
                                                     && validatedResource.has(AttributeNames.RFC7643.META);
      boolean isEmpty = validatedResource.isEmpty() || containsOnlyAttributesSchemasAndMeta;
      if (isEmpty)
      {
        String errorMessage = String.format("Request document is invalid it does not contain processable data '%s'",
                                            resource);
        getValidationContext().addError(errorMessage);
      }
      return validatedResource;
    }
    catch (DocumentValidationException ex)
    {
      ValidationContext validationContext = getValidationContext();
      Optional.ofNullable(validationContext).ifPresent(context -> context.addExceptionMessages(ex));
      ResourceNode resourceNode = (ResourceNode)JsonHelper.getNewInstance(getResourceType().getResourceHandlerImpl()
                                                                                           .getType());
      resourceNode.setMeta(Meta.builder().build());
      return resourceNode;
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
