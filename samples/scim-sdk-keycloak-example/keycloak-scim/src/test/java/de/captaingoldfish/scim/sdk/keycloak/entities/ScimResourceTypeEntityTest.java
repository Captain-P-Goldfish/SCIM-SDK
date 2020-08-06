package de.captaingoldfish.scim.sdk.keycloak.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakTest;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Slf4j
public class ScimResourceTypeEntityTest extends KeycloakTest
{

  /**
   * verifies that default of enabled is true
   */
  @Test
  public void testEnabled()
  {
    ScimResourceTypeEntity resourceType = ScimResourceTypeEntity.builder().build();
    Assertions.assertTrue(resourceType.isEnabled());

    resourceType = new ScimResourceTypeEntity();
    Assertions.assertTrue(resourceType.isEnabled());
  }

  /**
   * verifies that default of requiresAuthentication is true
   */
  @Test
  public void testRequireAuthentication()
  {
    ScimResourceTypeEntity resourceType = ScimResourceTypeEntity.builder().build();
    Assertions.assertTrue(resourceType.isRequireAuthentication());

    resourceType = new ScimResourceTypeEntity();
    Assertions.assertTrue(resourceType.isRequireAuthentication());
  }

  /**
   * verifies that a resource type can be created and persisted in the database
   */
  @Test
  public void testScimResourceTypeEntityTest()
  {
    ScimResourceTypeEntity resourceType = ScimResourceTypeEntity.builder()
                                                                .realmId(getRealmModel().getId())
                                                                .name("User")
                                                                .description("User Account")
                                                                .enabled(false)
                                                                .singletonEndpoint(false)
                                                                .autoFiltering(true)
                                                                .autoSorting(true)
                                                                .disableCreate(true)
                                                                .disableGet(true)
                                                                .disableUpdate(true)
                                                                .disableDelete(true)
                                                                .requireAuthentication(false)
                                                                .build();
    persist(resourceType);
    Assertions.assertEquals(1, countEntriesInTable(ScimResourceTypeEntity.class));
  }
}
