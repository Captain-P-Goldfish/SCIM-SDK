package de.captaingoldfish.scim.sdk.server.custom.resources;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import lombok.Builder;


/**
 * BulkIdReferences
 */
public class BulkIdReferences extends ResourceNode
{

  public BulkIdReferences()
  {
    setSchemas(Arrays.asList(FieldNames.SCHEMA));
  }

  @Builder
  public BulkIdReferences(String id,
                          Meta meta,
                          String userId,
                          List<String> userIdList,
                          Member member,
                          List<MemberList> memberList)
  {
    setSchemas(Arrays.asList(FieldNames.SCHEMA));
    setId(id);
    setMeta(meta);
    setUserId(userId);
    setUserIdList(userIdList);
    setMember(member);
    setMemberList(memberList);
  }

  /**
   * a simple user reference.
   */
  public Optional<String> getUserId()
  {
    return getStringAttribute(FieldNames.USERID);

  }

  /**
   * a simple user reference.
   */
  public void setUserId(String userId)
  {
    setAttribute(FieldNames.USERID, userId);
  }

  /**
   * a simple multivalued user reference.
   */
  public List<String> getUserIdList()
  {
    return getSimpleArrayAttribute(FieldNames.USERIDLIST);
  }

  /**
   * a simple multivalued user reference.
   */
  public void setUserIdList(List<String> userIdList)
  {
    setStringAttributeList(FieldNames.USERIDLIST, userIdList);
  }

  /**
   * A list of members of the Group.
   */
  public Optional<Member> getMember()
  {

    return getObjectAttribute(FieldNames.MEMBER, Member.class);

  }

  /**
   * A list of members of the Group.
   */
  public void setMember(Member member)
  {
    setAttribute(FieldNames.MEMBER, member);
  }

  /**
   * A list of members of the Group.
   */
  public List<MemberList> getMemberList()
  {
    return getArrayAttribute(FieldNames.MEMBERLIST, MemberList.class);
  }

  /**
   * A list of members of the Group.
   */
  public void setMemberList(List<MemberList> memberList)
  {
    setAttribute(FieldNames.MEMBERLIST, memberList);
  }


  /**
   * A list of members of the Group.
   */
  public static class Member extends ScimObjectNode
  {

    public Member()
    {}

    @Builder
    public Member(String userId, List<String> userIdList)
    {
      setUserId(userId);
      setUserIdList(userIdList);
    }

    /**
     * a single simple nested user reference.
     */
    public Optional<String> getUserId()
    {
      return getStringAttribute(FieldNames.USERID);

    }

    /**
     * a single simple nested user reference.
     */
    public void setUserId(String groupId)
    {
      setAttribute(FieldNames.USERID, groupId);
    }

    /**
     * a single simple nested multivalued Group reference.
     */
    public List<String> getUserIdList()
    {
      return getSimpleArrayAttribute(FieldNames.USERIDLIST);

    }

    /**
     * a single simple nested multivalued Group reference.
     */
    public void setUserIdList(List<String> userIdList)
    {
      setStringAttributeList(FieldNames.USERIDLIST, userIdList);
    }

  }

  /**
   * A list of members of the Group.
   */
  public static class MemberList extends ScimObjectNode
  {

    public MemberList()
    {}

    @Builder
    public MemberList(String groupId, List<String> userIdList)
    {
      setGroupId(groupId);
      setGroupIdList(userIdList);
    }

    /**
     * a multivalued simple nested group reference.
     */
    public Optional<String> getGroupId()
    {
      return getStringAttribute(FieldNames.GROUPID);

    }

    /**
     * a multivalued simple nested group reference.
     */
    public void setGroupId(String groupId)
    {
      setAttribute(FieldNames.GROUPID, groupId);
    }

    /**
     * a multivalued simple nested multivalued Group reference.
     */
    public List<String> getGroupIdList()
    {
      return getSimpleArrayAttribute(FieldNames.GROUPIDLIST);

    }

    /**
     * a multivalued simple nested multivalued Group reference.
     */
    public void setGroupIdList(List<String> userIdList)
    {
      setStringAttributeList(FieldNames.GROUPIDLIST, userIdList);
    }

  }

  /**
   * contains the attribute names of the resource representation
   */
  public static class FieldNames
  {

    public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:BulkIdReferences";


    public static final String USERID = "userId";

    public static final String USERIDLIST = "userIdList";

    public static final String GROUPIDLIST = "groupIdList";

    public static final String MEMBER = "member";

    public static final String GROUPID = "groupId";

    public static final String MEMBERLIST = "memberList";

  }
}
