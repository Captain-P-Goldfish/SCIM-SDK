package de.gold.scim.server.resources.multicomplex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:54 <br>
 * <br>
 */
public class MemberTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    Member instance = Assertions.assertDoesNotThrow(() -> Member.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new Member().isEmpty());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributes()
  {
    final String value = UUID.randomUUID().toString();
    final String display = UUID.randomUUID().toString();
    final boolean primary = true;
    final String type = UUID.randomUUID().toString();
    final String ref = UUID.randomUUID().toString();
    Member member = Member.builder().value(value).display(display).primary(primary).type(type).ref(ref).build();
    Assertions.assertEquals(value, member.getValue().get());
    Assertions.assertEquals(display, member.getDisplay().get());
    Assertions.assertEquals(primary, member.isPrimary());
    Assertions.assertEquals(type, member.getType().get());
    Assertions.assertEquals(ref, member.getRef().get());
  }
}
