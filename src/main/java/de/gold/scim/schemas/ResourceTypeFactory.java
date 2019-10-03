package de.gold.scim.schemas;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.exceptions.InvalidResourceTypeException;
import de.gold.scim.utils.HttpStatus;
import de.gold.scim.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 23:11 <br>
 * <br>
 * this class is used to register and get resource types. With this utility class the SCIM endpoints can be
 * extended by additional resource types, resource schemata and resource extensions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceTypeFactory
{

  /**
   * the resource type registry.<br>
   * The key will be the uri to the resource schema that represents the resource type. Like this the resource
   * type can be easier found if a request comes in
   */
  private static final Map<String, ResourceType> RESOURCE_TYPES = new HashMap<>();

  /*
   * will register the default resource types
   */
  static
  {
    registerResourceType(JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON),
                         JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON),
                         JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON));
    registerResourceType(JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_RESOURCE_TYPE_JSON),
                         JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON));
  }

  /**
   * @param resourceType the resource type as json document
   * @param resourceSchema the resource schema definition as json object these object will also be registered
   * @param resourceSchemaExtensions the extensions that will be appended to the {@code resourceSchema}
   *          definition
   */
  public static void registerResourceType(JsonNode resourceType,
                                          JsonNode resourceSchema,
                                          JsonNode... resourceSchemaExtensions)
  {
    addSchemaExtensions(resourceType, resourceSchemaExtensions);
    SchemaFactory.registerResourceSchema(resourceSchema);
    ResourceType resourceTypeObject = new ResourceType(resourceType);
    RESOURCE_TYPES.put(resourceTypeObject.getSchema(), resourceTypeObject);
  }

  /**
   * clears the inhabited schema extensions list from the resource type that was parsed from the json and
   * overrides it with the given resource schema extensions
   *
   * @param resourceType the resource type definition
   * @param resourceSchemaExtensions an array of resource extensions if extensions are present
   */
  private static void addSchemaExtensions(JsonNode resourceType, JsonNode[] resourceSchemaExtensions)
  {
    if (resourceSchemaExtensions != null && resourceSchemaExtensions.length > 0)
    {
      ArrayNode schemaExtensions = JsonHelper.getArrayAttribute(resourceType, AttributeNames.SCHEMA_EXTENSIONS)
                                             .orElse(new ArrayNode(JsonNodeFactory.instance));
      schemaExtensions.removeAll();
      for ( JsonNode resourceSchemaExtension : resourceSchemaExtensions )
      {
        String extensionId = JsonHelper.getSimpleAttribute(resourceSchemaExtension, AttributeNames.ID)
                                       .orElseThrow(() -> getAttributeMissingException(AttributeNames.ID));
        schemaExtensions.add(new TextNode(extensionId));
      }
      JsonHelper.addAttribute(resourceType, AttributeNames.SCHEMA_EXTENSIONS, schemaExtensions);
    }
  }

  /**
   * @param attributeName the name of the attribute that was missing
   * @return creates an invalid resource type exception
   */
  private static InvalidResourceTypeException getAttributeMissingException(String attributeName)
  {
    String errorMessage = "schema extension is missing '" + attributeName + "' attribute";
    return new InvalidResourceTypeException(errorMessage, null, HttpStatus.SC_INTERNAL_SERVER_ERROR, null);
  }

  /**
   * builds a json resource type object and calls {@link #registerResourceType(JsonNode, JsonNode, JsonNode...)}
   *
   * @param id the id of the resource type
   * @param name the name of the resource type
   * @param description the description of the resource type
   * @param schema the resource type schema that describes this resource type
   * @param endpoint the endpoint under which this resource should be reachable
   * @param resourceSchema the resource schema definition as json object these object will also be registered
   * @param resourceSchemaExtensions the extensions that will be appended to the {@code resourceSchema}
   *          definition
   */
  public static void registerResourceType(String id,
                                          String name,
                                          String description,
                                          String schema,
                                          String endpoint,
                                          JsonNode resourceSchema,
                                          JsonNode... resourceSchemaExtensions)
  {
    ObjectNode resourceType = new ObjectNode(JsonNodeFactory.instance);
    ArrayNode schemasNode = new ArrayNode(JsonNodeFactory.instance);
    schemasNode.add(new TextNode(SchemaUris.RESOURCE_TYPE_URI));
    JsonHelper.addAttribute(resourceType, AttributeNames.SCHEMAS, schemasNode);
    JsonHelper.addAttribute(resourceType, AttributeNames.ID, new TextNode(id));
    JsonHelper.addAttribute(resourceType, AttributeNames.NAME, new TextNode(name));
    JsonHelper.addAttribute(resourceType, AttributeNames.DESCRIPTION, new TextNode(description));
    JsonHelper.addAttribute(resourceType, AttributeNames.SCHEMA, new TextNode(schema));
    JsonHelper.addAttribute(resourceType, AttributeNames.ENDPOINT, new TextNode(endpoint));
    registerResourceType(resourceType, resourceSchema, resourceSchemaExtensions);
  }

  /**
   * tries to get a resource type by the schema uri of a resource
   *
   * @param schemaUri the schema uri of a resource e.g. {@link de.gold.scim.constants.SchemaUris#USER_URI}
   */
  public static ResourceType getResourceType(String schemaUri)
  {
    return RESOURCE_TYPES.get(schemaUri);
  }
}
