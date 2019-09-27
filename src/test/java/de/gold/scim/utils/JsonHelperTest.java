package de.gold.scim.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ClassPathReferences;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 00:10 <br>
 * <br>
 */
@Slf4j
public class JsonHelperTest
{

  /**
   * will test that the given class path reference is an existing file within the classpath that contains a json
   * document
   */
  @Test
  public void testReadClassPathJsonDocument()
  {
    JsonNode jsonNode = JsonHelper.readJsonObject(ClassPathReferences.META_SCHEMA_JSON);
    Assertions.assertNotNull(jsonNode);
    Assertions.assertNotNull(jsonNode.get("id"));
    log.trace("\"id\": \"{}\"", jsonNode.get("id").asText());
  }
}
