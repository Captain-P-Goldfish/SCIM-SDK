package de.gold.scim.schemas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 17:06 <br>
 * <br>
 */
class SchemaFactoryTest
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
    Assertions.assertNotNull(SchemaFactory.getMetaSchema(schemaId));
  }

  /**
   * this test will assure that the default schemata will be read correctly from the classpath
   */
  @ParameterizedTest
  @ValueSource(strings = {"urn:ietf:params:scim:schemas:core:2.0:User", "urn:ietf:params:scim:schemas:core:2.0:Group"})
  public void testGetDefaultResourceSchemata(String schemaId)
  {
    Assertions.assertNotNull(SchemaFactory.getResourceSchema(schemaId));
  }
}
