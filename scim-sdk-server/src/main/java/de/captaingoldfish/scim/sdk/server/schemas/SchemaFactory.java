package de.captaingoldfish.scim.sdk.server.schemas;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidSchemaException;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:01 <br>
 * <br>
 * this class can be used to read new resource schemas into the scim context
 */
public final class SchemaFactory
{

  /**
   * this map will hold the meta schemata that will define how other schemata must be build
   */
  @Getter(AccessLevel.PROTECTED)
  private final Map<String, Schema> metaSchemas = new HashMap<>();

  /**
   * this map will hold the resource schemata that will define how the resources itself must be build
   */
  @Getter(AccessLevel.PROTECTED)
  private final Map<String, Schema> resourceSchemas = new HashMap<>();

  /**
   * used for unit tests in order to prevent application context pollution
   */
  @Getter(AccessLevel.PROTECTED)
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * this constructor will register the default schemas defined by RFC7643
   */
  protected SchemaFactory(ResourceTypeFactory resourceTypeFactory)
  {
    this.resourceTypeFactory = resourceTypeFactory;
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.RESOURCE_TYPES_FEATURE_EXT_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_SERVICE_PROVIDER_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.BULK_REQUEST_SCHEMA));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.BULK_RESPONSE_SCHEMA));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.PATCH_REQUEST_SCHEMA));
  }

  /**
   * will register a new schema
   *
   * @param jsonSchema the schema as json node
   */
  protected void registerMetaSchema(JsonNode jsonSchema)
  {
    Schema schema = new Schema(jsonSchema);
    metaSchemas.put(schema.getNonNullId(), schema);
  }

  /**
   * will register a new resource schema
   *
   * @param jsonSchema the schema as json node
   */
  public void registerResourceSchema(JsonNode jsonSchema)
  {
    Schema metaSchema = getMetaSchema(SchemaUris.SCHEMA_URI);
    try
    {
      SchemaValidator.validateSchemaDocument(metaSchema, jsonSchema);
      Schema schema = new Schema(jsonSchema);
      // a schema that is already within the meta schemas should not be set as duplicate within the resource schemas
      if (metaSchemas.get(schema.getNonNullId()) == null)
      {
        resourceSchemas.put(schema.getNonNullId(), schema);
      }
      else
      {
        metaSchemas.put(schema.getNonNullId(), schema);
      }
    }
    catch (DocumentValidationException ex)
    {
      throw new InvalidSchemaException(ex.getMessage(), ex, null, null);
    }
  }

  /**
   * extracts a meta schema that will defines the base of another schema like the user resource schema or group
   * resource schema
   *
   * @param id the fully qualified id of the meta schema
   * @return the meta schema if it does exist or null
   */
  public Schema getMetaSchema(String id)
  {
    return metaSchemas.get(id);
  }

  /**
   * extracts a resource schema that will define a resource like "User" or "Group"
   *
   * @param id the fully qualified id of the resource schema
   * @return the resource schema if it does exist or null
   */
  public Schema getResourceSchema(String id)
  {
    Schema schema = resourceSchemas.get(id);
    if (schema != null)
    {
      return schema;
    }
    return metaSchemas.get(id);
  }

}
