package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 24.02.2021
 */
@Slf4j
public class RequestSchemaValidator
{

  private final ResourceType resourceType;

  private final Class<? extends ResourceNode> resourceNodeType;

  public RequestSchemaValidator(ResourceType resourceType)
  {
    this.resourceType = resourceType;
    this.resourceNodeType = resourceType.getResourceHandlerImpl().getType();
  }

  public ScimObjectNode validateDocument(JsonNode resource, HttpMethod httpMethod)
  {
    DocumentDescription documentDescription = new DocumentDescription(resourceType, resource);
    checkDocumentAndMetaSchemaRelationship(documentDescription.getMetaSchema(), resource);
    final ResourceNode validatedResource = (ResourceNode)validateResource(JsonHelper.getNewInstance(resourceNodeType),
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
    return validatedResource;
  }

  protected ScimObjectNode validateResource(ScimObjectNode validatedResource,
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
      ScimObjectNode validatedExtension = validateResource(new ScimObjectNode(),
                                                           extensionSchema,
                                                           extension,
                                                           httpMethod);
      validatedExtensionList.add(new ValidatedExtension(extensionSchema, validatedExtension));
    }
    return validatedExtensionList;
  }

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

  @Getter
  @RequiredArgsConstructor
  protected class ValidatedExtension
  {

    private final Schema extensionSchema;

    private final ScimObjectNode validatedExtension;
  }
}
