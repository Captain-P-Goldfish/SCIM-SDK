package de.gold.scim.server.resources.complex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:39 <br>
 * <br>
 */
public class NameTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    Name instance = Assertions.assertDoesNotThrow(() -> Name.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new Name().isEmpty());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributes()
  {
    final String formatted = UUID.randomUUID().toString();
    final String familyName = UUID.randomUUID().toString();
    final String givenName = UUID.randomUUID().toString();
    final String middleName = UUID.randomUUID().toString();
    final String honorificPrefix = UUID.randomUUID().toString();
    final String honorificSuffix = UUID.randomUUID().toString();
    Name name = Name.builder()
                    .formatted(formatted)
                    .familyName(familyName)
                    .givenName(givenName)
                    .middlename(middleName)
                    .honorificPrefix(honorificPrefix)
                    .honorificSuffix(honorificSuffix)
                    .build();
    Assertions.assertEquals(formatted, name.getFormatted().get());
    Assertions.assertEquals(familyName, name.getFamilyName().get());
    Assertions.assertEquals(givenName, name.getGivenName().get());
    Assertions.assertEquals(middleName, name.getMiddleName().get());
    Assertions.assertEquals(honorificPrefix, name.getHonorificPrefix().get());
    Assertions.assertEquals(honorificSuffix, name.getHonorificSuffix().get());
  }
}
