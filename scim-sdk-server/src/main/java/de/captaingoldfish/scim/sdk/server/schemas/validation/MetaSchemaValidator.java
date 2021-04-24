package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * @author Pascal Knueppel
 * @since 24.04.2021
 */
public class MetaSchemaValidator extends AbstractSchemaValidator
{

  /**
   * singleton instance
   */
  private static final MetaSchemaValidator META_SCHEMA_VALIDATOR = new MetaSchemaValidator();

  private MetaSchemaValidator()
  {
    super(ScimObjectNode.class);
  }

  /**
   * @return the singleton instance to validate documents like resourceTypes, resourceSchemas etc. against its
   *         meta schema definitions
   */
  public static MetaSchemaValidator getInstance()
  {
    return META_SCHEMA_VALIDATOR;
  }

  /**
   * validates the document and additionally adds the schemas-attribute to the validated document.
   * 
   * @param schema the schemas definition
   * @param resource the document that should be validated
   * @return the validated schema document
   */
  @Override
  public ScimObjectNode validateDocument(Schema schema, JsonNode resource)
  {
    ScimObjectNode scimObjectNode = super.validateDocument(schema, resource);
    ScimArrayNode schemasNode = new ScimArrayNode(Schema.SCHEMAS_ATTRIBUTE);
    schemasNode.add(new ScimTextNode(Schema.SCHEMAS_ATTRIBUTE, schema.getNonNullId()));
    scimObjectNode.set(AttributeNames.RFC7643.SCHEMAS, schemasNode);
    return scimObjectNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    return MetaAttributeValidator.validateAttribute(schemaAttribute, attribute);
  }
}
