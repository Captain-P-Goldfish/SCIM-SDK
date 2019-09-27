package de.gold.scim.utils;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
  public static JsonNode readJsonObject(String classPathLocation)
  {
    log.trace("trying to read classpath resource from: {}", classPathLocation);
    try (InputStream inputStream = JsonHelper.class.getResourceAsStream(classPathLocation))
    {
      return new ObjectMapper().readTree(inputStream);
    }
    catch (IOException e)
    {
      throw new de.gold.scim.exceptions.IOException(e.getMessage(), e);
    }
  }

}
