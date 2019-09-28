package de.gold.scim.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.exceptions.IncompatibleAttributeException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 00:05 <br>
 * <br>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonHelper
{

  /**
   * will read a json document from the classpath
   * 
   * @param classPathLocation the location of the document
   * @return the parsed json document
   */
  public static JsonNode loadJsonDocument(String classPathLocation)
  {
    log.trace("trying to read classpath resource from: {}", classPathLocation);
    try (InputStream inputStream = JsonHelper.class.getResourceAsStream(classPathLocation))
    {
      return new ObjectMapper().readTree(inputStream);
    }
    catch (IOException e)
    {
      throw de.gold.scim.exceptions.IOException.builder().message(e.getMessage()).cause(e).build();
    }
  }

  /**
   * will read a json document from a file
   * 
   * @param file the location of the document
   * @return the parsed json document
   */
  public static JsonNode loadJsonDocument(File file)
  {
    log.trace("trying to read classpath resource from: {}", file.getAbsolutePath());
    try (InputStream inputStream = new FileInputStream(file))
    {
      return new ObjectMapper().readTree(inputStream);
    }
    catch (IOException e)
    {
      throw de.gold.scim.exceptions.IOException.builder().message(e.getMessage()).cause(e).build();
    }
  }

  /**
   * will read a json document from the given string
   * 
   * @param jsonDocument the direct json representation
   * @return the parsed json document
   */
  public static JsonNode readJsonDocument(String jsonDocument)
  {
    log.trace("trying to read json document: {}", jsonDocument);
    try (Reader reader = new StringReader(jsonDocument))
    {
      return new ObjectMapper().readTree(reader);
    }
    catch (IOException e)
    {
      throw de.gold.scim.exceptions.IOException.builder().message("test").cause(e).build();
    }
  }

  /**
   * tries to get an array from the given json node
   * 
   * @param jsonNode the json node from which the array should be extracted
   * @param name the name of the json array attribute
   * @return the json array attribute or an empty if the attribute is not present
   */
  public static Optional<JsonNode> getArrayAttribute(JsonNode jsonNode, String name)
  {
    JsonNode attribute = Objects.requireNonNull(jsonNode, "jsonNode must not be null").get(name);
    if (attribute == null)
    {
      return Optional.empty();
    }
    if (attribute.isArray())
    {
      return Optional.of(attribute);
    }
    throw IncompatibleAttributeException.builder()
                                        .message("attribute with name '" + name + "' is not of type array")
                                        .build();
  }

  /**
   * extracts a list of simple attributes from the given json node
   * 
   * @param jsonNode the json document containing an array with simple attributes
   * @param attributeName the name of the attribute that is an array with simple attributes
   * @return a list of attributes or an empty
   */
  public static Optional<List<String>> getSimpleAttributeArray(JsonNode jsonNode, String attributeName)
  {
    return getSimpleAttributeArray(jsonNode, attributeName, String.class);
  }

  /**
   * extracts a list of simple attributes from the given json node
   * 
   * @param jsonNode the json document containing an array with simple attributes
   * @param attributeName the name of the attribute that is an array with simple attributes
   * @param type the type of the values within the array
   * @return a list of attributes or an empty
   */
  public static <T> Optional<List<T>> getSimpleAttributeArray(JsonNode jsonNode, String attributeName, Class<T> type)
  {
    Optional<JsonNode> simpleArrayOptional = getArrayAttribute(jsonNode, attributeName);
    if (!simpleArrayOptional.isPresent())
    {
      return Optional.empty();
    }
    JsonNode simpleArray = simpleArrayOptional.get();
    if (simpleArray.isObject())
    {
      final String errorMessage = "attribute '" + attributeName + "' is not a simple array attribute";
      throw IncompatibleAttributeException.builder().message(errorMessage).build();
    }
    List<T> arrayResult = new ArrayList<>();
    for ( JsonNode node : simpleArray )
    {
      getAsAttribute(node, type).ifPresent(arrayResult::add);
    }
    return Optional.of(arrayResult);
  }

  /**
   * will get a string attribute with the given name from the given json node
   *
   * @param jsonNode the json node to get the attribute from
   * @param name the name of the attribute
   * @return the value as string or an empty
   */
  public static Optional<String> getSimpleAttribute(JsonNode jsonNode, String name)
  {
    return getSimpleAttribute(jsonNode, name, String.class);
  }

  /**
   * will get a string attribute with the given name from the given json node
   * 
   * @param jsonNode the json node to get the attribute from
   * @param name the name of the attribute
   * @param type the type of the attribute to return
   * @return the value of the given type or an empty
   */
  public static <T> Optional<T> getSimpleAttribute(JsonNode jsonNode, String name, Class<T> type)
  {
    JsonNode attribute = Objects.requireNonNull(jsonNode, "jsonNode must not be null").get(name);
    if (attribute == null)
    {
      return Optional.empty();
    }
    if (attribute.isNull())
    {
      return Optional.empty();
    }
    if (attribute.isArray())
    {
      throw IncompatibleAttributeException.builder()
                                          .message("attribute '" + name + "' is not of type " + type.getSimpleName())
                                          .build();
    }
    return getAsAttribute(attribute, type);
  }

  /**
   * will remove an attribute from the given jsonNode
   * 
   * @param jsonNode the json node from which the attribute should be removed
   * @param attributeName the name of the attribute to remove
   */
  public static JsonNode removeAttribute(JsonNode jsonNode, String attributeName)
  {
    return ((ObjectNode)jsonNode).remove(attributeName);
  }

  /**
   * will remove an attribute from the given jsonNode
   *
   * @param jsonNode the json node from which the attribute should be removed
   * @param attributeName the name of the attribute to remove
   * @param newAttriute the new attribute that should be added
   */
  public static JsonNode addAttribute(JsonNode jsonNode, String attributeName, JsonNode newAttriute)
  {
    return ((ObjectNode)jsonNode).set(attributeName, newAttriute);
  }

  /**
   * will remove an attribute from the given jsonNode
   *
   * @param jsonArray the json node from which the attribute should be removed
   * @param newAttriute the new attribute that should be added
   */
  public static JsonNode addAttributeToArray(JsonNode jsonArray, JsonNode newAttriute)
  {
    return ((ArrayNode)jsonArray).add(newAttriute);
  }

  /**
   * will remove an attribute from the given jsonNode
   * 
   * @param jsonNode the json node from which the attribute should be removed
   * @param attributeName the name of the attribute to remove
   * @param value the value of the new replaced node
   */
  public static <T> JsonNode writeValue(JsonNode jsonNode, String attributeName, T value)
  {
    JsonNode valueNode = new TextNode(String.valueOf(value));
    return ((ObjectNode)jsonNode).replace(attributeName, valueNode);
  }

  /**
   * will remove an attribute from the given jsonNode
   * 
   * @param jsonNode the json node from which the attribute should be removed
   * @param attributeName the name of the attribute to remove
   * @param replaceNode the new node that should be used as replacement
   */
  public static JsonNode replaceNode(JsonNode jsonNode, String attributeName, JsonNode replaceNode)
  {
    return ((ObjectNode)jsonNode).replace(attributeName, replaceNode);
  }

  /**
   * gets the simple attribute of the given json node
   * 
   * @param attribute the json node that should be a simple attribute
   * @param type the type to extract
   * @return the extracted attribute or an empty
   */
  private static <T> Optional<T> getAsAttribute(JsonNode attribute, Class<T> type)
  {
    if (String.class.equals(type))
    {
      return Optional.of((T)attribute.asText());
    }
    if (Boolean.class.equals(type))
    {
      return Optional.of((T)Boolean.valueOf(attribute.asBoolean()));
    }
    if (Integer.class.equals(type))
    {
      return Optional.of((T)Integer.valueOf(attribute.asInt()));
    }
    if (Long.class.equals(type))
    {
      return Optional.of((T)Long.valueOf(attribute.asLong()));
    }
    if (Float.class.equals(type))
    {
      return Optional.of((T)Float.valueOf((float)attribute.asDouble()));
    }
    if (Double.class.equals(type))
    {
      return Optional.of((T)Double.valueOf(attribute.asDouble()));
    }
    throw IncompatibleAttributeException.builder()
                                        .message("attribute '" + attribute + "' is not of type" + type.getSimpleName())
                                        .build();
  }

}
