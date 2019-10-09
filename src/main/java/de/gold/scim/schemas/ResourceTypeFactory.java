package de.gold.scim.schemas;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.endpoints.ResourceHandler;
import de.gold.scim.exceptions.InvalidResourceTypeException;
import de.gold.scim.utils.HttpStatus;
import de.gold.scim.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 23:11 <br>
 * <br>
 * this class is used to register and get resource types. With this utility class the SCIM endpoints can be
 * extended by additional resource types, resource schemata and resource extensions
 */
public final class ResourceTypeFactory
{

  /**
   * the singleton instance
   */
  private static final ResourceTypeFactory INSTANCE = new ResourceTypeFactory();

  /**
   * the resource type registry.<br>
   * The key will be the uri to the resource schema that represents the resource type. Like this the resource
   * type can be easier found if a request comes in
   */
  private final Map<String, ResourceType> resourceTypes = new HashMap<>();

  /**
   * this instance is hold in order for unit tests to be able to write tests without polluting the whole
   * application context which might lead to unpredictable unit test errors
   */
  @Getter(AccessLevel.PROTECTED)
  @Setter(AccessLevel.PROTECTED)
  private SchemaFactory schemaFactory;

  /**
   * will register the default resource types
   */
  private ResourceTypeFactory()
  {
    this.schemaFactory = SchemaFactory.getInstance();
  }

  /**
   * @return the singleton instance
   */
  public static ResourceTypeFactory getInstance()
  {
    return INSTANCE;
  }

  /**
   * this method is explicitly for unit tests
   */
  static ResourceTypeFactory getUnitTestInstance()
  {
    ResourceTypeFactory resourceTypeFactory = new ResourceTypeFactory();
    resourceTypeFactory.setSchemaFactory(SchemaFactory.getUnitTestInstance(resourceTypeFactory));
    return resourceTypeFactory;
  }

  /**
   * this method will register a new resource type
   *
   * @param resourceType the resource type as json document
   * @param resourceSchema the resource schema definition as json object this object will additionally be
   *          registered within the {@link SchemaFactory}. This is the resource that is referenced under the
   *          "schema" attribute within the {@code resourceType} document
   * @param resourceSchemaExtensions the extensions that will be appended to the {@code resourceSchema}
   *          definition
   */
  public void registerResourceType(ResourceHandler resourceHandler,
                                   JsonNode resourceType,
                                   JsonNode resourceSchema,
                                   JsonNode... resourceSchemaExtensions)
  {
    addSchemaExtensions(resourceType, resourceSchemaExtensions);
    schemaFactory.registerResourceSchema(resourceSchema);
    ResourceType resourceTypeObject = new ResourceType(schemaFactory, this, resourceType);
    resourceTypeObject.setResourceHandlerImpl(resourceHandler);
    resourceTypes.put(resourceTypeObject.getEndpoint(), resourceTypeObject);
  }

  /**
   * reads the extensions defined withing the resource type document and validates them against the given
   * extension schemata and will register the extension schemata in the {@link SchemaFactory} if the validation
   * succeeds
   *
   * @param resourceType the resource type definition
   * @param resourceSchemaExtensions an array of resource extensions if extensions are present
   */
  private void addSchemaExtensions(JsonNode resourceType, JsonNode[] resourceSchemaExtensions)
  {
    ArrayNode schemaExtensions = JsonHelper.getArrayAttribute(resourceType, AttributeNames.SCHEMA_EXTENSIONS)
                                           .orElse(null);
    // if no further validation is required return from the method
    if (validateSchemaExtensionParameter(schemaExtensions, resourceSchemaExtensions))
    {
      return;
    }
    Set<String> resourceTypeExtensions = new HashSet<>();
    for ( JsonNode schemaExtension : schemaExtensions )
    {
      String schema = JsonHelper.getSimpleAttribute(schemaExtension, AttributeNames.SCHEMA)
                                .orElseThrow(() -> getAttributeMissingException(AttributeNames.SCHEMA));
      resourceTypeExtensions.add(schema);
    }
    Set<String> extensionIds = new HashSet<>();
    for ( JsonNode resourceSchemaExtension : resourceSchemaExtensions )
    {
      String extensionId = JsonHelper.getSimpleAttribute(resourceSchemaExtension, AttributeNames.ID)
                                     .orElseThrow(() -> getAttributeMissingException(AttributeNames.ID));
      extensionIds.add(extensionId);
    }
    if (!resourceTypeExtensions.equals(extensionIds))
    {
      throw new InvalidResourceTypeException("you did not register the extensions", null, null, null);
    }
    for ( JsonNode resourceSchemaExtension : resourceSchemaExtensions )
    {
      schemaFactory.registerResourceSchema(resourceSchemaExtension);
    }
    JsonHelper.addAttribute(resourceType, AttributeNames.SCHEMA_EXTENSIONS, schemaExtensions);
  }

  /**
   * will validate if the given extension parameters are valid and throws an exception if not
   *
   * @param schemaExtensions the extensions defined in the resource type json document
   * @param resourceSchemaExtensions the extension schemata that should be registered for the resource type
   * @return true if no further validation is needed and false if additional validation must be done
   */
  private boolean validateSchemaExtensionParameter(ArrayNode schemaExtensions, JsonNode[] resourceSchemaExtensions)
  {
    if (schemaExtensions == null && (resourceSchemaExtensions == null || resourceSchemaExtensions.length == 0))
    {
      // everything is fine. No extensions declared in the resource type and no extensions should be registered
      return true;
    }
    else if (schemaExtensions == null)
    {
      throw new InvalidResourceTypeException("you tried to add extensions that are not present in the resource type "
                                             + "json document", null, null, null);
    }
    else if (resourceSchemaExtensions.length < schemaExtensions.size())
    {
      throw new InvalidResourceTypeException("you missed to add an extension to the resource type. You added "
                                             + Arrays.asList(resourceSchemaExtensions)
                                             + " but the required extensions are " + schemaExtensions, null, null,
                                             null);
    }
    else if (resourceSchemaExtensions.length > schemaExtensions.size())
    {
      throw new InvalidResourceTypeException("you added too many extensions to the resource type. You added "
                                             + Arrays.asList(resourceSchemaExtensions)
                                             + " but the required extensions are " + schemaExtensions, null, null,
                                             null);
    }
    return false;
  }

  /**
   * @param attributeName the name of the attribute that was missing
   * @return creates an invalid resource type exception
   */
  private InvalidResourceTypeException getAttributeMissingException(String attributeName)
  {
    String errorMessage = "schema extension is missing '" + attributeName + "' attribute";
    return new InvalidResourceTypeException(errorMessage, null, HttpStatus.SC_INTERNAL_SERVER_ERROR, null);
  }

  /**
   * builds a json resource type object and calls
   * {@link #registerResourceType(ResourceHandler, JsonNode, JsonNode, JsonNode...)}
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
  public void registerResourceType(ResourceHandler resourceHandler,
                                   String id,
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
    registerResourceType(resourceHandler, resourceType, resourceSchema, resourceSchemaExtensions);
  }

  /**
   * tries to get a resource type by the endpoint path under which it is accessible
   *
   * @param endpoint the endpoint of the resource type
   */
  public ResourceType getResourceType(String endpoint)
  {
    String path = endpoint;
    if (!path.startsWith("/"))
    {
      path = "/" + path;
    }
    return resourceTypes.get(path);
  }

  /**
   * checks if a resource type with the given name does exist
   *
   * @param resourceName the name of the resource
   * @return true if a resource type with the given name was already registered, false else
   */
  protected boolean isResourceRegistered(String resourceName)
  {
    return resourceTypes.values().stream().map(ResourceType::getName).anyMatch(resourceName::equals);
  }
}
