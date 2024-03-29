package de.captaingoldfish.scim.sdk.common.request;

import java.util.Arrays;
import java.util.Collections;
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
    setPath(path);
    setOp(op);
    if (values == null || values.isEmpty())
    {
      setValueNode(valueNode);
    }
    else
    {
      setValues(values);
    }
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
    if (valueNode.isObject() || valueNode.isArray())
    {
      valueExtracted = true;
      return Optional.of(valueNode);
    }
    String textValue = valueNode.asText();
    if (valueNode instanceof TextNode && textValue.contains("{"))
    {
      try
      {
        valueNode = JsonHelper.readJsonDocument(textValue);
        setValue(valueNode);
      }
      catch (Exception ex)
      {
        // do nothing
      }
    }
    valueExtracted = true;
    return Optional.of(valueNode);
  }

  /**
   * the new value of the targeted attribute
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
   * the new value of the targeted attribute
   */
  public void setValues(List<String> value)
  {
    valueExtracted = false;
    if (value == null || value.size() > 1)
    {
      setAttributeList(AttributeNames.RFC7643.VALUE, value);
    }
    else if (value.size() == 1)
    {
      setAttribute(AttributeNames.RFC7643.VALUE, value.get(0));
    }
    else
    {
      setAttribute(AttributeNames.RFC7643.VALUE, (String)null);
    }
  }

  /**
   * reads the value-attribute and if it is not an {@link ArrayNode} it will be parsed to a {@link JsonNode}
   * that will be embedded within an {@link ArrayNode}
   */
  public Optional<ArrayNode> getValueNode()
  {
    JsonNode jsonNode = get(AttributeNames.RFC7643.VALUE);
    if (jsonNode == null || jsonNode.isNull())
    {
      return Optional.empty();
    }
    if (jsonNode.isArray())
    {
      return Optional.of((ArrayNode)jsonNode);
    }
    if (jsonNode instanceof TextNode)
    {
      ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
      try
      {
        JsonNode jsonValue = JsonHelper.readJsonDocument(jsonNode.textValue());
        arrayNode.add(jsonValue);
      }
      catch (Exception ex)
      {
        arrayNode.add(jsonNode.textValue());
      }
      setValueNode(arrayNode);
      return Optional.of(arrayNode);
    }
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.add(jsonNode);
    setValueNode(arrayNode);
    return Optional.of(arrayNode);
  }

  /**
   * the new value of the targeted attribute. in this case the value is represented by the resource itself
   */
  public void setValueNode(JsonNode value)
  {
    setAttribute(AttributeNames.RFC7643.VALUE, value == null ? null : Collections.singletonList(value));
  }

  public static class PatchRequestOperationBuilder
  {

    public PatchRequestOperationBuilder()
    {}

    public PatchRequestOperationBuilder value(String value)
    {
      this.values(Arrays.asList(value));
      return this;
    }
  }
}
