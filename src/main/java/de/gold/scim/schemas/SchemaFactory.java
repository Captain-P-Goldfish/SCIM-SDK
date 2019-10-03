package de.gold.scim.schemas;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:01 <br>
 * <br>
 * this class can be used to read new resource schemas into the scim context
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchemaFactory
{

  /**
   * this map will hold the meta schemata that will define how other schemata must be build
   */
  private static final Map<String, Schema> META_SCHEMAS = new HashMap<>();

  /**
   * this map will hold the resource schemata that will define how the resources itself must be build
   */
  private static final Map<String, Schema> RESOURCE_SCHEMAS = new HashMap<>();

  /*
   * this block will register the default schemas defined by RFC7643
   */
  static
  {
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON));
    registerMetaSchema(JsonHelper.loadJsonDocument(ClassPathReferences.META_SERVICE_PROVIDER_JSON));

    registerResourceSchema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    registerResourceSchema(JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON));
  }

  /**
   * will register a new schema
   *
   * @param jsonSchema the schema as json node
   */
  private static void registerMetaSchema(JsonNode jsonSchema)
  {
    Schema schema = new Schema(jsonSchema);
    META_SCHEMAS.put(schema.getId(), schema);
  }

  /**
   * will register a new resource schema
   *
   * @param jsonSchema the schema as json node
   */
  public static void registerResourceSchema(JsonNode jsonSchema)
  {
    Schema schema = new Schema(jsonSchema);
    RESOURCE_SCHEMAS.put(schema.getId(), schema);
  }

  /**
   * extracts a meta schema that will defines the base of another schema like the user resource schema or group
   * resource schema
   *
   * @param id the fully qualified id of the meta schema
   * @return the meta schema if it does exist or null
   */
  public static Schema getMetaSchema(String id)
  {
    return META_SCHEMAS.get(id);
  }

  /**
   * extracts a resource schema that will define a resource like "User" or "Group"
   *
   * @param id the fully qualified id of the resource schema
   * @return the resource schema if it does exist or null
   */
  public static Schema getResourceSchema(String id)
  {
    return RESOURCE_SCHEMAS.get(id);
  }

}
