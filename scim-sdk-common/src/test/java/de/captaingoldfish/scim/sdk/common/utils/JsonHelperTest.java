package de.captaingoldfish.scim.sdk.common.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.exceptions.IncompatibleAttributeException;
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
    JsonNode jsonNode = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON);
    Assertions.assertNotNull(jsonNode);
    Assertions.assertNotNull(jsonNode.get("id"));
    log.trace("\"id\": \"{}\"", jsonNode.get("id").asText());
  }

  /**
   * will test that a json object can also be read from a custom file location
   */
  @Test
  public void testReadFileJsonDocument()
  {
    File file = new File(getClass().getResource(ClassPathReferences.META_RESOURCE_SCHEMA_JSON).getFile());
    JsonNode jsonNode = JsonHelper.loadJsonDocument(file);
    Assertions.assertNotNull(jsonNode);
    Assertions.assertNotNull(jsonNode.get("id"));
    log.trace("\"id\": \"{}\"", jsonNode.get("id").asText());
  }

  /**
   * will test that a json object can also be read from a custom file location
   */
  @Test
  public void testReadStringJsonDocument() throws IOException
  {
    String jsonDocument = IOUtils.toString(getClass().getResource(ClassPathReferences.META_RESOURCE_SCHEMA_JSON),
                                           StandardCharsets.UTF_8);
    JsonNode jsonNode = JsonHelper.readJsonDocument(jsonDocument);
    Assertions.assertNotNull(jsonNode);
    Assertions.assertNotNull(jsonNode.get("id"));
    log.trace("\"id\": \"{}\"", jsonNode.get("id").asText());
  }

  /**
   * will test that attributes can be correctly read from a json document with the helper method
   */
  @Test
  public void readDifferentAttributesFromJson() throws IOException
  {
    String jsonDocument = IOUtils.toString(getClass().getResource(ClassPathReferences.META_RESOURCE_SCHEMA_JSON),
                                           StandardCharsets.UTF_8);
    JsonNode jsonNode = JsonHelper.readJsonDocument(jsonDocument);

    Assertions.assertFalse(JsonHelper.getSimpleAttribute(jsonNode, "unknown", String.class).isPresent());
    Assertions.assertEquals(SchemaUris.SCHEMA_URI, JsonHelper.getSimpleAttribute(jsonNode, "id", String.class).get());
    JsonNode attributes = jsonNode.get("attributes");
    Assertions.assertTrue(attributes.isArray());
    JsonNode firstAttribute = attributes.get(0);
    Assertions.assertEquals("id", JsonHelper.getSimpleAttribute(firstAttribute, "name", String.class).get());
    Assertions.assertEquals(false, JsonHelper.getSimpleAttribute(firstAttribute, "multiValued", Boolean.class).get());
    Assertions.assertEquals(true, JsonHelper.getSimpleAttribute(firstAttribute, "required", Boolean.class).get());

    ((ObjectNode)firstAttribute).put("myInt", 5);
    Assertions.assertEquals(5, JsonHelper.getSimpleAttribute(firstAttribute, "myInt", Integer.class).get());
    Assertions.assertEquals(5L, JsonHelper.getSimpleAttribute(firstAttribute, "myInt", Long.class).get());
    Assertions.assertEquals("5", JsonHelper.getSimpleAttribute(firstAttribute, "myInt", String.class).get());

    ((ObjectNode)firstAttribute).put("myDouble", 10.0);
    Assertions.assertEquals(10.0, JsonHelper.getSimpleAttribute(firstAttribute, "myDouble", Double.class).get());
    Assertions.assertEquals("10.0", JsonHelper.getSimpleAttribute(firstAttribute, "myDouble", String.class).get());

    ((ObjectNode)firstAttribute).put("myDouble", "10.0");
    Assertions.assertEquals(10.0f, JsonHelper.getSimpleAttribute(firstAttribute, "myDouble", Float.class).get());
    Assertions.assertEquals("10.0", JsonHelper.getSimpleAttribute(firstAttribute, "myDouble", String.class).get());

    ((ObjectNode)firstAttribute).put("null", (String)null);
    Assertions.assertFalse(JsonHelper.getSimpleAttribute(firstAttribute, "null", String.class).isPresent());

    Assertions.assertThrows(IncompatibleAttributeException.class,
                            () -> JsonHelper.getSimpleAttribute(jsonNode, "attributes", String.class));

    Assertions.assertThrows(IncompatibleAttributeException.class,
                            () -> JsonHelper.getSimpleAttribute(jsonNode, "id", Object.class));
  }

  @Test
  public void testGetArrayAttribute()
  {
    JsonNode jsonNode = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON);
    Assertions.assertTrue(JsonHelper.getArrayAttribute(jsonNode, "attributes").isPresent());
    Assertions.assertFalse(JsonHelper.getArrayAttribute(jsonNode, "unknown").isPresent());
    Assertions.assertThrows(IncompatibleAttributeException.class, () -> JsonHelper.getArrayAttribute(jsonNode, "id"));
  }
}
