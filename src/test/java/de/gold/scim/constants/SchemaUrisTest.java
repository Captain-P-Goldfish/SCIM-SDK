package de.gold.scim.constants;

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
}
