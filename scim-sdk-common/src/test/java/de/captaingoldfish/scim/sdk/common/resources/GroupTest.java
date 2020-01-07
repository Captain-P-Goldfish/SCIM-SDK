package de.captaingoldfish.scim.sdk.common.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 00:10 <br>
 * <br>
 */
public class GroupTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    Group instance = Assertions.assertDoesNotThrow(() -> Group.builder().build());
    Assertions.assertFalse(instance.isEmpty());
    Assertions.assertEquals(1, instance.size());
    Assertions.assertEquals(1, instance.getSchemas().size());
    Assertions.assertEquals(SchemaUris.GROUP_URI, instance.getSchemas().iterator().next());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Group instance = new Group();
    Assertions.assertFalse(instance.isEmpty());
    Assertions.assertEquals(1, instance.size());
    Assertions.assertEquals(1, instance.getSchemas().size());
    Assertions.assertEquals(SchemaUris.GROUP_URI, instance.getSchemas().iterator().next());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributes()
  {
    final String displayName = UUID.randomUUID().toString();
    Member member = Member.builder().value(UUID.randomUUID().toString()).display(UUID.randomUUID().toString()).build();
    List<Member> memberList = new ArrayList<>();
    memberList.add(member);

    Group group = Group.builder().displayName(displayName).members(memberList).build();
    member = Member.builder().value(UUID.randomUUID().toString()).display(UUID.randomUUID().toString()).build();
    group.addMember(member);
    memberList.add(member);

    Assertions.assertEquals(displayName, group.getDisplayName().get());
    Assertions.assertEquals(memberList, group.getMembers());
  }
}
