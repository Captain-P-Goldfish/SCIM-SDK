package de.gold.scim.schemas;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.endpoints.ResourceHandler;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.InternalServerException;
import de.gold.scim.exceptions.InvalidResourceTypeException;
import de.gold.scim.exceptions.ScimException;
import de.gold.scim.resources.ResourceNode;
import de.gold.scim.resources.base.ScimObjectNode;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.utils.JsonHelper;
import lombok.AccessLevel;
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
public class ResourceType extends ResourceNode
{

  /**
   * used for unit tests in order to prevent application context pollution
   */
  @Getter(AccessLevel.PROTECTED)
  private final SchemaFactory schemaFactory;

  /**
   * the resource handler implementation that is able to handle this kind of resource
   */
  @Getter(AccessLevel.PUBLIC)
  @Setter(AccessLevel.PROTECTED)
  private ResourceHandler resourceHandlerImpl;

  public ResourceType(String resourceDocument)
  {
    this(null, JsonHelper.readJsonDocument(resourceDocument));
  }

  protected ResourceType(SchemaFactory schemaFactory, String resourceDocument)
  {
    this(schemaFactory, JsonHelper.readJsonDocument(resourceDocument));
  }

  protected ResourceType(SchemaFactory schemaFactory, JsonNode resourceTypeDocument)
  {
    this.schemaFactory = getSchemaFactory(schemaFactory);
    setSchemas(JsonHelper.getSimpleAttributeArray(resourceTypeDocument, AttributeNames.SCHEMAS)
                         .orElse(Collections.singletonList(SchemaUris.RESOURCE_TYPE_URI)));
    setId(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.ID)
                    .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.ID))));
    setName(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.NAME)
                      .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.NAME))));
    setDescription(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.DESCRIPTION).orElse(null));
    setEndpoint(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.ENDPOINT)
                          .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.ENDPOINT))));
    setSchema(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.SCHEMA)
                        .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.SCHEMA))));
    List<SchemaExtension> schemaExtensions = new ArrayList<>();
    JsonHelper.getArrayAttribute(resourceTypeDocument, AttributeNames.SCHEMA_EXTENSIONS).ifPresent(jsonNodes -> {
      for ( JsonNode jsonNode : jsonNodes )
      {
        schemaExtensions.add(new SchemaExtension(jsonNode));
      }
    });
    setSchemaExtensions(schemaExtensions);
    Meta meta = getMetaNode(resourceTypeDocument);
    setMeta(meta);
  }

  /**
   * creates or gets the meta node and will extend it by the missing attributes
   *
   * @param resourceTypeDocument the resource type document from which this resource type is parsed. It may be
   *          that the developer already entered meta information within this document that should be preserved
   * @return the meta node
   */
  private Meta getMetaNode(JsonNode resourceTypeDocument)
  {
    Optional<ObjectNode> metaNode = JsonHelper.getObjectAttribute(resourceTypeDocument, AttributeNames.META);
    Meta meta;
    final String resourceType = "ResourceType";
    final String location = "/ResourceTypes/" + getName();
    if (metaNode.isPresent())
    {
      meta = JsonHelper.copyResourceToObject(metaNode.get(), Meta.class);
      meta = Meta.builder()
                 .resourceType(meta.getResourceType().orElse(resourceType))
                 .location(meta.getLocation().orElse(location))
                 .created(meta.getCreated()
                              .map(instant -> instant.atZone(ZoneId.systemDefault()).toLocalDateTime())
                              .orElse(LocalDateTime.now()))
                 .lastModified(meta.getLastModified()
                                   .map(instant -> instant.atZone(ZoneId.systemDefault()).toLocalDateTime())
                                   .orElse(LocalDateTime.now()))
                 .build();
    }
    else
    {
      meta = Meta.builder()
                 .resourceType(resourceType)
                 .location(location)
                 .created(LocalDateTime.now())
                 .lastModified(LocalDateTime.now())
                 .build();
    }
    return meta;
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
   * @return the required resource schema extensions that represents this resource type
   */
  public List<Schema> getRequiredResourceSchemaExtensions()
  {
    return getSchemaExtensions().stream()
                                .filter(SchemaExtension::isRequired)
                                .map(SchemaExtension::getSchema)
                                .map(getSchemaFactory()::getResourceSchema)
                                .collect(Collectors.toList());
  }

  /**
   * @return the not required resource schema extensions that represents this resource type
   */
  public List<Schema> getNotRequiredResourceSchemaExtensions()
  {
    return getSchemaExtensions().stream()
                                .filter(schemaExtension -> !schemaExtension.isRequired())
                                .map(SchemaExtension::getSchema)
                                .map(getSchemaFactory()::getResourceSchema)
                                .collect(Collectors.toList());
  }

  /**
   * this method will extract all {@link Schema} definitions that belong to this resource type. The first entry
   * in the list will always be the main {@link Schema} referenced in the attribute {@link #getSchema()}. All
   * other {@link Schema}s in the list will be the extensions of this resource
   *
   * @return a list of all {@link Schema} definitions that describe this resource type
   */
  public List<Schema> getAllSchemas()
  {
    List<Schema> schemaList = new ArrayList<>();
    schemaList.add(schemaFactory.getResourceSchema(getSchema()));
    getSchemaExtensions().forEach(schemaExtension -> {
      schemaList.add(schemaFactory.getResourceSchema(schemaExtension.getSchema()));
    });
    schemaList.add(schemaFactory.getMetaSchema(SchemaUris.META));
    return schemaList;
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
   * {@inheritDoc}
   */
  @Override
  public void setId(String id)
  {
    if (getId().isPresent())
    {
      throw new InternalServerException("the id attribute is immutable", null, null);
    }
    super.setId(id);
  }

  /**
   * The resource type's HTTP-addressable endpoint relative to the Base URL of the service provider, e.g.,
   * "Users". REQUIRED.
   */
  public String getEndpoint()
  {
    return getStringAttribute(AttributeNames.ENDPOINT).orElseThrow(() -> {
      return new InvalidResourceTypeException("the endpoint is a required attribute", null, null, null);
    });
  }

  /**
   * The resource type's HTTP-addressable endpoint relative to the Base URL of the service provider, e.g.,
   * "Users". REQUIRED.
   */
  private void setEndpoint(String endpoint)
  {
    setAttribute(AttributeNames.ENDPOINT, Objects.requireNonNull(StringUtils.stripToNull(endpoint)));
  }

  /**
   * The resource type name. When applicable, service providers MUST specify the name, e.g., "User" or "Group".
   * This name is referenced by the "meta.resourceType" attribute in all resources. REQUIRED.
   */
  public String getName()
  {
    return getStringAttribute(AttributeNames.NAME).orElseThrow(() -> {
      return new InvalidResourceTypeException("the name is a required attribute", null, null, null);
    });
  }

  /**
   * The resource type name. When applicable, service providers MUST specify the name, e.g., "User" or "Group".
   * This name is referenced by the "meta.resourceType" attribute in all resources. REQUIRED.
   */
  private void setName(String name)
  {
    setAttribute(AttributeNames.NAME, name);
  }

  /**
   * The resource type's human-readable description. When applicable, service providers MUST specify the
   * description. OPTIONAL.
   */
  public Optional<String> getDescription()
  {
    return getStringAttribute(AttributeNames.DESCRIPTION);
  }

  /**
   * The resource type's human-readable description. When applicable, service providers MUST specify the
   * description. OPTIONAL.
   */
  private void setDescription(String description)
  {
    setAttribute(AttributeNames.DESCRIPTION, description);
  }

  /**
   * The resource type's human-readable description. When applicable, service providers MUST specify the
   * description. OPTIONAL.
   */
  private void setResourceTypeDescription(String description)
  {
    setAttribute(AttributeNames.DESCRIPTION, description);
  }

  /**
   * The resource type's primary/base schema URI, e.g., "urn:ietf:params:scim:schemas:core:2.0:User". This MUST
   * be equal to the "id" attribute of the associated "Schema" resource. REQUIRED.
   */
  public String getSchema()
  {
    return getStringAttribute(AttributeNames.SCHEMA).orElseThrow(() -> {
      return new InvalidResourceTypeException("the schema is a required attribute", null, null, null);
    });
  }

  /**
   * The resource type's primary/base schema URI, e.g., "urn:ietf:params:scim:schemas:core:2.0:User". This MUST
   * be equal to the "id" attribute of the associated "Schema" resource. REQUIRED.
   */
  private void setSchema(String schema)
  {
    setAttribute(AttributeNames.SCHEMA, schema);
  }

  /**
   * A list of URIs of the resource type's schema extensions. OPTIONAL
   */
  public List<SchemaExtension> getSchemaExtensions()
  {
    return getArrayAttribute(AttributeNames.SCHEMA_EXTENSIONS, SchemaExtension.class);
  }

  /**
   * A list of URIs of the resource type's schema extensions. OPTIONAL
   */
  private void setSchemaExtensions(List<SchemaExtension> schemaExtensions)
  {
    setAttribute(AttributeNames.SCHEMA_EXTENSIONS, schemaExtensions);
  }

  /**
   * a schema extension representation
   */
  @Getter
  public class SchemaExtension extends ScimObjectNode
  {

    public SchemaExtension(JsonNode jsonNode)
    {
      super(null);
      setSchema(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.SCHEMA)
                          .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.SCHEMA))));
      setRequired(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.REQUIRED, Boolean.class)
                            .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.REQUIRED))));
    }

    /**
     * The URI of an extended schema, e.g., "urn:edu:2.0:Staff". This MUST be equal to the "id" attribute of a
     * "Schema" resource. REQUIRED.
     */
    public String getSchema()
    {
      return getStringAttribute(AttributeNames.SCHEMA).orElseThrow(() -> {
        return new InvalidResourceTypeException("the schema attribute is a required attribute", null, null, null);
      });
    }

    /**
     * The URI of an extended schema, e.g., "urn:edu:2.0:Staff". This MUST be equal to the "id" attribute of a
     * "Schema" resource. REQUIRED.
     */
    private void setSchema(String schema)
    {
      setAttribute(AttributeNames.SCHEMA, schema);
    }


    /**
     * A Boolean value that specifies whether or not the schema extension is required for the resource type. If
     * true, a resource of this type MUST include this schema extension and also include any attributes declared
     * as required in this schema extension. If false, a resource of this type MAY omit this schema extension.
     * REQUIRED.
     */
    public boolean isRequired()
    {
      return getBooleanAttribute(AttributeNames.REQUIRED).orElseThrow(() -> {
        return new InvalidResourceTypeException("the required attribute is a required attribute", null, null, null);
      });
    }

    /**
     * A Boolean value that specifies whether or not the schema extension is required for the resource type. If
     * true, a resource of this type MUST include this schema extension and also include any attributes declared
     * as required in this schema extension. If false, a resource of this type MAY omit this schema extension.
     * REQUIRED.
     */
    private void setRequired(boolean required)
    {
      setAttribute(AttributeNames.REQUIRED, required);
    }
  }

  /**
   * represents the schema descriptions of this resource type
   */
  @Getter
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
      if (!schemas.contains(getSchema()))
      {
        throw getBadRequestException("main resource schema '" + getSchema() + "' is not present in resource");
      }

      Function<String, String> missingSchema = s -> "resource schema with uri '" + s + "' is not registered";
      this.metaSchema = Optional.ofNullable(schemaFactory.getResourceSchema(getSchema()))
                                .orElseThrow(() -> getInvalidResourceException(missingSchema.apply(getSchema())));
      extensions = new ArrayList<>();
      schemas.remove(getSchema());
      for ( String schemaUri : schemas )
      {
        extensions.add(Optional.ofNullable(schemaFactory.getResourceSchema(schemaUri))
                               .orElseThrow(() -> getInvalidResourceException(missingSchema.apply(schemaUri))));
      }
    }
  }
}
