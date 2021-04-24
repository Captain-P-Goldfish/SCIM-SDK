package de.captaingoldfish.scim.sdk.server.schemas.validation;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
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

  public RequestResourceValidator(ResourceType resourceType, HttpMethod httpMethod)
  {
    super(resourceType, new RequestSchemaValidator(resourceType.getResourceHandlerImpl().getType(), httpMethod));
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
    ScimObjectNode validatedResource = super.validateDocument(resource);
    if (resource.has(AttributeNames.RFC7643.META))
    {
      validatedResource.set(AttributeNames.RFC7643.META, resource.get(AttributeNames.RFC7643.META));
    }
    return validatedResource;
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
