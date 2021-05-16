package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.server.schemas.DocumentDescription;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 21.04.2021
 */
@Slf4j
abstract class AbstractResourceValidator
{

  /**
   * the resource type that is the representative for the validation that will be executed on the document
   */
  @Getter(AccessLevel.PROTECTED)
  private final ResourceType resourceType;

  /**
   * the schema validator implementation to use
   */
  @Getter(AccessLevel.PROTECTED)
  private final AbstractSchemaValidator schemaValidator;

  public AbstractResourceValidator(ResourceType resourceType, AbstractSchemaValidator schemaValidator)
  {
    this.resourceType = resourceType;
    this.schemaValidator = schemaValidator;
  }

  /**
   * the http status code to use in the {@link DocumentValidationException} if the validation fails. Should be
   * 400 (bad request) for requests and 500 (internal server error) for responses
   */
  protected abstract int getHttpStatusCode();

  /**
   * checks the given document against the schema definition of the {@link #resourceType}
   *
   * @param document the document that should be validated
   * @return the validated resource
   */
  public ScimObjectNode validateDocument(JsonNode document)
  {
    try
    {
      DocumentDescription documentDescription = new DocumentDescription(resourceType, document);
      final Schema documentSchema = documentDescription.getMetaSchema();
      final ResourceNode validatedResource = (ResourceNode)schemaValidator.validateDocument(documentSchema, document);
      validatedResource.addSchema(documentDescription.getMetaSchema().getNonNullId());
      final List<Schema> inResourcePresentExtensions = documentDescription.getExtensions();
      List<ValidatedExtension> validatedExtensions = validateExtensions(resourceType.getRequiredResourceSchemaExtensions(),
                                                                        inResourcePresentExtensions,
                                                                        document);
      for ( ValidatedExtension validatedExtension : validatedExtensions )
      {
        if (!validatedExtension.getValidatedExtension().isEmpty())
        {
          validatedResource.addSchema(validatedExtension.getExtensionSchema().getNonNullId());
          validatedResource.set(validatedExtension.getExtensionSchema().getNonNullId(),
                                validatedExtension.getValidatedExtension());
        }
        else
        {
          validatedResource.removeSchema(validatedExtension.getExtensionSchema().getNonNullId());
        }
      }
      return validatedResource;
    }
    catch (AttributeValidationException ex)
    {
      Throwable cause = ExceptionUtils.getRootCause(ex);
      String errorMessage = Optional.ofNullable(cause).map(Throwable::getMessage).orElse(ex.getMessage());
      throw new DocumentValidationException(errorMessage, ex, getHttpStatusCode(), null);
    }
    catch (DocumentValidationException ex)
    {
      ex.setStatus(getHttpStatusCode());
      throw ex;
    }
  }

  /**
   * validates the extensions that are present within the document that should be validated
   *
   * @param extensions all extensions that are defined within the {@link #resourceType}
   * @param inResourcePresentExtensions all extensions that were found within the documents body
   * @return the list of validated extensions. If an extension evaluated to an empty object it will not be
   *         present within this list
   */
  protected List<ValidatedExtension> validateExtensions(List<Schema> extensions,
                                                        List<Schema> inResourcePresentExtensions,
                                                        JsonNode resource)
  {
    List<ValidatedExtension> validatedExtensionList = new ArrayList<>();
    checkForMissingRequiredExtensions(extensions, inResourcePresentExtensions);
    for ( Schema extensionSchema : inResourcePresentExtensions )
    {
      JsonNode extension = resource.get(extensionSchema.getNonNullId());
      ScimObjectNode validatedExtension = schemaValidator.validateDocument(new ScimObjectNode(),
                                                                           extensionSchema,
                                                                           extension);
      validatedExtensionList.add(new ValidatedExtension(extensionSchema, validatedExtension));
    }
    return validatedExtensionList;
  }

  /**
   * checks if the extensions within the documents body are missing a required extension
   *
   * @param requiredExtensionList the list of extensions that are required for the {@link #resourceType}
   * @param inResourcePresentExtensions all extensions that were found within the documents body
   */
  protected void checkForMissingRequiredExtensions(List<Schema> requiredExtensionList,
                                                   List<Schema> inResourcePresentExtensions)
  {
    for ( Schema requiredExtension : requiredExtensionList )
    {
      boolean isRequiredExtensionPresent = inResourcePresentExtensions.stream().anyMatch(schema -> {
        return schema.getNonNullId().equals(requiredExtension.getNonNullId());
      });
      if (!isRequiredExtensionPresent)
      {
        throw new DocumentValidationException(String.format("Required extension '%s' is missing",
                                                            requiredExtension.getNonNullId()),
                                              getHttpStatusCode(), null);
      }
    }
  }

  /**
   * the representation of a validated extension
   */
  @Getter
  @RequiredArgsConstructor
  protected class ValidatedExtension
  {

    /**
     * the schemas definition of the validated extension
     */
    private final Schema extensionSchema;

    /**
     * the validated extension itself
     */
    private final ScimObjectNode validatedExtension;
  }
}
