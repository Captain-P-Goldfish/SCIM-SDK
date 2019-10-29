package de.gold.scim.request;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.enums.PatchOp;
import de.gold.scim.resources.base.ScimObjectNode;
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

  @Builder
  public PatchRequestOperation(String path, PatchOp op, List<String> value)
  {
    this();
    setPath(path);
    setOp(op);
    setValue(value);
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
   * the new value of the targeted attribute <br>
   * (This will never return null on server side for schema validation is executed before this method is called)
   */
  public List<String> getValue()
  {
    return getSimpleArrayAttribute(AttributeNames.RFC7643.VALUE);
  }

  /**
   * the new value of the targeted attribute
   */
  public void setValue(List<String> value)
  {
    setAttributeList(AttributeNames.RFC7643.VALUE, value);
  }

  /**
   * the new value of the targeted attribute. in this case the value is represented by the resource itself
   */
  public void setValueNode(JsonNode value)
  {
    setAttribute(AttributeNames.RFC7643.VALUE, Collections.singletonList(value));
  }

}
