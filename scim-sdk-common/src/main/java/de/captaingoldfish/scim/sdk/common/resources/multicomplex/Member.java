package de.captaingoldfish.scim.sdk.common.resources.multicomplex;

import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:42 <br>
 * <br>
 * A list of members of the Group. While values MAY be added or removed, sub-attributes of members are
 * "immutable". The "value" sub-attribute contains the value of an "id" attribute of a SCIM resource, and the
 * "$ref" sub-attribute must be the URI of a SCIM resource such as a "User", or a "Group". The intention of
 * the "Group" type is to allow the service provider to support nested groups. Service providers MAY require
 * clients to provide a non-empty value by setting the "required" attribute characteristic of a sub-attribute
 * of the "members" attribute in the "Group" resource schema.
 */
@NoArgsConstructor
public class Member extends MultiComplexNode
{

  @Builder
  public Member(String type, Boolean primary, String display, String value, String ref)
  {
    super(type, primary, display, value, ref);
  }

  /**
   * override lombok builder with public constructor
   */
  public static class MemberBuilder
  {

    public MemberBuilder()
    {}
  }
}
