package de.captaingoldfish.scim.sdk.common.resources;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import lombok.Builder;


// @formatter:off
/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 23:22 <br>
 * <br>
 * SCIM provides a schema for representing groups, identified using the
 * following schema URI: "urn:ietf:params:scim:schemas:core:2.0:Group".
 *
 * "Group" resources are meant to enable expression of common
 * group-based or role-based access control models, although no explicit
 * authorization model is defined.  It is intended that the semantics of
 * group membership, and any behavior or authorization granted as a
 * result of membership, are defined by the service provider; these are
 * considered out of scope for this specification.
 */
// @formatter:on
public class Group extends ResourceNode
{

  public Group()
  {
    setSchemas(Arrays.asList(SchemaUris.GROUP_URI));
  }

  @Builder
  public Group(String id, String externalId, String displayName, List<Member> members, Meta meta)
  {
    this();
    setId(id);
    setExternalId(externalId);
    setDisplayName(displayName);
    setMembers(members);
    setMeta(meta);
  }

  /**
   * A human-readable name for the Group. REQUIRED.
   */
  public Optional<String> getDisplayName()
  {
    return getStringAttribute(AttributeNames.RFC7643.DISPLAY_NAME);
  }

  /**
   * A human-readable name for the Group. REQUIRED.
   */
  public void setDisplayName(String displayName)
  {
    setAttribute(AttributeNames.RFC7643.DISPLAY_NAME, displayName);
  }

  /**
   * A list of members of the Group. While values MAY be added or removed, sub-attributes of members are
   * "immutable". The "value" sub-attribute contains the value of an "id" attribute of a SCIM resource, and the
   * "$ref" sub-attribute must be the URI of a SCIM resource such as a "User", or a "Group". The intention of
   * the "Group" type is to allow the service provider to support nested groups. Service providers MAY require
   * clients to provide a non-empty value by setting the "required" attribute characteristic of a sub-attribute
   * of the "members" attribute in the "Group" resource schema.
   */
  public List<Member> getMembers()
  {
    return getArrayAttribute(AttributeNames.RFC7643.MEMBERS, Member.class);
  }

  /**
   * A list of members of the Group. While values MAY be added or removed, sub-attributes of members are
   * "immutable". The "value" sub-attribute contains the value of an "id" attribute of a SCIM resource, and the
   * "$ref" sub-attribute must be the URI of a SCIM resource such as a "User", or a "Group". The intention of
   * the "Group" type is to allow the service provider to support nested groups. Service providers MAY require
   * clients to provide a non-empty value by setting the "required" attribute characteristic of a sub-attribute
   * of the "members" attribute in the "Group" resource schema.
   */
  public void setMembers(List<Member> members)
  {
    setAttribute(AttributeNames.RFC7643.MEMBERS, members);
  }

  /**
   * A list of members of the Group. While values MAY be added or removed, sub-attributes of members are
   * "immutable". The "value" sub-attribute contains the value of an "id" attribute of a SCIM resource, and the
   * "$ref" sub-attribute must be the URI of a SCIM resource such as a "User", or a "Group". The intention of
   * the "Group" type is to allow the service provider to support nested groups. Service providers MAY require
   * clients to provide a non-empty value by setting the "required" attribute characteristic of a sub-attribute
   * of the "members" attribute in the "Group" resource schema.
   */
  public void addMember(Member member)
  {
    addAttribute(AttributeNames.RFC7643.MEMBERS, member);
  }
}
