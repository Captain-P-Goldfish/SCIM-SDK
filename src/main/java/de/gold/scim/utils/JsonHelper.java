package de.gold.scim.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
   * will get a string attribute with the given name from the given json node
   * 
   * @param jsonNode the json node to get the attribute from
   * @param name the name of the attribute
   * @param type the type of the attribute to return
   * @return the value as string or an empty
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
                                          .message("attribute '" + name + "' is not of type" + type.getSimpleName())
                                          .build();
    }
    if (String.class.equals(type))
    {
      return (Optional<T>)Optional.of(attribute.asText());
    }
    if (Boolean.class.equals(type))
    {
      return (Optional<T>)Optional.of(attribute.asBoolean());
    }
    if (Integer.class.equals(type))
    {
      return (Optional<T>)Optional.of(attribute.asInt());
    }
    if (Long.class.equals(type))
    {
      return (Optional<T>)Optional.of(attribute.asLong());
    }
    if (Float.class.equals(type))
    {
      return (Optional<T>)Optional.of((float)attribute.asDouble());
    }
    if (Double.class.equals(type))
    {
      return (Optional<T>)Optional.of(attribute.asDouble());
    }
    throw IncompatibleAttributeException.builder()
                                        .message("attribute '" + name + "' is not of type" + type.getSimpleName())
                                        .build();
  }

}
