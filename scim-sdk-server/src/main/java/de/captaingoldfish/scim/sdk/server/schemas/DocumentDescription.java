package de.captaingoldfish.scim.sdk.server.schemas;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * will describe a request document by identifying the present extensions and setting the schemas-attribute
 * accordingly
 *
 * @author Pascal Knueppel
 * @since 18.04.2021
 */
@Slf4j
@Getter
public class DocumentDescription
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

  public DocumentDescription(ResourceType resourceType, JsonNode jsonNode)
  {
    if (!jsonNode.isObject())
    {
      String errorMessage = String.format("The received resource document is not an object '%s'", jsonNode);
      throw new DocumentValidationException(errorMessage, HttpStatus.BAD_REQUEST, null);
    }

    this.extensions = new ArrayList<>();

    ObjectNode resourceDocument = (ObjectNode)jsonNode;
    for ( ResourceType.SchemaExtension schemaExtension : resourceType.getSchemaExtensions() )
    {
      addPresentOrRemoveNonePresentExtensions(resourceType, resourceDocument, schemaExtension);
    }
    ArrayNode schemasNode = new ArrayNode(JsonNodeFactory.instance);
    final String mainSchemaUri = resourceType.getSchema();
    schemasNode.add(mainSchemaUri);
    extensions.stream().map(Schema::getNonNullId).forEach(schemasNode::add);
    resourceDocument.set(AttributeNames.RFC7643.SCHEMAS, schemasNode);

    this.metaSchema = resourceType.getMainSchema();
    log.trace("Determined main schema as '{}'", metaSchema.getNonNullId());
    log.trace("Determined present extensions '{}'", extensions);
  }

  /**
   * checks if an extension is present in the document and adds the schema to the list of present extensions. If
   * an extension is not set or is a null-node or an empty object the extension will be removed from the
   * document.
   *
   * @param resourceDocument the sent resource document
   * @param schemaExtension the schema extension attribute from this resource type definition
   */
  private void addPresentOrRemoveNonePresentExtensions(ResourceType resourceType,
                                                       ObjectNode resourceDocument,
                                                       ResourceType.SchemaExtension schemaExtension)
  {
    JsonNode extensionNode = resourceDocument.get(schemaExtension.getSchema());
    boolean isExtensionPresent = extensionNode != null && !extensionNode.isNull() && !extensionNode.isEmpty();
    if (isExtensionPresent)
    {
      Schema extensionSchema = resourceType.getSchemaFactory().getResourceSchema(schemaExtension.getSchema());
      this.extensions.add(extensionSchema);
    }
    else
    {
      resourceDocument.remove(schemaExtension.getSchema());
    }
  }
}
