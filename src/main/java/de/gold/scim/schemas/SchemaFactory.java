package de.gold.scim.schemas;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:01 <br>
 * <br>
 * this class can be used to read new resource schemas into the scim context
 */
public final class SchemaFactory
{

  /**
   * the singleton instance of this class
   */
  private static final SchemaFactory INSTANCE = new SchemaFactory();

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
  @Setter(AccessLevel.PROTECTED)
  private ResourceTypeFactory resourceTypeFactory;

  /*
   * this block will register the default schemas defined by RFC7643
   */
  private SchemaFactory()
  {
    this.resourceTypeFactory = ResourceTypeFactory.getInstance();
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_SERVICE_PROVIDER_JSON));
    registerMetaSubSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON), "meta");
  }

  /**
   * @return the singleton instance
   */
  public static SchemaFactory getInstance()
  {
    return INSTANCE;
  }

  /**
   * this method is explicitly for unit tests
   */
  static SchemaFactory getUnitTestInstance(ResourceTypeFactory resourceTypeFactory)
  {
    SchemaFactory schemaFactory = new SchemaFactory();
    schemaFactory.setResourceTypeFactory(resourceTypeFactory);
    return schemaFactory;
  }

  /**
   * will register a new schema
   *
   * @param jsonSchema the schema as json node
   */
  private void registerMetaSchema(JsonNode jsonSchema)
  {
    Schema schema = new Schema(jsonSchema);
    metaSchemas.put(schema.getId(), schema);
  }

  /**
   * will register a new schema
   *
   * @param jsonSchema the schema as json node
   * @param overrideNamePrefix a name that as prepended to the attribute names of the attributes to this schema
   */
  private void registerMetaSubSchema(JsonNode jsonSchema, String overrideNamePrefix)
  {
    Schema schema = new Schema(jsonSchema, overrideNamePrefix);
    metaSchemas.put(schema.getId(), schema);
  }

  /**
   * will register a new resource schema
   *
   * @param jsonSchema the schema as json node
   */
  public void registerResourceSchema(JsonNode jsonSchema)
  {
    Schema metaSchema = getMetaSchema(SchemaUris.SCHEMA_URI);
    SchemaValidator.validateSchemaDocument(resourceTypeFactory, metaSchema, jsonSchema);
    Schema schema = new Schema(jsonSchema);
    resourceSchemas.put(schema.getId(), schema);
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
    return resourceSchemas.get(id);
  }

}
