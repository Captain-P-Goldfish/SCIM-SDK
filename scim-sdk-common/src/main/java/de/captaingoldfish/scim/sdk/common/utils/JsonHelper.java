package de.captaingoldfish.scim.sdk.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.IncompatibleAttributeException;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
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

  public static final ObjectMapper mapper;

  static
  {
    mapper = new ObjectMapper();
  }

  /**
   * will read a json document from the classpath
   *
   * @param classPathLocation the location of the document
   * @return the parsed json document
   */
  public static JsonNode loadJsonDocument(String classPathLocation)
  {
    log.trace("Trying to read classpath resource from: {}", classPathLocation);
    try (InputStream inputStream = JsonHelper.class.getResourceAsStream(classPathLocation))
    {
      return mapper.readTree(inputStream);
    }
    catch (IOException e)
    {
      throw new de.captaingoldfish.scim.sdk.common.exceptions.IOException(e.getMessage(), e, null, null);
    }
  }

  /**
   * will read a json document from the classpath
   *
   * @param classPathLocation the location of the document
   * @return the parsed json document
   */
  public static <T extends ObjectNode> T loadJsonDocument(String classPathLocation, Class<T> type)
  {
    log.trace("Trying to read classpath resource from: {}", classPathLocation);
    try (InputStream inputStream = JsonHelper.class.getResourceAsStream(classPathLocation))
    {
      JsonNode jsonNode = mapper.readTree(inputStream);
      return copyResourceToObject(jsonNode, type);
    }
    catch (IOException e)
    {
      throw new de.captaingoldfish.scim.sdk.common.exceptions.IOException(e.getMessage(), e, null, null);
    }
  }

  /**
   * will read a json document from a file
   *
   * @param file the location of the document
   * @return the parsed json document
   */
  public static <T extends ObjectNode> T loadJsonDocument(File file, Class<T> type)
  {
    log.trace("Trying to read classpath resource from: {}", file.getAbsolutePath());
    try (InputStream inputStream = new FileInputStream(file))
    {
      JsonNode jsonNode = mapper.readTree(inputStream);
      return copyResourceToObject(jsonNode, type);
    }
    catch (IOException e)
    {
      throw new de.captaingoldfish.scim.sdk.common.exceptions.IOException(e.getMessage(), e, null, null);
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
    log.trace("Trying to read classpath resource from: {}", file.getAbsolutePath());
    try (InputStream inputStream = new FileInputStream(file))
    {
      return mapper.readTree(inputStream);
    }
    catch (IOException e)
    {
      throw new de.captaingoldfish.scim.sdk.common.exceptions.IOException(e.getMessage(), e, null, null);
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
    if (StringUtils.isBlank(jsonDocument))
    {
      return null;
    }
    log.trace("Trying to read json document: {}", jsonDocument);
    try (Reader reader = new StringReader(jsonDocument))
    {
      return mapper.readTree(reader);
    }
    catch (IOException e)
    {
      throw new de.captaingoldfish.scim.sdk.common.exceptions.IOException("Invalid content, the document could not be parsed",
                                                                          e, null, null);
    }
  }

  /**
   * will read a json document from the given string
   *
   * @param jsonDocument the direct json representation
   * @return the parsed json document
   */
  public static <T extends ObjectNode> T readJsonDocument(String jsonDocument, Class<T> type)
  {
    log.trace("Trying to read json document: {}", jsonDocument);
    try (Reader reader = new StringReader(jsonDocument))
    {
      JsonNode jsonNode = mapper.readTree(reader);
      return copyResourceToObject(jsonNode, type);
    }
    catch (IOException e)
    {
      throw new de.captaingoldfish.scim.sdk.common.exceptions.IOException("Invalid content, the document could not be parsed",
                                                                          e, null, null);
    }
  }

  /**
   * tries to get an array from the given json node
   *
   * @param jsonNode the json node from which the array should be extracted
   * @param name the name of the json array attribute
   * @return the json array attribute or an empty if the attribute is not present
   */
  public static Optional<ArrayNode> getArrayAttribute(JsonNode jsonNode, String name)
  {
    JsonNode attribute = Objects.requireNonNull(jsonNode, "jsonNode must not be null").get(name);
    if (attribute == null)
    {
      return Optional.empty();
    }
    if (attribute.isArray())
    {
      return Optional.of((ArrayNode)attribute);
    }
    throw new IncompatibleAttributeException("attribute with name '" + name + "' is not of type array", null, null,
                                             null);
  }

  /**
   * tries to get an json object from the given json node
   *
   * @param jsonNode the json node from which the json object should be extracted
   * @param name the name of the json object attribute
   * @return the json object attribute or an empty if the attribute is not present
   */
  public static Optional<ObjectNode> getObjectAttribute(JsonNode jsonNode, String name)
  {
    JsonNode attribute = Objects.requireNonNull(jsonNode, "jsonNode must not be null").get(name);
    if (attribute == null)
    {
      return Optional.empty();
    }
    if (attribute.isObject())
    {
      return Optional.of((ObjectNode)attribute);
    }
    throw new IncompatibleAttributeException("attribute with name '" + name + "' is not of type object", null, null,
                                             null);
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
    Optional<ArrayNode> simpleArrayOptional = getArrayAttribute(jsonNode, attributeName);
    if (!simpleArrayOptional.isPresent())
    {
      return Optional.empty();
    }
    ArrayNode simpleArray = simpleArrayOptional.get();
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
      throw new IncompatibleAttributeException(String.format("attribute '%s' is not of type %s",
                                                             name,
                                                             type.getSimpleName()),
                                               null, null, ScimType.RFC7644.INVALID_VALUE);
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
   * will remove a simple value from a simple array node in the given json document
   *
   * @param jsonNode the array from which the value should be removed
   * @param value the value that should be removed from the document
   */
  public static JsonNode removeSimpleAttributeFromArray(JsonNode jsonNode, String value)
  {
    if (jsonNode == null)
    {
      return null;
    }
    if (!jsonNode.isArray())
    {
      log.info("Cannot remove value '{}' from a json node that is not a simple array", value);
      return jsonNode;
    }
    int index = -1;
    for ( int i = 0 ; i < jsonNode.size() ; i++ )
    {
      JsonNode simpleNode = jsonNode.get(i);
      if (simpleNode.isObject() || simpleNode.isArray())
      {
        break;
      }
      if (simpleNode.textValue().equals(value))
      {
        index = i;
        break;
      }
    }
    if (index > -1)
    {
      ((ArrayNode)jsonNode).remove(index);
    }
    else
    {
      log.info("Could not remove value '{}' from json array because its sub-elements are not primitive types", value);
    }
    return jsonNode;
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
    if (byte[].class.equals(type))
    {
      if (attribute.isBinary())
      {
        try
        {
          return Optional.of((T)attribute.binaryValue());
        }
        catch (IOException e)
        {
          throw new IncompatibleAttributeException("binary attribute value could not be read", e, null,
                                                   ScimType.RFC7644.INVALID_VALUE);
        }
      }
      try
      {
        return Optional.of((T)Base64.getDecoder().decode(attribute.textValue()));
      }
      catch (IllegalArgumentException e)
      {
        throw new IncompatibleAttributeException(String.format("attribute value '%s' is not of type %s",
                                                               attribute.textValue(),
                                                               Type.BINARY),
                                                 e, null, ScimType.RFC7644.INVALID_VALUE);
      }
    }
    throw new IncompatibleAttributeException(String.format("attribute '%s' is not of type %s",
                                                           attribute,
                                                           type.getSimpleName()),
                                             null, null, ScimType.RFC7644.INVALID_VALUE);
  }

  /**
   * creates a new instance of the given type and moves the content from the resource into the new node
   *
   * @param resource the resource that holds the content that must be moved to the new object
   * @param type the type from which an instance will be created with a noArgs constructor
   * @return a newly created instance with the content of the {@code resource}-node
   */
  public static <T extends ObjectNode> T copyResourceToObject(JsonNode resource, Class<T> type)
  {
    if (resource == null)
    {
      return null;
    }
    if (resource.isArray())
    {
      throw new IncompatibleAttributeException("operation not possible for array", null, null,
                                               ScimType.Custom.INVALID_PARAMETERS);
    }
    if (type.isAssignableFrom(resource.getClass()))
    {
      return (T)resource;
    }
    T newInstance = getNewInstance(type, resource);
    resource.properties().forEach(stringJsonNodeEntry -> {
      JsonHelper.addAttribute(newInstance, stringJsonNodeEntry.getKey(), stringJsonNodeEntry.getValue());
    });
    return newInstance;
  }

  /**
   * builds a new instance of the given resource node type
   *
   * @param type the type for which the instance should be build
   * @param <T> the type must define a noArgs constructor
   * @return the newly created instance
   */
  public static <T extends ScimObjectNode> T getNewInstance(Class<T> type)
  {
    try
    {
      Constructor<T> constructor = type.getConstructor();
      return constructor.newInstance();
    }
    catch (InstantiationException | IllegalAccessException | InvocationTargetException e)
    {
      throw new InternalServerException("could not create instance of type '" + type + "': " + e.getMessage(), e, null);
    }
    catch (NoSuchMethodException e)
    {
      throw new InternalServerException("missing no args constructor for type '" + type + "': " + e.getMessage(), e,
                                        null);
    }
  }

  /**
   * creates a new instance of the given type
   *
   * @param <T> the type must define a noArgs constructor
   * @param type the type from which a new instance will be created
   * @param resource an already existing instance that is passed as constructor param to the instance, if such a
   *          constructor exists
   * @return the newly created instance
   */
  private static <T extends JsonNode> T getNewInstance(Class<T> type, JsonNode resource)
  {
    try
    {
      if (resource != null && resource.size() != 0)
      {
        try
        {
          Constructor<T> constructor = type.getConstructor(JsonNode.class);
          return constructor.newInstance(resource);
        }
        catch (NoSuchMethodException e)
        {}
        catch (InvocationTargetException e)
        {
          throw new InternalServerException("could not create instance of type '" + type + "': " + e.getMessage(), e,
                                            null);
        }
      }
      Constructor<T> constructor = type.getConstructor();
      return constructor.newInstance();
    }
    catch (InstantiationException | IllegalAccessException | InvocationTargetException e)
    {
      throw new InternalServerException("could not create instance of type '" + type + "': " + e.getMessage(), e, null);
    }
    catch (NoSuchMethodException e)
    {
      throw new InternalServerException("missing no args constructor for type '" + type + "': " + e.getMessage(), e,
                                        null);
    }
  }

  /**
   * will extract a scim attribute by its scim-name.
   *
   * @param attributeName the scim name of the attribute e.g. "userName" of "name.givenName"
   * @return the json node or an empty
   */
  public static Optional<JsonNode> getSimpleAttributeByName(JsonNode jsonNode, String attributeName)
  {
    String[] nameParts = attributeName.split("\\.");
    JsonNode subNode = jsonNode.get(nameParts[0]);
    if (nameParts.length == 1)
    {
      return Optional.ofNullable(subNode);
    }
    return Optional.ofNullable(subNode.get(nameParts[1]));
  }

  /**
   * validates if the given string structure is valid json or not
   *
   * @param json the string to validate
   * @return true if the given string is a valid json structure, false else
   */
  public static boolean isValidJson(final String json)
  {
    if (StringUtils.isBlank(json))
    {
      return false;
    }
    try
    {
      final JsonParser parser = mapper.getFactory().createParser(json);
      while (parser.nextToken() != null)
      {}
      return true;
    }
    catch (IOException ex)
    {
      log.info(ex.getMessage());
      log.trace(ex.getMessage(), ex);
      return false;
    }
  }

  /**
   * creates a json string from the given json node
   */
  /**
   * override method for usage with wildfly 18 that still uses jackson 2.9.x
   */
  public static String toJsonString(JsonNode jsonNode)
  {
    try
    {
      return mapper.writeValueAsString(jsonNode);
    }
    catch (JsonProcessingException e)
    {
      throw new InternalServerException(e.getMessage(), e);
    }
  }

  /**
   * override method for usage with wildfly 18 that still uses jackson 2.9.x
   */
  public static String toPrettyJsonString(JsonNode jsonNode)
  {
    try
    {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    }
    catch (JsonProcessingException e)
    {
      throw new InternalServerException(e.getMessage(), e);
    }
  }

  /**
   * override method for usage with wildfly 18 that still uses jackson 2.9.x
   */
  public static boolean isEmpty(JsonNode jsonNode)
  {
    return jsonNode.size() == 0;
  }

  /**
   * jackson is currently not supporting comparison of int and long-nodes even if the nodes do have the same
   * values. for this reason we are overriding the comparison here in case for number nodes
   *
   * @return true if the list contains a node that is equal to given complexNode, false else
   */
  public static <T extends JsonNode> boolean containsEqualObject(List<T> originalNodes, T jsonNode)
  {
    return originalNodes.parallelStream().anyMatch(originalNode -> isEqual(originalNode, jsonNode));
  }

  /**
   * jackson is currently not supporting comparison of int and long-nodes even if the nodes do have the same
   * values. for this reason we are overriding the comparison here in case for number nodes
   *
   * @return true if the list contains a node that is equal to given complexNode, false else
   */
  public static boolean isEqual(JsonNode node1, JsonNode node2)
  {
    if (node1 == null && node2 == null)
    {
      return true;
    }
    else if (node1 == null || node2 == null)
    {
      return false;
    }
    return node1.equals((o1, o2) -> {
      boolean isO1Null = o1 == null || o1.isNull();
      boolean isO2Null = o2 == null || o2.isNull();
      if (isO1Null && isO2Null)
      {
        return 0;
      }
      else if (isO1Null || isO2Null)
      {
        return 1;
      }

      if (o1.isNumber() && o2.isNumber())
      {
        return o1.longValue() == o2.longValue() ? 0 : 1;
      }
      if (o1.isNumber() && o2.isNumber())
      {
        return o1.longValue() == o2.longValue() ? 0 : 1;
      }
      else
      {
        return o1.equals(o2) ? 0 : 1;
      }
    }, node2);
  }

}
