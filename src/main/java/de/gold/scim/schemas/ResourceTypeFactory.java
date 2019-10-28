package de.gold.scim.schemas;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.endpoints.ResourceHandler;
import de.gold.scim.exceptions.InvalidResourceTypeException;
import de.gold.scim.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 23:11 <br>
 * <br>
 * this class is used to register and get resource types. With this utility class the SCIM endpoints can be
 * extended by additional resource types, resource schemata and resource extensions
 */
@Slf4j
public final class ResourceTypeFactory
{

  /**
   * the resource type registry.<br>
   * The key will be the uri to the resource schema that represents the resource type. Like this the resource
   * type can be easier found if a request comes in
   */
  @Getter(AccessLevel.PROTECTED)
  private final Map<String, ResourceType> resourceTypes = new HashMap<>();

  /**
   * this instance is hold in order for unit tests to be able to write tests without polluting the whole
   * application context which might lead to unpredictable unit test errors
   */
  @Getter
  @Setter(AccessLevel.PRIVATE)
  private SchemaFactory schemaFactory;

  /**
   * will register the default resource types
   */
  public ResourceTypeFactory()
  {
    this.schemaFactory = new SchemaFactory(this);
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
  public ResourceType registerResourceType(ResourceHandler resourceHandler,
                                           JsonNode resourceType,
                                           JsonNode resourceSchema,
                                           JsonNode... resourceSchemaExtensions)
  {
    Schema resourceTypeSchema = schemaFactory.getMetaSchema(SchemaUris.RESOURCE_TYPE_URI);
    JsonNode validatedResourceType = SchemaValidator.validateSchemaDocument(this, resourceTypeSchema, resourceType);
    ResourceType resourceTypeObject = new ResourceType(schemaFactory, resourceType);
    addSchemaExtensions(validatedResourceType, resourceSchemaExtensions);
    checkResourceSchema(resourceTypeObject, resourceSchema);
    resourceTypeObject.setResourceHandlerImpl(resourceHandler);
    resourceTypes.put(resourceTypeObject.getEndpoint(), resourceTypeObject);
    return resourceTypeObject;
  }

  /**
   * checks if the resource schema for the resource type is already registered.<br>
   * if the resource schema is null it is expected, that the resource schema does already exist. If not an
   * exception is thrown. Otherwise the given resource schema is registered and might override an existing
   * schema with the same id
   *
   * @param resourceTypeObject the resource type data
   * @param resourceSchema the representing main resource schema for the resource type
   */
  private void checkResourceSchema(ResourceType resourceTypeObject, JsonNode resourceSchema)
  {
    Schema registeredResourceSchema = schemaFactory.getResourceSchema(resourceTypeObject.getSchema());
    if (resourceSchema == null && registeredResourceSchema == null)
    {
      String errorMessage = "the resource type cannot be registered since the required resource schema '"
                            + resourceTypeObject.getSchema() + "' is not registered yet";
      throw new InvalidResourceTypeException(errorMessage, null, null, null);
    }
    else
    {
      if (registeredResourceSchema != null && resourceSchema != null
          && !registeredResourceSchema.equals(resourceSchema))
      {
        log.warn("resource schema with id '{}' is already registered. The new instance that was given is not equal to"
                 + " the old schema document which will be overridden ",
                 resourceTypeObject.getSchema());
      }
      if (resourceSchema != null)
      {
        schemaFactory.registerResourceSchema(resourceSchema);
      }
    }
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
    ArrayNode schemaExtensions = JsonHelper.getArrayAttribute(resourceType, AttributeNames.RFC7643.SCHEMA_EXTENSIONS)
                                           .orElse(null);
    Set<String> resourceTypeExtensionIds = getExtensionIds(schemaExtensions);
    Set<String> extensionsToRegisterIds = getExtensionIds(resourceSchemaExtensions);
    if (resourceTypeExtensionIds.isEmpty() && extensionsToRegisterIds.isEmpty())
    {
      // no further validation is needed because no extensions have been added
      return;
    }
    validateSchemaExtensions(resourceTypeExtensionIds, extensionsToRegisterIds);
    for ( JsonNode resourceSchemaExtension : resourceSchemaExtensions )
    {
      schemaFactory.registerResourceSchema(resourceSchemaExtension);
    }
  }

  /**
   * will validate if the given extension parameters are valid and throws an exception if not.
   *
   * @param resourceTypeExtensionIds the ids of the extensions present in the resource type document
   * @param extensionsToRegisterIds the ids of the extensions that were given to the method
   *          {@link #registerResourceType(ResourceHandler, JsonNode, JsonNode, JsonNode...)} for registration
   */
  private void validateSchemaExtensions(Set<String> resourceTypeExtensionIds, Set<String> extensionsToRegisterIds)
  {
    if (resourceTypeExtensionIds.equals(extensionsToRegisterIds))
    {
      // everything is fine. The call added exactly the same extensions that were added to the resource type
      return;
    }
    validateUnreferencedExtensions(resourceTypeExtensionIds, extensionsToRegisterIds);
    validateMissingExtensions(resourceTypeExtensionIds, extensionsToRegisterIds);
  }

  /**
   * this method will check if missing extensions are present that these extensions are already registered. If
   * this is not the case an exception is thrown
   *
   * @param resourceTypeExtensionIds the ids of the extensions present in the resource type document
   * @param extensionsToRegisterIds the ids of the extensions that were given to the method
   *          {@link #registerResourceType(ResourceHandler, JsonNode, JsonNode, JsonNode...)} for registration
   */
  private void validateMissingExtensions(Set<String> resourceTypeExtensionIds, Set<String> extensionsToRegisterIds)
  {
    Set<String> resourceTypeIds = new HashSet<>(resourceTypeExtensionIds);
    resourceTypeIds.removeAll(extensionsToRegisterIds);

    for ( String extensionId : resourceTypeIds )
    {
      if (schemaFactory.getResourceSchema(extensionId) == null)
      {
        String errorMessage = "You missed to add the extension with the id '" + extensionId + "' for registration";
        throw new InvalidResourceTypeException(errorMessage, null, null, null);
      }
    }
  }

  /**
   * this method checks if extensions have been added that are not referenced by the resource type
   *
   * @param resourceTypeExtensionIds the ids of the extensions present in the resource type document
   * @param extensionsToRegisterIds the ids of the extensions that were given to the method
   *          {@link #registerResourceType(ResourceHandler, JsonNode, JsonNode, JsonNode...)} for registration
   */
  private void validateUnreferencedExtensions(Set<String> resourceTypeExtensionIds, Set<String> extensionsToRegisterIds)
  {

    if (!resourceTypeExtensionIds.containsAll(extensionsToRegisterIds))
    {
      throw new InvalidResourceTypeException("The extensions " + extensionsToRegisterIds + " are not "
                                             + "referenced in the schemaExtensions attribute within the resource "
                                             + "type. The referenced schemas are: " + resourceTypeExtensionIds, null,
                                             null, null);
    }
  }

  private Set<String> getExtensionIds(ArrayNode arrayNode)
  {
    if (arrayNode == null || arrayNode.isEmpty())
    {
      return Collections.emptySet();
    }
    Set<String> resourceTypeExtensionIds = new HashSet<>();
    for ( JsonNode extension : arrayNode )
    {
      // should never give a nullPointer since schema validation was executed before
      resourceTypeExtensionIds.add(extension.get(AttributeNames.RFC7643.SCHEMA).textValue());
    }
    return resourceTypeExtensionIds;
  }

  private Set<String> getExtensionIds(JsonNode[] resourceSchemaExtensions)
  {
    if (resourceSchemaExtensions == null || resourceSchemaExtensions.length == 0)
    {
      return Collections.emptySet();
    }
    return Arrays.stream(resourceSchemaExtensions)
                 // should never give a nullPointer since schema validation was executed before
                 .map(jsonNode -> jsonNode.get(AttributeNames.RFC7643.ID).textValue())
                 .collect(Collectors.toSet());
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
    JsonHelper.addAttribute(resourceType, AttributeNames.RFC7643.SCHEMAS, schemasNode);
    JsonHelper.addAttribute(resourceType, AttributeNames.RFC7643.ID, new TextNode(id));
    JsonHelper.addAttribute(resourceType, AttributeNames.RFC7643.NAME, new TextNode(name));
    JsonHelper.addAttribute(resourceType, AttributeNames.RFC7643.DESCRIPTION, new TextNode(description));
    JsonHelper.addAttribute(resourceType, AttributeNames.RFC7643.SCHEMA, new TextNode(schema));
    JsonHelper.addAttribute(resourceType, AttributeNames.RFC7643.ENDPOINT, new TextNode(endpoint));
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

  /**
   * gets a resource type by its name value
   *
   * @param name the name value of the given resource type
   * @return the resource type or an empty if no resource type with the given name exists
   */
  public Optional<ResourceType> getResourceTypeByName(String name)
  {
    return resourceTypes.values().stream().filter(resourceType -> resourceType.getName().equals(name)).findAny();
  }

  /**
   * @return returns all registered resource types
   */
  public Collection<ResourceType> getAllResourceTypes()
  {
    return resourceTypes.values();
  }
}
