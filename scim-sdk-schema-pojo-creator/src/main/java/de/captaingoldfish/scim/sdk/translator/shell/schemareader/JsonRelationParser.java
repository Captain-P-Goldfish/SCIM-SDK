package de.captaingoldfish.scim.sdk.translator.shell.schemareader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.validation.MetaSchemaValidator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 28.01.2022
 */
@Slf4j
public class JsonRelationParser
{

  private static final Schema RESOURCE_SCHEMA_SCHEMA = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));

  private static final Schema RESOURCE_TYPE_SCHEMA = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON));

  private final List<FileInfoWrapper> fileInfoWrapperList;

  @Getter
  private final List<FileInfoWrapper> resourceTypes;

  @Getter
  private final List<FileInfoWrapper> resourceSchemas;

  public JsonRelationParser(List<FileInfoWrapper> fileInfoWrapperList)
  {
    this.fileInfoWrapperList = fileInfoWrapperList;
    this.resourceTypes = extractResourceTypes();
    this.resourceSchemas = extractResourceSchemas();
  }

  private List<FileInfoWrapper> extractResourceTypes()
  {
    List<FileInfoWrapper> tmpResourceSchemas = fileInfoWrapperList.stream().filter(fileInfoWrapper -> {
      JsonNode jsonNode = fileInfoWrapper.getJsonNode();
      SchemaType schemaType = SchemaType.getSchemaType(jsonNode);
      return SchemaType.RESOURCE_TYPE.equals(schemaType);
    }).collect(Collectors.toList());

    return validateResourceTypes(tmpResourceSchemas);
  }

  private List<FileInfoWrapper> extractResourceSchemas()
  {
    List<FileInfoWrapper> tmpResourceTypes = fileInfoWrapperList.stream().filter(fileInfoWrapper -> {
      JsonNode jsonNode = fileInfoWrapper.getJsonNode();
      SchemaType schemaType = SchemaType.getSchemaType(jsonNode);
      return SchemaType.RESOURCE_SCHEMA.equals(schemaType);
    }).collect(Collectors.toList());

    return validateResourceSchemas(tmpResourceTypes);
  }

  public List<SchemaRelation> getSchemaRelations()
  {
    List<SchemaRelation> schemaRelationWrapperList = new ArrayList<>();
    for ( FileInfoWrapper resourceTypeWrapper : resourceTypes )
    {
      String resourceSchemaUri = resourceTypeWrapper.getJsonNode().get(AttributeNames.RFC7643.SCHEMA).textValue();
      ArrayNode schemaExtensions = (ArrayNode)resourceTypeWrapper.getJsonNode()
                                                                 .get(AttributeNames.RFC7643.SCHEMA_EXTENSIONS);
      List<String> extensionUris = new ArrayList<>();
      if (schemaExtensions != null && !schemaExtensions.isNull())
      {
        for ( JsonNode schemaExtension : schemaExtensions )
        {
          if (schemaExtension instanceof ObjectNode)
          {
            String extensionSchemaUri = schemaExtension.get(AttributeNames.RFC7643.SCHEMA).textValue();
            extensionUris.add(extensionSchemaUri);
          }
        }
      }
      JsonNode resourceSchema = getResourceSchemaByUri(resourceSchemaUri).orElse(null);
      List<Schema> extensionNodes = extensionUris.stream().map(resourceSchemaUri1 -> {
        return getResourceSchemaByUri(resourceSchemaUri1).orElse(null);
      }).filter(Objects::nonNull).map(Schema::new).collect(Collectors.toList());
      SchemaRelation schemaRelationWrapper = new SchemaRelation(resourceTypeWrapper.getJsonNode(),
                                                                  new Schema(resourceSchema), extensionNodes);
      schemaRelationWrapperList.add(schemaRelationWrapper);
    }
    return schemaRelationWrapperList;
  }

  private Optional<JsonNode> getResourceSchemaByUri(String resourceSchemaUri)
  {
    return resourceSchemas.stream()
                          .map(FileInfoWrapper::getJsonNode)
                          .filter(jsonNode -> jsonNode.get(AttributeNames.RFC7643.ID)
                                                      .textValue()
                                                      .equals(resourceSchemaUri))
                          .findAny();
  }

  private List<FileInfoWrapper> validateResourceSchemas(List<FileInfoWrapper> tmpResourceSchemas)
  {
    return tmpResourceSchemas.stream().filter(fileInfoWrapper -> {
      try
      {
        MetaSchemaValidator.getInstance().validateDocument(RESOURCE_SCHEMA_SCHEMA, fileInfoWrapper.getJsonNode());
        return true;
      }
      catch (Exception ex)
      {
        log.debug(ex.getMessage(), ex);
        log.info("Document '{}' is not a valid resource schema and will be ignored: {}",
                 fileInfoWrapper.getResourceFile().getAbsolutePath(),
                 ex.getMessage());
        return false;
      }
    }).collect(Collectors.toList());
  }

  private List<FileInfoWrapper> validateResourceTypes(List<FileInfoWrapper> tmpResourceTypes)
  {
    return tmpResourceTypes.stream().filter(fileInfoWrapper -> {
      try
      {
        MetaSchemaValidator.getInstance().validateDocument(RESOURCE_TYPE_SCHEMA, fileInfoWrapper.getJsonNode());
        return true;
      }
      catch (Exception ex)
      {
        log.debug(ex.getMessage(), ex);
        log.info("Document '{}' is not a valid resource type and will be ignored: {}",
                 fileInfoWrapper.getResourceFile().getAbsolutePath(),
                 ex.getMessage());
        return false;
      }
    }).collect(Collectors.toList());
  }

  public static enum SchemaType
  {

    RESOURCE_TYPE, RESOURCE_SCHEMA, UNPARSEABLE;

    public static SchemaType getSchemaType(JsonNode jsonNode)
    {
      ArrayNode schemasNode = (ArrayNode)jsonNode.get(AttributeNames.RFC7643.SCHEMAS);
      if (schemasNode == null || schemasNode.isNull() || schemasNode.isEmpty())
      {
        return UNPARSEABLE;
      }
      for ( JsonNode node : schemasNode )
      {
        if (node instanceof TextNode)
        {
          TextNode schema = (TextNode)node;
          switch (schema.textValue())
          {
            case "urn:ietf:params:scim:schemas:core:2.0:Schema":
              return RESOURCE_SCHEMA;
            case "urn:ietf:params:scim:schemas:core:2.0:ResourceType":
              return RESOURCE_TYPE;
          }
        }
      }
      return UNPARSEABLE;
    }
  }
}
