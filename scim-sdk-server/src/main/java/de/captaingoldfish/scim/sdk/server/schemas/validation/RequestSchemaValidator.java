package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.DocumentDescription;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * validates a request document against the schema of the current {@link ResourceType}
 * 
 * @author Pascal Knueppel
 * @since 24.02.2021
 */
@Slf4j
public class RequestSchemaValidator
{

  /**
   * the resource type that is the representative for the validation that will be executed on the document
   */
  private final ResourceType resourceType;

  /**
   * the resources class type that will tell us of which type a new instance will be created
   */
  private final Class<? extends ResourceNode> resourceNodeType;

  public RequestSchemaValidator(ResourceType resourceType)
  {
    this.resourceType = resourceType;
    this.resourceNodeType = resourceType.getResourceHandlerImpl().getType();
  }

  /**
   * checks the given document against the schema definition of the {@link #resourceType}
   * 
   * @param resource the document that should be validated
   * @param httpMethod the current request type which is either one of [POST, PUT or PATCH]. The validation must
   *          be handled differently in case of POST requests if an attribute is required and has a mutability
   *          of writeOnly or immutable
   * @return the validated resource
   */
  public ScimObjectNode validateDocument(JsonNode resource, HttpMethod httpMethod)
  {
    try
    {
      DocumentDescription documentDescription = new DocumentDescription(resourceType, resource);
      checkDocumentAndMetaSchemaRelationship(documentDescription.getMetaSchema(), resource);
      final ResourceNode validatedResource = (ResourceNode)validateDocument(JsonHelper.getNewInstance(resourceNodeType),
                                                                            documentDescription.getMetaSchema(),
                                                                            resource,
                                                                            httpMethod);
      final List<Schema> inResourcePresentExtensions = documentDescription.getExtensions();
      List<ValidatedExtension> validatedExtensions = validateExtensions(resourceType.getRequiredResourceSchemaExtensions(),
                                                                        inResourcePresentExtensions,
                                                                        resource,
                                                                        httpMethod);
      for ( ValidatedExtension validatedExtension : validatedExtensions )
      {
        if (!validatedExtension.getValidatedExtension().isEmpty())
        {
          validatedResource.addSchema(validatedExtension.getExtensionSchema().getNonNullId());
          validatedResource.set(validatedExtension.getExtensionSchema().getNonNullId(),
                                validatedExtension.getValidatedExtension());
        }
      }
      Optional.ofNullable(resource.get(AttributeNames.RFC7643.META))
              .ifPresent(meta -> validatedResource.set(AttributeNames.RFC7643.META, meta));
      return validatedResource;
    }
    catch (AttributeValidationException ex)
    {
      Throwable cause = ExceptionUtils.getRootCause(ex);
      String errorMessage = Optional.ofNullable(cause).map(Throwable::getMessage).orElse(ex.getMessage());
      throw new DocumentValidationException(errorMessage, ex, HttpStatus.BAD_REQUEST, null);
    }
  }

  /**
   * this method will validates either a resource document or an extension document that is part of the resource
   * document. Extensions are handled as individual schemas.
   * 
   * @param validatedResource The object into which the validated attributes will be added. In case of main
   *          document validation this object will be of type {@link ResourceNode} and in case of extension
   *          validation of type {@link ScimObjectNode}
   * @param resourceSchema the definition of the document that is either the main schema of the
   *          {@link #resourceType} of an extension that is present within the current document
   * @param resource the document that should be validated
   * @param httpMethod the current request type which is either one of [POST, PUT or PATCH]. The validation must
   *          be handled differently in case of POST requests if an attribute is required and has a mutability
   *          of writeOnly or immutable
   * @return the validated document with its scim attribute representations
   */
  protected ScimObjectNode validateDocument(ScimObjectNode validatedResource,
                                            Schema resourceSchema,
                                            JsonNode resource,
                                            HttpMethod httpMethod)
  {
    for ( SchemaAttribute schemaAttribute : resourceSchema.getAttributes() )
    {
      final String attributeName = schemaAttribute.getName();
      JsonNode attribute = resource.get(attributeName);
      Optional<JsonNode> validatedAttributeOptional = RequestAttributeValidator.validateAttribute(schemaAttribute,
                                                                                                  attribute,
                                                                                                  httpMethod);
      validatedAttributeOptional.ifPresent(validatedAttribute -> {
        validatedResource.set(attributeName, validatedAttribute);
      });
    }
    return validatedResource;
  }

  /**
   * this method will verify that the meta schema is the correct schema to validate the document. This is done
   * by comparing the "id"-attribute of the metaSchema with the "schemas"-attribute of the document
   *
   * @param resourceSchema the resources schema that should be used to validate the document
   * @param document the document that should be validated
   */
  protected JsonNode checkDocumentAndMetaSchemaRelationship(Schema resourceSchema, JsonNode document)
  {
    final String resourceSchemaId = resourceSchema.getNonNullId();
    Supplier<String> noSchemasMessage = () -> String.format("document does not have a '%s'-attribute",
                                                            AttributeNames.RFC7643.SCHEMAS);
    List<String> documentSchemas = JsonHelper.getSimpleAttributeArray(document, AttributeNames.RFC7643.SCHEMAS)
                                             .orElseThrow(() -> new DocumentValidationException(noSchemasMessage.get(),
                                                                                                HttpStatus.BAD_REQUEST,
                                                                                                null));
    log.trace("Resource schema with id {} does apply to document with schemas '{}'", resourceSchemaId, documentSchemas);
    ScimArrayNode schemasNode = new ScimArrayNode(null);
    schemasNode.addAll(documentSchemas.stream().map(s -> new ScimTextNode(null, s)).collect(Collectors.toList()));
    return schemasNode;
  }

  /**
   * validates the extensions that are present within the document that should be validated
   * 
   * @param extensions all extensions that are defined within the {@link #resourceType}
   * @param inResourcePresentExtensions all extensions that were found within the documents body
   * @param httpMethod the current request type which is either one of [POST, PUT or PATCH]. The validation must
   *          be handled differently in case of POST requests if an attribute is required and has a mutability
   *          of writeOnly or immutable
   * @return the list of validated extensions. If an extension evaluated to an empty object it will not be
   *         present within this list
   */
  protected List<ValidatedExtension> validateExtensions(List<Schema> extensions,
                                                        List<Schema> inResourcePresentExtensions,
                                                        JsonNode resource,
                                                        HttpMethod httpMethod)
  {
    List<ValidatedExtension> validatedExtensionList = new ArrayList<>();
    checkForMissingRequiredExtensions(extensions, inResourcePresentExtensions);
    for ( Schema extensionSchema : inResourcePresentExtensions )
    {
      JsonNode extension = resource.get(extensionSchema.getNonNullId());
      ScimObjectNode validatedExtension = validateDocument(new ScimObjectNode(),
                                                           extensionSchema,
                                                           extension,
                                                           httpMethod);
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
                                              HttpStatus.BAD_REQUEST, null);
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
