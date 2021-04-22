package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;


/**
 * @author Pascal Knueppel
 * @since 21.04.2021
 */
public class ResponseSchemaValidator extends AbstractSchemaValidator
{

  /**
   * the attributes parameter list from the clients request
   */
  private final List<SchemaAttribute> attributesList;

  /**
   * the excluded attributes parameter list from the clients request
   */
  private final List<SchemaAttribute> excludedAttributesList;

  /**
   * the request object of the client that is used to evaluate if an attribute with a returned-value of
   * "request" or "default" should be returned if the attributes parameter is present.
   */
  private final JsonNode requestDocument;

  /**
   * overrides the $ref attribute within a given complex attribute if not already set and enough information is
   * available
   */
  private final BiFunction<String, String, String> referenceUrlSupplier;

  public ResponseSchemaValidator(ResourceType resourceType,
                                 List<SchemaAttribute> attributesList,
                                 List<SchemaAttribute> excludedAttributesList,
                                 JsonNode requestDocument,
                                 BiFunction<String, String, String> referenceUrlSupplier)
  {
    super(resourceType);
    this.attributesList = attributesList;
    this.excludedAttributesList = excludedAttributesList;
    this.requestDocument = requestDocument;
    this.referenceUrlSupplier = referenceUrlSupplier;
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
    ScimObjectNode validatedResource = super.validateDocument(resource);
    final JsonNode metaAttributeWrapper = validateDocument(new ScimObjectNode(null),
                                                           getResourceType().getMetaSchema(),
                                                           resource);
    if (!metaAttributeWrapper.isEmpty())
    {
      JsonNode metaNode = metaAttributeWrapper.get(AttributeNames.RFC7643.META);
      validatedResource.set(AttributeNames.RFC7643.META, metaNode);
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

  /**
   * validates the attribute in a response context
   */
  @Override
  protected Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                        attribute,
                                                        requestDocument,
                                                        attributesList,
                                                        excludedAttributesList,
                                                        referenceUrlSupplier);
  }
}
