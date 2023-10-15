package de.captaingoldfish.scim.sdk.client.resources;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import lombok.Builder;


/**
 * Specifies the schema that describes a SCIM resource type
 */
public class ResourceType extends ResourceNode
{

  public ResourceType()
  {
    setSchemas(Arrays.asList(FieldNames.SCHEMA));
  }

  @Builder
  public ResourceType(String id,
                      Meta meta,
                      String name,
                      String description,
                      String endpoint,
                      String schema,
                      List<SchemaExtensions> schemaExtensions)
  {
    setSchemas(Arrays.asList(FieldNames.SCHEMA));
    setId(id);
    setMeta(meta);
    setName(name);
    setDescription(description);
    setEndpoint(endpoint);
    setSchema(schema);
    setSchemaExtensions(schemaExtensions);
  }

  /**
   * The resource type name. When applicable, service providers MUST specify the name, e.g., 'User'.
   */
  public String getName()
  {
    return getStringAttribute(FieldNames.NAME).get();

  }

  /**
   * The resource type name. When applicable, service providers MUST specify the name, e.g., 'User'.
   */
  public void setName(String name)
  {
    setAttribute(FieldNames.NAME, name);
  }

  /**
   * The resource type's human-readable description. When applicable, service providers MUST specify the
   * description.
   */
  public Optional<String> getDescription()
  {
    return getStringAttribute(FieldNames.DESCRIPTION);

  }

  /**
   * The resource type's human-readable description. When applicable, service providers MUST specify the
   * description.
   */
  public void setDescription(String description)
  {
    setAttribute(FieldNames.DESCRIPTION, description);
  }

  /**
   * The resource type's HTTP-addressable endpoint relative to the Base URL, e.g., '/Users'.
   */
  public String getEndpoint()
  {
    return getStringAttribute(FieldNames.ENDPOINT).get();

  }

  /**
   * The resource type's HTTP-addressable endpoint relative to the Base URL, e.g., '/Users'.
   */
  public void setEndpoint(String endpoint)
  {
    setAttribute(FieldNames.ENDPOINT, endpoint);
  }

  /**
   * The resource type's primary/base schema URI.
   */
  public String getSchema()
  {
    return getStringAttribute(AttributeNames.RFC7643.SCHEMA).get();

  }

  /**
   * The resource type's primary/base schema URI.
   */
  public void setSchema(String schema)
  {
    setAttribute(AttributeNames.RFC7643.SCHEMA, schema);
  }

  /**
   * A list of URIs of the resource type's schema extensions.
   */
  public List<SchemaExtensions> getSchemaExtensions()
  {
    return getArrayAttribute(FieldNames.SCHEMAEXTENSIONS, SchemaExtensions.class);
  }

  /**
   * A list of URIs of the resource type's schema extensions.
   */
  public void setSchemaExtensions(List<SchemaExtensions> schemaExtensions)
  {
    setAttribute(FieldNames.SCHEMAEXTENSIONS, schemaExtensions);
  }


  /**
   * A list of URIs of the resource type's schema extensions.
   */
  public static class SchemaExtensions extends ScimObjectNode
  {

    public SchemaExtensions()
    {}

    @Builder
    public SchemaExtensions(String schema, String required)
    {
      setSchema(schema);
      setRequired(required);
    }

    /**
     * The URI of a schema extension.
     */
    public String getSchema()
    {
      return getStringAttribute(FieldNames.SCHEMA).get();

    }

    /**
     * The URI of a schema extension.
     */
    public void setSchema(String schema)
    {
      setAttribute(FieldNames.SCHEMA, schema);
    }

    /**
     * A Boolean value that specifies whether or not the schema extension is required for the resource type. If
     * true, a resource of this type MUST include this schema extension and also include any attributes declared
     * as required in this schema extension. If false, a resource of this type MAY omit this schema extension.
     */
    public String getRequired()
    {
      return getStringAttribute(FieldNames.REQUIRED).get();

    }

    /**
     * A Boolean value that specifies whether or not the schema extension is required for the resource type. If
     * true, a resource of this type MUST include this schema extension and also include any attributes declared
     * as required in this schema extension. If false, a resource of this type MAY omit this schema extension.
     */
    public void setRequired(String required)
    {
      setAttribute(FieldNames.REQUIRED, required);
    }

  }

  /**
   * contains the attribute names of the resource representation
   */
  public static class FieldNames
  {

    public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:ResourceType";


    public static final String NAME = "name";

    public static final String DESCRIPTION = "description";

    public static final String ENDPOINT = "endpoint";

    public static final String SCHEMAEXTENSIONS = "schemaExtensions";

    public static final String REQUIRED = "required";

  }

  /**
   * override lombok builder with public constructor
   */
  public static class ResourceTypeBuilder
  {

    public ResourceTypeBuilder()
    {}
  }
}
