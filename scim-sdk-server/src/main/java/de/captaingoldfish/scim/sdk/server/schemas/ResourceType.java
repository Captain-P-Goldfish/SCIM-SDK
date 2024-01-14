package de.captaingoldfish.scim.sdk.server.schemas;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidConfigException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidResourceTypeException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
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
  @Setter(AccessLevel.PUBLIC)
  private ResourceHandler resourceHandlerImpl;

  public ResourceType()
  {
    this.schemaFactory = null;
  }

  protected ResourceType(SchemaFactory schemaFactory, String resourceDocument)
  {
    this(schemaFactory, JsonHelper.readJsonDocument(resourceDocument));
  }

  protected ResourceType(SchemaFactory schemaFactory, JsonNode resourceTypeDocument)
  {
    this.schemaFactory = Objects.requireNonNull(schemaFactory);
    setSchemas(JsonHelper.getSimpleAttributeArray(resourceTypeDocument, AttributeNames.RFC7643.SCHEMAS)
                         .orElse(Collections.singletonList(SchemaUris.RESOURCE_TYPE_URI)));
    setId(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.RFC7643.ID)
                    .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.RFC7643.ID))));
    setName(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.RFC7643.NAME)
                      .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.RFC7643.NAME))));
    setDescription(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.RFC7643.DESCRIPTION)
                             .orElse(null));
    setEndpoint(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.RFC7643.ENDPOINT)
                          .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.RFC7643.ENDPOINT))));
    setSchema(JsonHelper.getSimpleAttribute(resourceTypeDocument, AttributeNames.RFC7643.SCHEMA)
                        .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.RFC7643.SCHEMA))));
    List<SchemaExtension> schemaExtensions = new ArrayList<>();
    JsonHelper.getArrayAttribute(resourceTypeDocument, AttributeNames.RFC7643.SCHEMA_EXTENSIONS)
              .ifPresent(jsonNodes -> {
                for ( JsonNode jsonNode : jsonNodes )
                {
                  schemaExtensions.add(new SchemaExtension(jsonNode));
                }
              });
    setSchemaExtensions(schemaExtensions);
    Meta meta = getMetaNode(resourceTypeDocument);
    setMeta(meta);
    JsonNode featureNode = resourceTypeDocument.get(SchemaUris.RESOURCE_TYPE_FEATURE_EXTENSION_URI);
    ResourceTypeFeatures resourceTypeFeatures = JsonHelper.copyResourceToObject(featureNode,
                                                                                ResourceTypeFeatures.class);
    setFeatures(resourceTypeFeatures);
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
    Optional<ObjectNode> metaNode = JsonHelper.getObjectAttribute(resourceTypeDocument, AttributeNames.RFC7643.META);
    Meta meta;
    final String resourceType = "ResourceType";
    if (metaNode.isPresent())
    {
      meta = JsonHelper.copyResourceToObject(metaNode.get(), Meta.class);
      meta = Meta.builder()
                 .resourceType(meta.getResourceType().orElse(resourceType))
                 .location(meta.getLocation().orElse(null))
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
                 .created(LocalDateTime.now())
                 .lastModified(LocalDateTime.now())
                 .build();
    }
    return meta;
  }

  /**
   * @return the main schema that represents this resource type
   */
  public Schema getMainSchema()
  {
    return schemaFactory.getResourceSchema(getSchema());
  }

  /**
   * @return the resource schema extensions that represents this resource type
   */
  public List<Schema> getAllSchemaExtensions()
  {
    return getAllSchemaExtensionsStream().collect(Collectors.toList());
  }

  /**
   * @return the resource schema extensions that represents this resource type
   */
  public Stream<Schema> getAllSchemaExtensionsStream()
  {
    return getSchemaExtensions().stream().map(SchemaExtension::getSchema).map(schemaFactory::getResourceSchema);
  }

  /**
   * @return the required resource schema extensions that represents this resource type
   */
  public List<Schema> getRequiredResourceSchemaExtensions()
  {
    return getSchemaExtensions().stream()
                                .filter(SchemaExtension::isRequired)
                                .map(SchemaExtension::getSchema)
                                .map(schemaFactory::getResourceSchema)
                                .collect(Collectors.toList());
  }

  /**
   * a delegation method to retrieve the schema that represents the meta-attribute. This attribute has received
   * its own schema under the URI "urn:ietf:params:scim:schemas:core:2.0:Meta"
   *
   * @return the meta schema definition
   */
  public Schema getMetaSchema()
  {
    return schemaFactory.getMetaSchema(SchemaUris.META);
  }

  /**
   * tries to find a scim node of this resource type in the registered schemas. This method may return the wrong
   * attribute if there are colliding attribute names among the main-schema and an extension or among two
   * extensions.
   *
   * @param scimNodeName the scim node name of the attribute that should be extracted
   * @return the schema attribute if it does exist or an empty.
   */
  public Optional<SchemaAttribute> getSchemaAttribute(String scimNodeName)
  {
    {
      SchemaAttribute schemaAttribute = getMainSchema().getSchemaAttribute(scimNodeName);
      if (schemaAttribute != null)
      {
        return Optional.of(schemaAttribute);
      }
    }
    for ( Schema extension : getAllSchemaExtensions() )
    {
      SchemaAttribute schemaAttribute = extension.getSchemaAttribute(scimNodeName);
      if (schemaAttribute != null)
      {
        return Optional.of(schemaAttribute);
      }
    }
    return Optional.empty();
  }

  /**
   * checks if the schema with the given uri is a required extension
   *
   * @param schemaUri the extension of the schema
   * @return true if the given extension is required, false else
   */
  private boolean isExtensionRequired(String schemaUri)
  {
    return getSchemaExtensions().stream()
                                .filter(schemaExtension -> schemaExtension.getSchema().equals(schemaUri))
                                .anyMatch(SchemaExtension::isRequired);
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
    Schema resourceSchema = schemaFactory.getResourceSchema(getSchema());
    if (resourceSchema == null)
    {
      String error = String.format("Noticed a mismatch of referenced schema in resource-type definition and actual "
                                   + "registration. SchemaId in resource-type definition '%s' was not found in "
                                   + "schema factory. Registered schemas are: %s",
                                   getSchema(),
                                   schemaFactory.getResourceSchemas().keySet());
      throw new InvalidConfigException(error);
    }
    schemaList.add(resourceSchema);
    getSchemaExtensions().forEach(schemaExtension -> {
      schemaList.add(schemaFactory.getResourceSchema(schemaExtension.getSchema()));
    });
    schemaList.add(schemaFactory.getMetaSchema(SchemaUris.META));
    return schemaList;
  }

  /**
   * gets a member schema of this resource type by its uri
   *
   * @param schemaUri the uri of the schema that is wanted
   * @return the schema if it does exist and is a member of this resource type
   * @throws BadRequestException if the schemaUri is not a member of this resource type
   */
  public final Schema getSchemaByUri(String schemaUri)
  {
    boolean doesUriBelongToResourceType = schemaUri.equals(getSchema())
                                          || getSchemaExtensions().stream()
                                                                  .anyMatch(extension -> extension.getSchema()
                                                                                                  .equals(schemaUri));
    if (!doesUriBelongToResourceType)
    {
      throw new BadRequestException(String.format("Schema URI '%s' is not part of resource type '%s'",
                                                  schemaUri,
                                                  getName()));
    }
    return schemaFactory.getResourceSchema(schemaUri);
  }

  /**
   * builds an error message in case of a required missing attribute
   *
   * @param attributeName the name of the attribute that is missing
   * @return the error message
   */
  private String missingAttrMessage(String attributeName)
  {
    return "missing '" + attributeName + "' attribute in resource type with name '" + getName() + "'";
  }

  /**
   * builds an exception for resource types
   *
   * @param message the error message
   * @return the exception
   */
  private ScimException getInvalidResourceException(String message)
  {
    return new InvalidResourceTypeException(message, null, HttpStatus.INTERNAL_SERVER_ERROR, null);
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
    return getStringAttribute(AttributeNames.RFC7643.ENDPOINT).orElseThrow(() -> {
      return new InvalidResourceTypeException("the endpoint is a required attribute", null, null, null);
    });
  }

  /**
   * The resource type's HTTP-addressable endpoint relative to the Base URL of the service provider, e.g.,
   * "Users". REQUIRED.
   */
  private void setEndpoint(String endpoint)
  {
    setAttribute(AttributeNames.RFC7643.ENDPOINT, Objects.requireNonNull(StringUtils.stripToNull(endpoint)));
  }

  /**
   * The resource type name. When applicable, service providers MUST specify the name, e.g., "User" or "Group".
   * This name is referenced by the "meta.resourceType" attribute in all resources. REQUIRED.
   */
  public String getName()
  {
    return getStringAttribute(AttributeNames.RFC7643.NAME).orElseThrow(() -> {
      return new InvalidResourceTypeException("the name is a required attribute", null, null, null);
    });
  }

  /**
   * The resource type name. When applicable, service providers MUST specify the name, e.g., "User" or "Group".
   * This name is referenced by the "meta.resourceType" attribute in all resources. REQUIRED.
   */
  private void setName(String name)
  {
    setAttribute(AttributeNames.RFC7643.NAME, name);
  }

  /**
   * The resource type's human-readable description. When applicable, service providers MUST specify the
   * description. OPTIONAL.
   */
  public Optional<String> getDescription()
  {
    return getStringAttribute(AttributeNames.RFC7643.DESCRIPTION);
  }

  /**
   * The resource type's human-readable description. When applicable, service providers MUST specify the
   * description. OPTIONAL.
   */
  public void setDescription(String description)
  {
    setAttribute(AttributeNames.RFC7643.DESCRIPTION, description);
  }

  /**
   * The resource type's primary/base schema URI, e.g., "urn:ietf:params:scim:schemas:core:2.0:User". This MUST
   * be equal to the "id" attribute of the associated "Schema" resource. REQUIRED.
   */
  public String getSchema()
  {
    return getStringAttribute(AttributeNames.RFC7643.SCHEMA).orElseThrow(() -> {
      return new InvalidResourceTypeException("the schema is a required attribute", null, null, null);
    });
  }

  /**
   * The resource type's primary/base schema URI, e.g., "urn:ietf:params:scim:schemas:core:2.0:User". This MUST
   * be equal to the "id" attribute of the associated "Schema" resource. REQUIRED.
   */
  private void setSchema(String schema)
  {
    setAttribute(AttributeNames.RFC7643.SCHEMA, schema);
  }

  /**
   * A list of URIs of the resource type's schema extensions. OPTIONAL
   */
  public List<SchemaExtension> getSchemaExtensions()
  {
    return getArrayAttribute(AttributeNames.RFC7643.SCHEMA_EXTENSIONS, SchemaExtension.class);
  }

  /**
   * A list of URIs of the resource type's schema extensions. OPTIONAL
   */
  private void setSchemaExtensions(List<SchemaExtension> schemaExtensions)
  {
    setAttribute(AttributeNames.RFC7643.SCHEMA_EXTENSIONS, schemaExtensions);
  }

  /**
   * @see ResourceTypeFeatures
   */
  public ResourceTypeFeatures getFeatures()
  {
    ResourceTypeFeatures filterExtension = getObjectAttribute(SchemaUris.RESOURCE_TYPE_FEATURE_EXTENSION_URI,
                                                              ResourceTypeFeatures.class).orElse(null);
    if (filterExtension == null)
    {
      filterExtension = ResourceTypeFeatures.builder().autoFiltering(false).singletonEndpoint(false).build();
      setFeatures(filterExtension);
    }
    return filterExtension;
  }

  /**
   * @see ResourceTypeFeatures
   */
  public void setFeatures(ResourceTypeFeatures filterExtension)
  {
    setAttribute(SchemaUris.RESOURCE_TYPE_FEATURE_EXTENSION_URI, filterExtension);
    if (filterExtension == null)
    {
      removeSchema(SchemaUris.RESOURCE_TYPE_FEATURE_EXTENSION_URI);
    }
    else
    {
      addSchema(SchemaUris.RESOURCE_TYPE_FEATURE_EXTENSION_URI);
    }
  }

  /**
   * @return true if this resource type was disabled, false else
   */
  public boolean isDisabled()
  {
    return getFeatures().isResourceTypeDisabled();
  }

  /**
   * disables or enables this resourcetype
   */
  public void setDisabled(Boolean disabled)
  {
    getFeatures().setResourceTypeDisabled(disabled);
  }

  /**
   * tries to extract the extension with the given id from the resourceType if it is part of this resourceType
   */
  public Optional<Schema> getExtensionById(String extensionId)
  {
    return getAllSchemaExtensionsStream().filter(schema -> schema.getNonNullId().equals(extensionId)).findAny();
  }

  /**
   * a schema extension representation
   */
  public class SchemaExtension extends ScimObjectNode
  {

    public SchemaExtension(JsonNode jsonNode)
    {
      super(null);
      setSchema(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.SCHEMA)
                          .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.RFC7643.SCHEMA))));
      setRequired(JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.RFC7643.REQUIRED, Boolean.class)
                            .orElseThrow(() -> getInvalidResourceException(missingAttrMessage(AttributeNames.RFC7643.REQUIRED))));
    }

    /**
     * The URI of an extended schema, e.g., "urn:edu:2.0:Staff". This MUST be equal to the "id" attribute of a
     * "Schema" resource. REQUIRED.
     */
    public String getSchema()
    {
      return getStringAttribute(AttributeNames.RFC7643.SCHEMA).orElseThrow(() -> {
        return new InvalidResourceTypeException("the schema attribute is a required attribute", null, null, null);
      });
    }

    /**
     * The URI of an extended schema, e.g., "urn:edu:2.0:Staff". This MUST be equal to the "id" attribute of a
     * "Schema" resource. REQUIRED.
     */
    public void setSchema(String schema)
    {
      setAttribute(AttributeNames.RFC7643.SCHEMA, schema);
    }


    /**
     * A Boolean value that specifies whether or not the schema extension is required for the resource type. If
     * true, a resource of this type MUST include this schema extension and also include any attributes declared
     * as required in this schema extension. If false, a resource of this type MAY omit this schema extension.
     * REQUIRED.
     */
    public boolean isRequired()
    {
      return getBooleanAttribute(AttributeNames.RFC7643.REQUIRED).orElseThrow(() -> {
        return new InvalidResourceTypeException("the required attribute is a required attribute", null, null, null);
      });
    }

    /**
     * A Boolean value that specifies whether the schema extension is required for the resource type. If true, a
     * resource of this type MUST include this schema extension and also include any attributes declared as
     * required in this schema extension. If false, a resource of this type MAY omit this schema extension.
     * REQUIRED.
     */
    public void setRequired(boolean required)
    {
      setAttribute(AttributeNames.RFC7643.REQUIRED, required);
    }
  }
}
