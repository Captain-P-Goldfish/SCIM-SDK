package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 21.04.2021
 */
@Slf4j
public class ResponseResourceValidator extends AbstractResourceValidator
{

  public ResponseResourceValidator(ServiceProvider serviceProvider,
                                   ResourceType resourceType,
                                   List<SchemaAttribute> attributesList,
                                   List<SchemaAttribute> excludedAttributesList,
                                   JsonNode requestDocument,
                                   BiFunction<String, String, String> referenceUrlSupplier)
  {
    super(resourceType,
          new ResponseSchemaValidator(serviceProvider, resourceType.getResourceHandlerImpl().getType(), attributesList,
                                      excludedAttributesList, requestDocument, referenceUrlSupplier));
  }

  /**
   * Does validate the meta-attribute after the document itself has been validated
   * 
   * @param resource the document that should be validated
   * @return the validated document with the also validated meta attribute
   */
  @Override
  public ScimObjectNode validateDocument(JsonNode resource)
  {
    log.trace("Start validating response resource");
    ScimObjectNode validatedResource = super.validateDocument(resource);
    final JsonNode metaAttributeWrapper = getSchemaValidator().validateDocument(new ScimObjectNode(null),
                                                                                getResourceType().getMetaSchema(),
                                                                                resource);
    if (!metaAttributeWrapper.isEmpty())
    {
      JsonNode metaNode = metaAttributeWrapper.get(AttributeNames.RFC7643.META);
      validatedResource.set(AttributeNames.RFC7643.META, metaNode);
    }
    boolean containsOnlyAttributesSchemasAndMeta = validatedResource.size() == 2
                                                   && validatedResource.has(AttributeNames.RFC7643.SCHEMAS)
                                                   && validatedResource.has(AttributeNames.RFC7643.META);
    boolean isEmpty = validatedResource.isEmpty() || containsOnlyAttributesSchemasAndMeta;
    if (isEmpty)
    {
      log.debug("Validated response resource is empty");
      return null;
    }
    return validatedResource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getHttpStatusCode()
  {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
