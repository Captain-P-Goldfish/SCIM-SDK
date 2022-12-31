package de.captaingoldfish.scim.sdk.common.resources.base;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.common.utils.TimeUtils;
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

  public ScimObjectNode()
  {
    super(JsonNodeFactory.instance);
  }

  public ScimObjectNode(SchemaAttribute schemaAttribute)
  {
    this();
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
   * extracts a long type attribute
   */
  protected Optional<Long> getLongAttribute(String attributeName)
  {
    return JsonHelper.getSimpleAttribute(this, attributeName, Long.class);
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
   * extracts a {@link TextNode} type attribute
   */
  protected <T extends TextNode> Optional<T> getStringAttribute(String attributeName, Class<T> type)
  {
    JsonNode jsonNode = this.get(attributeName);
    if (jsonNode == null || jsonNode.isNull())
    {
      return Optional.empty();
    }
    if (!(jsonNode instanceof TextNode))
    {
      throw new InternalServerException("tried to extract a string node from document with attribute name '"
                                        + attributeName + "' but type is of: " + jsonNode.getNodeType(), null, null);
    }
    if (type.isAssignableFrom(jsonNode.getClass()))
    {
      return Optional.of((T)jsonNode);
    }
    try
    {
      T t = (T)type.getMethod("newInstance", String.class).invoke(null, jsonNode.textValue());
      this.set(attributeName, t);
      return Optional.of(t);
    }
    catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
    {
      throw new InternalServerException(e.getMessage(), e, null);
    }
  }

  /**
   * extracts an object type attribute
   */
  protected <T extends ObjectNode> Optional<T> getObjectAttribute(String attributeName, Class<T> type)
  {
    JsonNode jsonNode = this.get(attributeName);
    if (jsonNode == null || jsonNode.isNull())
    {
      return Optional.empty();
    }
    if (!(jsonNode instanceof ObjectNode))
    {
      throw new InternalServerException("tried to extract a complex node from document with attribute " + "name '"
                                        + attributeName + "' but type is of: " + jsonNode.getNodeType(), null, null);
    }
    if (type.isAssignableFrom(jsonNode.getClass()))
    {
      return Optional.of((T)jsonNode);
    }
    T t = JsonHelper.copyResourceToObject(jsonNode, type);
    this.set(attributeName, t);
    return Optional.of(t);
  }

  /**
   * extracts an object type attribute
   */
  protected <T extends ObjectNode> List<T> getArrayAttribute(String attributeName, Class<T> type)
  {
    JsonNode jsonNode = this.get(attributeName);
    if (jsonNode == null || jsonNode.isNull())
    {
      return new ArrayList<>();
    }
    if (!(jsonNode instanceof ArrayNode))
    {
      throw new InternalServerException("tried to extract a multi valued complex node from document with attribute "
                                        + "name '" + attributeName + "' but type is of: " + jsonNode.getNodeType(),
                                        null, null);
    }
    List<T> multiValuedComplexTypes = new ArrayList<>();
    boolean shouldBeReplaced = false;
    for ( JsonNode node : jsonNode )
    {
      if (!(node instanceof ObjectNode))
      {
        throw new InternalServerException("tried to extract a complex node from document with attribute " + "name '"
                                          + attributeName + "' but type is of: " + jsonNode.getNodeType(), null, null);
      }
      if (type.isAssignableFrom(node.getClass()))
      {
        multiValuedComplexTypes.add((T)node);
      }
      else
      {
        shouldBeReplaced = true;
        T t = JsonHelper.copyResourceToObject(node, type);
        multiValuedComplexTypes.add(t);
      }
    }
    if (shouldBeReplaced)
    {
      setAttribute(attributeName, multiValuedComplexTypes);
    }
    return multiValuedComplexTypes;
  }

  /**
   * extracts a simple attribute type
   *
   * @param attributeName the name of the array attribute
   */
  protected List<String> getSimpleArrayAttribute(String attributeName)
  {
    return getSimpleArrayAttribute(attributeName, String.class);
  }

  /**
   * extracts a simple attribute type
   *
   * @param attributeName the name of the array attribute
   * @param type the type that should be extracted
   * @param <T> a simple attribute type as Long, Double, String, Boolean or Instant. Other types are not allowed
   */
  protected <T> List<T> getSimpleArrayAttribute(String attributeName, Class<T> type)
  {
    if (!Arrays.asList(Long.class, Double.class, Boolean.class, String.class, Instant.class).contains(type))
    {
      throw new InternalServerException("the type '" + type.getSimpleName() + "' is not allowed for this method", null,
                                        null);
    }
    JsonNode jsonNode = this.get(attributeName);
    if (jsonNode == null || jsonNode.isNull())
    {
      return new ArrayList<>();
    }
    if (!(jsonNode instanceof ArrayNode))
    {
      if (jsonNode instanceof ObjectNode)
      {
        throw new InternalServerException("tried to extract a multi valued complex node from document with attribute "
                                          + "name '" + attributeName + "' but type is of: " + jsonNode.getNodeType(),
                                          null, null);
      }
      else
      {
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        arrayNode.add(jsonNode);
        jsonNode = arrayNode;
      }
    }
    List<T> multiValuedSimpleTypes = new ArrayList<>();
    for ( JsonNode node : jsonNode )
    {
      if (Long.class.isAssignableFrom(type))
      {
        multiValuedSimpleTypes.add((T)Long.valueOf(node.longValue()));
      }
      else if (Double.class.isAssignableFrom(type))
      {
        multiValuedSimpleTypes.add((T)Double.valueOf(node.doubleValue()));
      }
      else if (Boolean.class.isAssignableFrom(type))
      {
        multiValuedSimpleTypes.add((T)Boolean.valueOf(node.booleanValue()));
      }
      else if (String.class.isAssignableFrom(type))
      {
        multiValuedSimpleTypes.add((T)(node.isTextual() ? node.textValue() : node.toString()));
      }
      else
      {
        multiValuedSimpleTypes.add((T)TimeUtils.parseDateTime(node.textValue()));
      }
    }
    return multiValuedSimpleTypes;
  }

  /**
   * extracts a simple attribute type
   *
   * @param attributeName the name of the array attribute
   */
  protected Set<String> getSimpleArrayAttributeSet(String attributeName)
  {
    return getSimpleArrayAttributeSet(attributeName, String.class);
  }

  /**
   * extracts a simple attribute type
   *
   * @param attributeName the name of the array attribute
   * @param type the type that should be extracted
   * @param <T> a simple attribute type as Long, Double, String, Boolean or Instant. Other types are not allowed
   */
  protected <T> Set<T> getSimpleArrayAttributeSet(String attributeName, Class<T> type)
  {
    if (!Arrays.asList(Long.class, Double.class, Boolean.class, String.class, Instant.class).contains(type))
    {
      throw new InternalServerException("the type '" + type.getSimpleName() + "' is not allowed for this method", null,
                                        null);
    }
    JsonNode jsonNode = this.get(attributeName);
    if (jsonNode == null || jsonNode.isNull())
    {
      return new HashSet<>();
    }
    if (!(jsonNode instanceof ArrayNode))
    {
      throw new InternalServerException("tried to extract a multi valued complex node from document with attribute "
                                        + "name '" + attributeName + "' but type is of: " + jsonNode.getNodeType(),
                                        null, null);
    }
    Set<T> multiValuedSimpleTypes = new HashSet<>();
    for ( JsonNode node : jsonNode )
    {
      if (Long.class.isAssignableFrom(type))
      {
        multiValuedSimpleTypes.add((T)Long.valueOf(node.longValue()));
      }
      else if (Double.class.isAssignableFrom(type))
      {
        multiValuedSimpleTypes.add((T)Double.valueOf(node.doubleValue()));
      }
      else if (Boolean.class.isAssignableFrom(type))
      {
        multiValuedSimpleTypes.add((T)Boolean.valueOf(node.booleanValue()));
      }
      else if (String.class.isAssignableFrom(type))
      {
        multiValuedSimpleTypes.add((T)(node.isTextual() ? node.textValue() : node.toString()));
      }
      else
      {
        multiValuedSimpleTypes.add((T)TimeUtils.parseDateTime(node.textValue()));
      }
    }
    return multiValuedSimpleTypes;
  }

  /**
   * adds or removes a string type attribute
   */
  protected void setAttribute(String attributeName, String attributeValue)
  {
    if (attributeValue == null)
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
   * adds or removes a long type attribute
   */
  protected void setAttribute(String attributeName, Long attributeValue)
  {
    if (attributeValue == null)
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    if (attributeValue == attributeValue.intValue())
    {
      JsonHelper.addAttribute(this, attributeName, new IntNode(attributeValue.intValue()));
    }
    else
    {
      JsonHelper.addAttribute(this, attributeName, new LongNode(attributeValue));
    }
  }

  /**
   * adds or removes an integer type attribute
   */
  protected void setAttribute(String attributeName, Integer attributeValue)
  {
    setAttribute(attributeName, attributeValue == null ? null : attributeValue.longValue());
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
   * adds or removes a dateTime type attribute by default the JSON String representation will keep
   * DEFAULT_FRACTIONALS_TO_KEEP fractionals i.e. 1970-01-01T00:00:00.000Z
   *
   * @see TimeUtils#DEFAULT_INSTANT_FRACTIONAL_DIGITS_FORMAT
   */
  protected void setDateTimeAttribute(String attributeName, Instant attributeValue)
  {
    setDateTimeAttribute(attributeName, attributeValue, TimeUtils.DEFAULT_INSTANT_FRACTIONAL_DIGITS_FORMAT);
  }

  /**
   * adds or removes a dateTime type attribute including the given fractionalDigits inside the JSON TextNode
   *
   * @param attributeName the given attributeName for the related Instant attributeValue
   * @param attributeValue the attributeValue might be null to remove the attribute from the JSON document
   * @param fractionalDigits MUST be a positive value between zero and nine i.e 0-9 default is set to 3
   * @see TimeUtils#DEFAULT_INSTANT_FRACTIONAL_DIGITS_FORMAT
   */
  protected void setDateTimeAttribute(String attributeName, Instant attributeValue, int fractionalDigits)
  {
    if (attributeValue == null)
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    // Keep given fractional digits in the stored JSON TextNode just in case of a zero value for the nano Instant
    // attributeValue @see
    // https://stackoverflow.com/questions/33025988/java-time-iso-date-format-with-fixed-millis-digits-in-java-8-and-later
    DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant(fractionalDigits).toFormatter();
    JsonHelper.addAttribute(this, attributeName, new TextNode(formatter.format(attributeValue)));
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
    setDateTimeAttribute(attributeName, attributeValue.atOffset(zoneOffset));
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
    String dateTime = attributeValue.format(DateTimeFormatter.ISO_DATE_TIME);
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
    for ( T jsonNodes : attributeValue )
    {
      // null nodes are ignored
      if (jsonNodes == null)
      {
        continue;
      }
      if (jsonNodes.isArray())
      {
        arrayNode.addAll((ArrayNode)jsonNodes);
      }
      else
      {
        arrayNode.add(jsonNodes);
      }
    }
    JsonHelper.addAttribute(this, attributeName, arrayNode);
  }

  /**
   * adds or removes an array type attribute
   */
  protected void setStringAttributeList(String attributeName, List<String> attributeValue)
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
   * adds or removes an array type attribute
   */
  protected void setStringAttributeList(String attributeName, Set<String> attributeValue)
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
   * adds or removes an array type attribute
   */
  protected <T> void setAttributeList(String attributeName, List<T> attributeValue)
  {
    if (attributeValue == null || attributeValue.isEmpty())
    {
      JsonHelper.removeAttribute(this, attributeName);
      return;
    }
    Class type = attributeValue.stream().filter(Objects::nonNull).findAny().orElse((T)"").getClass();
    if (!Arrays.asList(Long.class, Double.class, Boolean.class, String.class, Instant.class).contains(type))
    {
      throw new InternalServerException("the type '" + type.getSimpleName() + "' is not allowed for this method", null,
                                        null);
    }
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    if (Long.class.isAssignableFrom(type))
    {
      attributeValue.forEach(t -> arrayNode.add((Long)t));
    }
    else if (Double.class.isAssignableFrom(type))
    {
      attributeValue.forEach(t -> arrayNode.add((Double)t));
    }
    else if (Boolean.class.isAssignableFrom(type))
    {
      attributeValue.forEach(t -> arrayNode.add((Boolean)t));
    }
    else
    {
      attributeValue.forEach(t -> arrayNode.add(t == null ? null : String.valueOf(t)));
    }
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

  /**
   * override method for usage with wildfly 18 that still uses jackson 2.9.x
   */
  public String toString()
  {
    return JsonHelper.toJsonString(this);
  }

  /**
   * override method for usage with wildfly 18 that still uses jackson 2.9.x
   */
  public String toPrettyString()
  {
    return JsonHelper.toPrettyJsonString(this);
  }

  /**
   * override method for usage with wildfly 18 that still uses jackson 2.9.x
   */
  public boolean isEmpty()
  {
    return JsonHelper.isEmpty(this);
  }
}
