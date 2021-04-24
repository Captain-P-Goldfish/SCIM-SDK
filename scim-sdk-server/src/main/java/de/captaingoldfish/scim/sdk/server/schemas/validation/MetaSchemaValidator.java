package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
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
   * {@inheritDoc}
   */
  @Override
  protected Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    return MetaAttributeValidator.validateAttribute(schemaAttribute, attribute);
  }
}
