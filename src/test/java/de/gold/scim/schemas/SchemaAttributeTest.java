package de.gold.scim.schemas;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 14:18 <br>
 * <br>
 */
class SchemaAttributeTest
{

  @Test
  public void testReadSchema()
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON);
  }
}
