package de.captaingoldfish.scim.sdk.common.request;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 08:32 <br>
 * <br>
 * represents a single operation within a patch request
 */
@NoArgsConstructor
public class PatchRequestOperation extends ScimObjectNode
{

  /**
   * if the value-attribute was extracted once. This will prevent repeated parsing of specific node-types
   */
  private boolean valueExtracted = false;

  @Builder
  public PatchRequestOperation(String path, PatchOp op, List<String> values, JsonNode valueNode)
  {
    this();
    setOp(op);
    if (values != null && !values.isEmpty())
    {
      setValues(values);
    }
    else if (valueNode != null)
    {
      setValueNode(valueNode);
    }
    setPath(path);
  }

  /**
   * The "path" attribute value is a String containing an attribute path describing the target of the
   * operation.The "path" attribute is OPTIONAL for "add" and "replace" and is REQUIRED for "remove" operations.
   */
  public Optional<String> getPath()
  {
    return getStringAttribute(AttributeNames.RFC7643.PATH);
  }

  /**
   * The "path" attribute value is a String containing an attribute path describing the target of the
   * operation.The "path" attribute is OPTIONAL for "add" and "replace" and is REQUIRED for "remove" operations.
   */
  public void setPath(String path)
  {
    setAttribute(AttributeNames.RFC7643.PATH, path);
  }

  /**
   * Each PATCH operation object MUST have exactly one "op" member, whose value indicates the operation to
   * perform and MAY be one of "add", "remove", or "replace" <br>
   * (This will never return null on server side for schema validation is executed before this method is called)
   */
  public PatchOp getOp()
  {
    return getStringAttribute(AttributeNames.RFC7643.OP).map(PatchOp::getByValue).orElse(null);
  }

  /**
   * Each PATCH operation object MUST have exactly one "op" member, whose value indicates the operation to
   * perform and MAY be one of "add", "remove", or "replace"
   */
  public void setOp(PatchOp patchOp)
  {
    setAttribute(AttributeNames.RFC7643.OP, patchOp == null ? null : patchOp.getValue());
  }

  /**
   * the new value of the targeted attribute
   */
  public Optional<JsonNode> getValue()
  {
    JsonNode valueNode = get(AttributeNames.RFC7643.VALUE);
    if (valueNode == null || valueNode.isNull())
    {
      return Optional.empty();
    }
    if (valueExtracted)
    {
      return Optional.of(valueNode);
    }
    valueNode = materializeStructuredValue(valueNode);
    if (valueNode.isObject() || valueNode.isArray())
    {
      valueExtracted = true;
      return Optional.of(valueNode);
    }
    valueExtracted = true;
    return Optional.ofNullable(valueNode);
  }

  /**
   * sets a single scalar value for the targeted attribute. The value is stored verbatim as a scalar string.
   * {@link #getValues()} and {@link #getValueNode()} present it as a single-element array during processing.
   */
  public void setValue(String value)
  {
    valueExtracted = false;
    setAttribute(AttributeNames.RFC7643.VALUE, value);
  }

  /**
   * the new value of the targeted attribute
   */
  public void setValue(JsonNode value)
  {
    valueExtracted = false;
    set(AttributeNames.RFC7643.VALUE, value);
  }

  /**
   * the new value of the targeted attribute <br>
   * (This will never return null on server side for schema validation is executed before this method is called)
   */
  public List<String> getValues()
  {
    return getSimpleArrayAttribute(AttributeNames.RFC7643.VALUE);
  }

  /**
   * sets the values for the targeted attribute. The values are stored as a JSON array, so a singleton list
   * serializes as {@code "value":["..."]} rather than a scalar. For a single scalar value use
   * {@link #setValue(String)} instead.
   */
  public void setValues(List<String> value)
  {
    valueExtracted = false;
    if (value == null || value.isEmpty())
    {
      remove(AttributeNames.RFC7643.VALUE);
    }
    else
    {
      setAttributeList(AttributeNames.RFC7643.VALUE, value);
    }
  }

  /**
   * reads the value-attribute and, if necessary, returns it embedded within an {@link ArrayNode}. The stored
   * value itself is not modified, so its JSON type is preserved for serialization.
   */
  public Optional<ArrayNode> getValueNode()
  {
    JsonNode jsonNode = get(AttributeNames.RFC7643.VALUE);
    if (jsonNode == null || jsonNode.isNull())
    {
      return Optional.empty();
    }
    jsonNode = materializeStructuredValue(jsonNode);
    if (jsonNode.isArray())
    {
      return Optional.of((ArrayNode)jsonNode);
    }
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.add(jsonNode);
    return Optional.of(arrayNode);
  }

  /**
   * Parses JSON-encoded objects and arrays and replaces the textual value in this operation. Scalar text values
   * are deliberately not parsed, so values such as {@code "67890"} retain their JSON string type. Returning the
   * stored node is important because callers modify the node returned by the getters in place.
   */
  private JsonNode materializeStructuredValue(JsonNode valueNode)
  {
    if (valueNode.isArray())
    {
      ArrayNode arrayNode = (ArrayNode)valueNode;
      for ( int i = 0 ; i < arrayNode.size() ; i++ )
      {
        JsonNode parsedElement = parseStructuredText(arrayNode.get(i));
        if (parsedElement != null)
        {
          arrayNode.set(i, parsedElement);
        }
      }
      return arrayNode;
    }

    JsonNode parsedValue = parseStructuredText(valueNode);
    if (parsedValue == null)
    {
      return valueNode;
    }
    setValueNode(parsedValue);
    valueExtracted = true;
    return get(AttributeNames.RFC7643.VALUE);
  }

  /**
   * Parses a textual node only if it contains a JSON object or array.
   */
  private JsonNode parseStructuredText(JsonNode valueNode)
  {
    if (!(valueNode instanceof TextNode))
    {
      return null;
    }
    try
    {
      JsonNode parsedNode = JsonHelper.readJsonDocument(valueNode.textValue());
      return parsedNode.isObject() || parsedNode.isArray() ? parsedNode : null;
    }
    catch (Exception ex)
    {
      return null;
    }
  }

  /**
   * sets the value of the targeted attribute as a JSON node. The node is stored verbatim, preserving its type
   * (scalar, object, or array). This is the appropriate setter for multivalued attributes, whose values are
   * represented as arrays (e.g. {@code ["ADMIN"]} or {@code [{...}]}).
   */
  public void setValueNode(JsonNode value)
  {
    if (value == null)
    {
      remove(AttributeNames.RFC7643.VALUE);
      return;
    }
    valueExtracted = false;
    set(AttributeNames.RFC7643.VALUE, value);
  }

  public static class PatchRequestOperationBuilder
  {

    public PatchRequestOperationBuilder()
    {}

    /**
     * sets a single scalar value for the operation. The value is stored as a scalar string, so it serializes as
     * {@code "value":"..."}. For an explicit array value use {@link #values(List)} or
     * {@link #valueNode(JsonNode)}.
     */
    public PatchRequestOperationBuilder value(String value)
    {
      this.valueNode(value == null ? null : TextNode.valueOf(value));
      return this;
    }
  }
}
