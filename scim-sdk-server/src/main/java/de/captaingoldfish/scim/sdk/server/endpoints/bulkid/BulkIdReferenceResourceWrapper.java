package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 21.08.2022 - 13:48 <br>
 * <br>
 * this wrapper class expects that the value node is present within an object-node and that this node is
 * simply replaceable
 */
class BulkIdReferenceResourceWrapper implements BulkIdReferenceWrapper
{

  /**
   * the parent node is always needed to replace the underlying value node since jacksons value nodes are
   * immutable
   */
  private final JsonNode parentNode;

  /**
   * the attribute definition of the valueNode that is needed to replace the valueNode within the
   * {@link #parentNode}
   */
  private final SchemaAttribute schemaAttribute;

  /**
   * the bulkId stored within the valueNode
   */
  @Getter
  private final String bulkId;

  public BulkIdReferenceResourceWrapper(JsonNode parentNode, JsonNode valueNode, SchemaAttribute schemaAttribute)
  {
    this.parentNode = parentNode;
    this.schemaAttribute = schemaAttribute;
    String bulkIdReference = valueNode.textValue();
    this.bulkId = bulkIdReference.replaceFirst(String.format("^%s:", AttributeNames.RFC7643.BULK_ID), "");
  }

  /**
   * will replace the valueNode with a new node that contains the new value
   *
   * @param newValue the new value to add into the parent node
   */
  public void replaceValueNode(String newValue)
  {
    JsonHelper.replaceNode(parentNode, schemaAttribute.getName(), new TextNode(newValue));
  }
}
