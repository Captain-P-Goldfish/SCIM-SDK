package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.AttributeExtractor;
import de.captaingoldfish.scim.sdk.common.utils.CaseInsensitiveAttributeExtractor;
import de.captaingoldfish.scim.sdk.common.utils.CaseSensitiveAttributeExtractor;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 23.04.2021
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSchemaValidator
{

  /**
   * the service provider configuration in order to check if case-insensitive attribute extraction is enabled or
   * not
   */
  private final ServiceProvider serviceProvider;

  protected final Class resourceNodeType;

  /**
   * the concrete attribute validation that is differs by the context in which the attribute is validated
   *
   * @param schemaAttribute the definition of the attribute that must be validated
   * @param attribute the attribute to validate
   * @return the validated attribute
   */
  protected abstract Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute, JsonNode attribute);

  /**
   * the http status code to use in the {@link DocumentValidationException} if the validation fails. Should be
   * 400 (bad request) for requests and 500 (internal server error) for responses
   */
  protected abstract int getHttpStatusCode();

  /**
   * checks the given document against the schema definition of the {@link #resourceType}
   *
   * @param resource the document that should be validated
   * @return the validated resource
   */
  public ScimObjectNode validateDocument(Schema schema, JsonNode resource)
  {
    try
    {
      checkDocumentAndMetaSchemaRelationship(schema, resource);
      ScimObjectNode scimObjectNode = JsonHelper.getNewInstance(resourceNodeType);
      return validateDocument(scimObjectNode, schema, resource);
    }
    catch (AttributeValidationException ex)
    {
      Throwable cause = ExceptionUtils.getRootCause(ex);
      String errorMessage = Optional.ofNullable(cause).map(Throwable::getMessage).orElse(ex.getMessage());
      throw new DocumentValidationException(errorMessage, ex, getHttpStatusCode(), null);
    }
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
    Supplier<String> noSchemasMessage = () -> String.format("Document does not have a '%s'-attribute",
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
   * this method will validates either a resource document or an extension document that is part of the resource
   * document. Extensions are handled as individual schemas.
   *
   * @param validatedResource The object into which the validated attributes will be added. In case of main
   *          document validation this object will be of type {@link ResourceNode} and in case of extension
   *          validation of type {@link ScimObjectNode}
   * @param resourceSchema the definition of the document that is either the main schema of the
   *          {@link #resourceType} of an extension that is present within the current document
   * @param resource the document that should be validated
   * @return the validated document with its scim attribute representations
   */
  protected ScimObjectNode validateDocument(ScimObjectNode validatedResource, Schema resourceSchema, JsonNode resource)
  {
    AttributeExtractor attributeExtractor = getAttributeExtractor(resource);

    for ( SchemaAttribute schemaAttribute : resourceSchema.getAttributes() )
    {
      log.trace("Validating attribute '{}'", schemaAttribute.getScimNodeName());
      final String attributeName = schemaAttribute.getName();
      JsonNode attribute = attributeExtractor.getAttribute(schemaAttribute).orElse(null);
      Optional<JsonNode> validatedAttributeOptional = validateAttribute(schemaAttribute, attribute);
      validatedAttributeOptional.ifPresent(validatedAttribute -> {
        validatedResource.set(attributeName, validatedAttribute);
      });
    }
    return validatedResource;
  }

  /**
   * retrieves the attribute extractor that should be used based on the service providers configuration
   *
   * @param resource the resource that acts as the attribute extractors base document
   * @return the attribute extractor to use. Default is the case-sensitive attribute extractor
   */
  public AttributeExtractor getAttributeExtractor(JsonNode resource)
  {
    final boolean caseInsensitiveValidation = Optional.ofNullable(serviceProvider)
                                                      .map(ServiceProvider::isCaseInsensitiveValidation)
                                                      .orElse(false);
    if (caseInsensitiveValidation)
    {
      return new CaseInsensitiveAttributeExtractor(resource);
    }
    else
    {
      return new CaseSensitiveAttributeExtractor(resource);
    }
  }
}
