package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * @author Pascal Knueppel
 * @since 24.04.2021
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

  public ResponseSchemaValidator(ServiceProvider serviceProvider,
                                 Class resourceNodeType,
                                 List<SchemaAttribute> attributesList,
                                 List<SchemaAttribute> excludedAttributesList,
                                 JsonNode requestDocument,
                                 BiFunction<String, String, String> referenceUrlSupplier)
  {
    super(serviceProvider, resourceNodeType);
    this.attributesList = attributesList;
    this.excludedAttributesList = excludedAttributesList;
    this.requestDocument = requestDocument;
    this.referenceUrlSupplier = referenceUrlSupplier;
  }

  /**
   * validates the attribute in a response context
   */
  @Override
  protected Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    JsonNode effectiveAttribute = attribute;
    if (getServiceProvider().isUseDefaultValuesOnResponse())
    {
      effectiveAttribute = DefaultValueHandler.getOrGetDefault(schemaAttribute, attribute);
    }
    return ResponseAttributeValidator.validateAttribute(schemaAttribute,
                                                        effectiveAttribute,
                                                        requestDocument,
                                                        attributesList,
                                                        excludedAttributesList,
                                                        referenceUrlSupplier);
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
