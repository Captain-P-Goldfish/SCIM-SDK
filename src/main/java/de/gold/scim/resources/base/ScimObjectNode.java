package de.gold.scim.resources.base;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.exceptions.InternalServerException;
import de.gold.scim.schemas.SchemaAttribute;
import de.gold.scim.utils.JsonHelper;
import de.gold.scim.utils.TimeUtils;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 20:10 <br>
 * <br>
 */
public class ScimObjectNode extends ObjectNode implements ScimNode
{

  /**
   * the schema definition of this document node
   */
  @Getter
  private SchemaAttribute schemaAttribute;

  public ScimObjectNode(SchemaAttribute schemaAttribute)
  {
    super(JsonNodeFactory.instance);
    this.schemaAttribute = schemaAttribute;
  }

  /**
   * extracts a string type attribute
   */
  protected Optional<String> getStringAttribute(String attributeName)
  {
    return JsonHelper.getSimpleAttribute(this, attributeName);
  }

  /**
   * extracts a boolean type attribute
   */
  protected Optional<Boolean> getBooleanAttribute(String attributeName)
  {
    return JsonHelper.getSimpleAttribute(this, attributeName, Boolean.class);
  }

  /**
   * extracts an integer type attribute
   */
  protected Optional<Integer> getIntegerAttribute(String attributeName)
  {
    return JsonHelper.getSimpleAttribute(this, attributeName, Integer.class);
  }

  /**
   * extracts a double type attribute
   */
  protected Optional<Double> getDoubleAttribute(String attributeName)
  {
    return JsonHelper.getSimpleAttribute(this, attributeName, Double.class);
  }

  /**
   * extracts a dateTime type attribute
   */
  protected Optional<Instant> getDateTimeAttribute(String attributeName)
  {
    String dateTime = JsonHelper.getSimpleAttribute(this, attributeName).orElse(null);
    return Optional.ofNullable(TimeUtils.parseDateTime(dateTime));
  }

  /**
   * extracts an object type attribute
   */
  protected <T extends ObjectNode> Optional<T> getObjectAttribute(String attributeName, Class<T> type)
  {
    JsonNode jsonNode = this.get(attributeName);
    if (jsonNode == null)
    {
      return Optional.empty();
    }
    if (!(jsonNode instanceof ObjectNode))
    {
      throw new InternalServerException("tried to extract a complex node from document with attribute " + "name '"
                                        + attributeName + "' but type is of: " + jsonNode.getNodeType(), null, null);
    }
    return Optional.of(JsonHelper.copyResourceToObject(jsonNode, type));
  }

  /**
   * extracts an object type attribute
   */
  protected <T extends ObjectNode> List<T> getArrayAttribute(String attributeName, Class<T> type)
  {
    JsonNode jsonNode = this.get(attributeName);
    if (jsonNode == null)
    {
      return Collections.emptyList();
    }
    if (!(jsonNode instanceof ArrayNode))
    {
      throw new InternalServerException("tried to extract a multi valued complex node from document with attribute "
                                        + "name '" + attributeName + "' but type is of: " + jsonNode.getNodeType(),
                                        null, null);
    }
    List<T> multiValuedComplexTypes = new ArrayList<>();
    for ( JsonNode node : jsonNode )
    {
      if (!(node instanceof ObjectNode))
      {
        throw new InternalServerException("tried to extract a complex node from document with attribute " + "name '"
                                          + attributeName + "' but type is of: " + jsonNode.getNodeType(), null, null);
      }
      multiValuedComplexTypes.add(JsonHelper.copyResourceToObject(node, type));
    }
    return multiValuedComplexTypes;
  }

  /**
   * adds or removes a string type attribute
   */
  protected void setAttribute(String attributeName, String attributeValue)
  {
    if (StringUtils.isBlank(attributeValue))
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    JsonHelper.addAttribute(this, attributeName, new TextNode(attributeValue));
  }

  /**
   * adds or removes a boolean type attribute
   */
  protected void setAttribute(String attributeName, Boolean attributeValue)
  {
    if (attributeValue == null)
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    JsonHelper.addAttribute(this, attributeName, BooleanNode.valueOf(attributeValue));
  }

  /**
   * adds or removes an integer type attribute
   */
  protected void setAttribute(String attributeName, Integer attributeValue)
  {
    if (attributeValue == null)
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    JsonHelper.addAttribute(this, attributeName, new IntNode(attributeValue));
  }

  /**
   * adds or removes a double type attribute
   */
  protected void setAttribute(String attributeName, Double attributeValue)
  {
    if (attributeValue == null)
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    JsonHelper.addAttribute(this, attributeName, new DoubleNode(attributeValue));
  }

  /**
   * adds or removes a dateTime type attribute
   */
  protected void setDateTimeAttribute(String attributeName, Instant attributeValue)
  {
    if (attributeValue == null)
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    String dateTime = attributeValue.truncatedTo(ChronoUnit.SECONDS).toString();
    JsonHelper.addAttribute(this, attributeName, new TextNode(dateTime));
  }

  /**
   * adds or removes a dateTime type attribute
   */
  protected void setDateTimeAttribute(String attributeName, LocalDateTime attributeValue)
  {
    if (attributeValue == null)
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    ZoneOffset zoneOffset = OffsetDateTime.now().getOffset();
    setDateTimeAttribute(attributeName, attributeValue.withNano(0).atOffset(zoneOffset));
  }

  /**
   * adds or removes a dateTime type attribute
   */
  protected void setDateTimeAttribute(String attributeName, OffsetDateTime attributeValue)
  {
    if (attributeValue == null)
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    String dateTime = attributeValue.withNano(0).format(DateTimeFormatter.ISO_DATE_TIME);
    JsonHelper.addAttribute(this, attributeName, new TextNode(dateTime));
  }

  /**
   * adds or removes an object type attribute
   */
  protected void setAttribute(String attributeName, ObjectNode attributeValue)
  {
    if (attributeValue == null)
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    JsonHelper.addAttribute(this, attributeName, attributeValue);
  }

  /**
   * adds or removes an array type attribute
   */
  protected <T extends JsonNode> void setAttribute(String attributeName, List<T> attributeValue)
  {
    if (attributeValue == null || attributeValue.isEmpty())
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    attributeValue.forEach(arrayNode::add);
    JsonHelper.addAttribute(this, attributeName, arrayNode);
  }

  /**
   * adds a single entry to the array type attribute
   */
  protected <T extends JsonNode> void addAttribute(String attributeName, T attributeValue)
  {
    if (attributeValue == null)
    {
      return;
    }
    ArrayNode arrayNode = JsonHelper.getArrayAttribute(this, attributeName)
                                    .orElse(new ArrayNode(JsonNodeFactory.instance));
    arrayNode.add(attributeValue);
    JsonHelper.addAttribute(this, attributeName, arrayNode);
  }

}
