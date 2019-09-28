package de.gold.scim.schemas.meta;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.exceptions.InvalidSchemaException;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 16:20 <br>
 * <br>
 */
@Slf4j
public class SchemaDefinitionTest
{

  /**
   * the meta schema gets modified in the following tests and therefore it must be reloaded after each test
   */
  @AfterEach
  public void reloadDefaultSchema()
  {
    SchemaDefinition schemaDefinition = SchemaDefinition.getInstance();
    schemaDefinition.loadCustomSchemaDefinition(ClassPathReferences.META_SCHEMA_JSON);
  }

  /**
   * will verify that the test will fail if any of the given attributes is missing
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.ID, AttributeNames.NAME, AttributeNames.DESCRIPTION,
                          AttributeNames.ATTRIBUTES})
  public void testSchemaValidationFailsOnMissingAttribute(String attributeName)
  {
    SchemaDefinition schemaDefinition = SchemaDefinition.getInstance();
    ((ObjectNode)schemaDefinition.getSchemaDocument()).remove(attributeName);
    Assertions.assertThrows(InvalidSchemaException.class, schemaDefinition::validateCustomSchema);
  }

  /**
   * will verify that the test will fail if any of the given attributes from the "attributes"-attribute is
   * missing
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.NAME, AttributeNames.DESCRIPTION, AttributeNames.TYPE})
  public void testSchemaValidationFailsOnMissingAttributeInAttributes(String attributeName)
  {
    SchemaDefinition schemaDefinition = SchemaDefinition.getInstance();
    JsonNode attributes = JsonHelper.getArrayAttribute(schemaDefinition.getSchemaDocument(), AttributeNames.ATTRIBUTES)
                                    .orElseThrow(() -> new IllegalStateException());
    MatcherAssert.assertThat(attributes.size(), Matchers.greaterThan(0));
    attributes.forEach(attribute -> {
      ((ObjectNode)attribute).remove(attributeName);
    });
    Assertions.assertThrows(InvalidSchemaException.class, schemaDefinition::validateCustomSchema);
  }
}
