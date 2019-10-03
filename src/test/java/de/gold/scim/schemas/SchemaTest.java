package de.gold.scim.schemas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 18:02 <br>
 * <br>
 */
@Slf4j
class SchemaTest
{

  /**
   * this test will assure that the default schemata will be read correctly from the classpath
   */
  @ParameterizedTest
  @ValueSource(strings = {"urn:ietf:params:scim:schemas:core:2.0:Schema",
                          "urn:ietf:params:scim:schemas:core:2.0:ResourceType",
                          "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"})
  public void testGetDefaultMetaSchemata(String schemaId)
  {
    String json = SchemaFactory.getMetaSchema(schemaId).toString();
    log.warn(json);
    JsonNode jsonNode = JsonHelper.readJsonDocument(json);
    Assertions.assertFalse(jsonNode.get(AttributeNames.ATTRIBUTES).isObject());
  }

  /**
   * this test will assure that the default schemata will be read correctly from the classpath
   */
  @ParameterizedTest
  @ValueSource(strings = {"urn:ietf:params:scim:schemas:core:2.0:User", "urn:ietf:params:scim:schemas:core:2.0:Group"})
  public void testGetDefaultResourceSchemata(String schemaId)
  {
    String json = SchemaFactory.getResourceSchema(schemaId).toString();
    log.warn(json);
    JsonNode jsonNode = JsonHelper.readJsonDocument(json);
    Assertions.assertFalse(jsonNode.get(AttributeNames.ATTRIBUTES).isTextual());
  }
}
