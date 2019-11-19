package de.gold.scim.common.constants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 15:31 <br>
 * <br>
 */
@Slf4j
public class SchemaUrisTest
{

  /**
   * will validate that the core uri gets not changed by accident
   */
  @Test
  @DisplayName("uri must be: urn:ietf:params:scim:schemas:core:2.0:")
  public void testCoreUri()
  {
    Assertions.assertEquals("urn:ietf:params:scim:schemas:core:2.0:", SchemaUris.SCIM_CORE_URI);
  }

  /**
   * will validate that the message uri gets not changed by accident
   */
  @Test
  @DisplayName("uri must be: urn:ietf:params:scim:api:messages:2.0:")
  public void testMessageUri()
  {
    Assertions.assertEquals("urn:ietf:params:scim:api:messages:2.0:", SchemaUris.SCIM_MESSAGES_URI);
  }

  /**
   * this test will verify that the schema uris in the constants class are using the correct values
   */
  @Test
  public void testCheckUrisDefinedByScim()
  {
    Assertions.assertEquals("urn:ietf:params:scim:schemas:core:2.0:User", SchemaUris.USER_URI);
    Assertions.assertEquals("urn:ietf:params:scim:schemas:core:2.0:Group", SchemaUris.GROUP_URI);
    Assertions.assertEquals("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                            SchemaUris.ENTERPRISE_USER_URI);
    Assertions.assertEquals("urn:ietf:params:scim:schemas:core:2.0:ResourceType", SchemaUris.RESOURCE_TYPE_URI);
    Assertions.assertEquals("urn:ietf:params:scim:api:messages:2.0:SearchRequest", SchemaUris.SEARCH_REQUEST_URI);
    Assertions.assertEquals("urn:ietf:params:scim:api:messages:2.0:Error", SchemaUris.ERROR_URI);
    Assertions.assertEquals("urn:ietf:params:scim:schemas:core:2.0:Schema", SchemaUris.SCHEMA_URI);
    Assertions.assertEquals("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig",
                            SchemaUris.SERVICE_PROVIDER_CONFIG_URI);
    Assertions.assertEquals("urn:ietf:params:scim:api:messages:2.0:ListResponse", SchemaUris.LIST_RESPONSE_URI);
    Assertions.assertEquals("urn:ietf:params:scim:api:messages:2.0:BulkRequest", SchemaUris.BULK_REQUEST_URI);
    Assertions.assertEquals("urn:ietf:params:scim:api:messages:2.0:BulkResponse", SchemaUris.BULK_RESPONSE_URI);
    Assertions.assertEquals("urn:ietf:params:scim:api:messages:2.0:PatchOp", SchemaUris.PATCH_OP);

  }

  /**
   * this test will verify that the custom uris that were defined for this implementation do have the correct
   * values
   */
  @Test
  public void testCheckUrisNotDefinedByScim()
  {
    Assertions.assertEquals("urn:ietf:params:scim:schemas:core:2.0:Meta", SchemaUris.META);
  }
}
