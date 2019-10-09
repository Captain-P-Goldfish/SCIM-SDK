package de.gold.scim.schemas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.constants.ScimType;
import de.gold.scim.endpoints.ResourceHandler;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.DocumentValidationException;
import de.gold.scim.exceptions.InvalidResourceTypeException;
import de.gold.scim.exceptions.ScimException;
import de.gold.scim.utils.HttpStatus;
import de.gold.scim.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:36 <br>
 * <br>
 * represents a resource type in SCIM. A resource type defines an endpoint definition that can be used by
 * clients.
 */
@Slf4j
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ResourceType
{

  /**
   * used for unit tests in order to prevent application context pollution
   */
  private final SchemaFactory schemaFactory;

  /**
   * used for unit tests in order to prevent application context pollution
   */
  private final ResourceTypeFactory resourceTypeFactory;

  /**
   * the references to the meta schemas that describe this endpoint definition
   */
  private final List<String> schemas;

  /**
   * the id that should point to the name of the resource itself described by this endpoint definition
   */
  private final String id;

  /**
   * the endpoint under which the resource can be accessed
   */
  private final String endpoint;

  /**
   * the name of the document which is normally the same as the id
   */
  private String name;

  /**
   * an optional description
   */
  private String description;

  /**
   * the reference to the resource schema
   */
  private String schema;

  /**
   * the extensions that are supported by this resource
   */
  private List<SchemaExtension> schemaExtensions;

  /**
   * the resource handler implementation that is able to handle this kind of resource
   */
  @Getter(AccessLevel.PUBLIC)
  private ResourceHandler resourceHandlerImpl;

  public ResourceType(String resourceDocument)
  {
    this(null, null, JsonHelper.readJsonDocument(resourceDocument));
  }

  protected ResourceType(SchemaFactory schemaFactory, ResourceTypeFactory resourceTypeFactory, String resourceDocument)
  {
    this(schemaFactory, resourceTypeFactory, JsonHelper.readJsonDocument(resourceDocument));
  }

  protected ResourceType(SchemaFactory schemaFactory,
                         ResourceTypeFactory resourceTypeFactory,
                         JsonNode resourceTypeDocument)
  {
    if (log.isTraceEnabled())
    {
      log.trace("parse resource type document: \n{}", resourceTypeDocument.toPrettyString());
    }
    this.schemaFactory = getSchemaFactory(schemaFactory);
    this.resourceTypeFactory = getResourceTypeFactory(resourceTypeFactory);
    Schema resourceMetaSchema = getMetaSchemaFromDocument(resourceTypeDocument);
    validateResourceType(resourceMetaSchema, resourceTypeDocument);
    this.schemas = JsonHelper.getSimpleAttributeArray(resourceTypeDocument, AttributeNames.SCHEMAS)
                             .orElse(Collections.singletonList(SchemaUris.RESOURCE_TYPE_URI));
    this.id = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.ID)
                        .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.ID)));
    this.name = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.NAME)
                          .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.NAME)));
    this.description = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.DESCRIPTION).orElse(null);
    this.endpoint = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.ENDPOINT)
                              .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.ENDPOINT)));
    this.schema = JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.SCHEMA)
                            .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.SCHEMA)));
    Optional.ofNullable(schemaFactory.getResourceSchema(schema))
            .orElseThrow(() -> getInvalidResourceException("the resource schema with the uri '" + schema
                                                           + "' has not been registered"));
    schemaExtensions = new ArrayList<>();
    JsonHelper.getArrayAttribute(resourceTypeDocument, AttributeNames.SCHEMA_EXTENSIONS).ifPresent(jsonNodes -> {
      for ( JsonNode jsonNode : jsonNodes )
      {
        schemaExtensions.add(new SchemaExtension(jsonNode));
      }
    });
  }

  /**
   * gets the {@link SchemaFactory} that should be used. This method is only added to be able to add clean
   * {@link SchemaFactory} instances from unit tests
   *
   * @param schemaFactory null or a clean factory created by an unit test
   * @return the factory to use
   */
  private SchemaFactory getSchemaFactory(SchemaFactory schemaFactory)
  {
    if (schemaFactory == null)
    {
      return SchemaFactory.getInstance();
    }
    else
    {
      return schemaFactory;
    }
  }

  /**
   * gets the {@link ResourceTypeFactory} that should be used. This method is only added to be able to add clean
   * {@link ResourceTypeFactory} instances from unit tests
   *
   * @param resourceTypeFactory null or a clean factory created by an unit test
   * @return the factory to use
   */
  private ResourceTypeFactory getResourceTypeFactory(ResourceTypeFactory resourceTypeFactory)
  {
    if (resourceTypeFactory == null)
    {
      return ResourceTypeFactory.getInstance();
    }
    else
    {
      return resourceTypeFactory;
    }
  }

  /**
   * validates the given resource type document against its meta schema definition
   *
   * @param resourceMetaSchema the meta schema for resource types
   * @param resourceTypeDocument the resource type document
   */
  private void validateResourceType(Schema resourceMetaSchema, JsonNode resourceTypeDocument)
  {
    try
    {
      SchemaValidator.validateDocumentForResponse(resourceTypeFactory,
                                                  resourceMetaSchema,
                                                  resourceTypeDocument);
    }
    catch (DocumentValidationException ex)
    {
      throw new InvalidResourceTypeException("The given resource type is not valid: " + ex.getMessage(), ex,
                                             ex.getStatus(), ex.getScimType());
    }
  }

  /**
   * Will get the meta schema for resource types and validate that the resource type meta schema uri is present
   * within the schemas-attribute of the given document the resource type documents are supposed to have no
   * extensions. Therefore
   *
   * @param resourceTypeDocument the resource type json document to extract the meta schema from. Eventhough we
   *          know which value must be present in the schemas attribute we will just validate that it was
   *          entered correctly
   * @return the meta schema for resource types
   */
  private Schema getMetaSchemaFromDocument(JsonNode resourceTypeDocument)
  {
    Supplier<String> errorMessage = () -> missingAttrMessage(AttributeNames.SCHEMAS);
    List<String> schemas = JsonHelper.getSimpleAttributeArray(resourceTypeDocument, AttributeNames.SCHEMAS)
                                     .orElseThrow(() -> getInvalidResourceException(errorMessage.get()));
    if (schemas.size() != 1 || !schemas.contains(SchemaUris.RESOURCE_TYPE_URI))
    {
      throw getInvalidResourceException("The resource type document must contain only a single uri in the schemas "
                                        + "attribute: " + SchemaUris.RESOURCE_TYPE_URI);
    }
    return schemaFactory.getMetaSchema(SchemaUris.RESOURCE_TYPE_URI);
  }

  /**
   * @return the required resource schema extensions that represents this resource type
   */
  public List<Schema> getRequiredResourceSchemaExtensions()
  {
    return schemaExtensions.stream()
                           .filter(SchemaExtension::isRequired)
                           .map(SchemaExtension::getSchema)
                           .map(schemaFactory::getResourceSchema)
                           .collect(Collectors.toList());
  }

  /**
   * @return the not required resource schema extensions that represents this resource type
   */
  public List<Schema> getNotRequiredResourceSchemaExtensions()
  {
    return schemaExtensions.stream()
                           .filter(schemaExtension -> !schemaExtension.isRequired())
                           .map(SchemaExtension::getSchema)
                           .map(schemaFactory::getResourceSchema)
                           .collect(Collectors.toList());
  }

  /**
   * will find the meta resource schema and its extensions of this resource type that apply to the given
   * document
   *
   * @param resourceDocument a document that should be validated against its schemas
   * @return a holder object that contains the meta schemata that can be used to validate the given document
   */
  public ResourceSchema getResourceSchema(String resourceDocument)
  {
    return getResourceSchema(JsonHelper.readJsonDocument(resourceDocument));
  }

  /**
   * will find the meta resource schema and its extensions of this resource type that apply to the given
   * document
   *
   * @param resourceDocument a document that should be validated against its schemas
   * @return a holder object that contains the meta schemata that can be used to validate the given document
   */
  public ResourceSchema getResourceSchema(JsonNode resourceDocument)
  {
    return new ResourceSchema(resourceDocument);
  }

  /**
   * builds an error message in case of a required missing attribute
   *
   * @param attributeName the name of the attribute that is missing
   * @return the error message
   */
  private String missingAttrMessage(String attributeName)
  {
    return "missing '" + attributeName + "' attribute in resource type";
  }

  /**
   * builds an exception for resource types
   *
   * @param message the error message
   * @return the exception
   */
  private ScimException getInvalidResourceException(String message)
  {
    return new InvalidResourceTypeException(message, null, HttpStatus.SC_INTERNAL_SERVER_ERROR, null);
  }

  /**
   * builds an exception for malformed requests
   *
   * @param message the error message
   * @return the exception
   */
  private ScimException getBadRequestException(String message)
  {
    return new BadRequestException(message, null, null);
  }

  /**
   * @return this object as json document
   */
  public JsonNode toJsonNode()
  {
    ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
    List<JsonNode> schemaNodes = schemas.stream().map(TextNode::new).collect(Collectors.toList());
    JsonHelper.addAttribute(objectNode, AttributeNames.SCHEMAS, new ArrayNode(JsonNodeFactory.instance, schemaNodes));
    JsonHelper.addAttribute(objectNode, AttributeNames.ID, new TextNode(id));
    JsonHelper.addAttribute(objectNode, AttributeNames.NAME, new TextNode(name));
    Optional.ofNullable(description)
            .ifPresent(s -> JsonHelper.addAttribute(objectNode, AttributeNames.DESCRIPTION, new TextNode(s)));
    JsonHelper.addAttribute(objectNode, AttributeNames.SCHEMA, new TextNode(schema));
    JsonHelper.addAttribute(objectNode, AttributeNames.ENDPOINT, new TextNode(endpoint));
    if (!schemaExtensions.isEmpty())
    {
      ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
      for ( SchemaExtension schemaExtension : schemaExtensions )
      {
        ObjectNode extensionNode = new ObjectNode(JsonNodeFactory.instance);
        JsonHelper.addAttribute(extensionNode, AttributeNames.SCHEMA, new TextNode(schemaExtension.getSchema()));
        JsonHelper.addAttribute(extensionNode,
                                AttributeNames.REQUIRED,
                                BooleanNode.valueOf(schemaExtension.isRequired()));
        arrayNode.add(extensionNode);
      }
      JsonHelper.addAttribute(objectNode, AttributeNames.SCHEMA_EXTENSIONS, arrayNode);
    }
    return objectNode;
  }

  /**
   * @return this object as json document
   */
  public String toString()
  {
    return toJsonNode().toString();
  }

  /**
   * a schema extension representation
   */
  @Data
  public class SchemaExtension
  {

    /**
     * the resource schema reference
     */
    private String schema;

    /**
     * if the extension is a required one or not
     */
    private boolean required;

    public SchemaExtension(JsonNode jsonNode)
    {
      this.schema = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.SCHEMA)
                              .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.SCHEMA)));
      this.required = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.REQUIRED, Boolean.class)
                                .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.REQUIRED)));
    }
  }

  /**
   * represents the schema descriptions of this resource type
   */
  @Data
  public class ResourceSchema
  {

    /**
     * this is the main schema that will describe the resource
     */
    private Schema metaSchema;

    /**
     * these are the schema extensions that describe the additional attributes of this resource type. This list
     * will only have those entries added to it that are added in the 'schemas'-attribute of the request
     */
    private List<Schema> extensions;

    public ResourceSchema(JsonNode resourceDocument)
    {
      List<String> schemas = JsonHelper.getSimpleAttributeArray(resourceDocument, AttributeNames.SCHEMAS)
                                       .orElseThrow(() -> getBadRequestException(missingAttrMessage(AttributeNames.SCHEMAS)));
      if (!schemas.contains(schema))
      {
        throw getBadRequestException("main resource schema '" + schema + "' is not present in resource");
      }

      Function<String, String> missingSchema = s -> "resource schema with uri '" + s + "' is not registered";
      this.metaSchema = Optional.ofNullable(schemaFactory.getResourceSchema(schema))
                                .orElseThrow(() -> getInvalidResourceException(missingSchema.apply(schema)));
      extensions = new ArrayList<>();
      schemas.remove(schema);
      for ( String schemaUri : schemas )
      {
        extensions.add(Optional.ofNullable(schemaFactory.getResourceSchema(schemaUri))
                               .orElseThrow(() -> getInvalidResourceException(missingSchema.apply(schemaUri))));
      }
      validateForRequiredExtensions(schemas);
    }

    /**
     * this method will verify that the document contains the required extensions and therefore applies to the
     * defined resource type
     */
    private void validateForRequiredExtensions(List<String> schemas)
    {
      List<SchemaExtension> requiredExtensions = schemaExtensions.stream()
                                                                 .filter(SchemaExtension::isRequired)
                                                                 .collect(Collectors.toList());
      for ( SchemaExtension requiredExtension : requiredExtensions )
      {
        if (!schemas.contains(requiredExtension.getSchema()))
        {
          throw new BadRequestException("the required extension '" + requiredExtension.getSchema() + "' is missing",
                                        null, ScimType.MISSING_EXTENSION);
        }
      }
    }
  }
}
