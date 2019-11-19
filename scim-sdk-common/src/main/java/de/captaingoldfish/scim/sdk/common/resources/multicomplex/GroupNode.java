package de.captaingoldfish.scim.sdk.common.resources.multicomplex;

import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:15 <br>
 * <br>
 * A list of groups to which the user belongs, either through direct membership, through nested groups, or
 * dynamically calculated. The values are meant to enable expression of common group-based or role-based
 * access control models, although no explicit authorization model is defined. It is intended that the
 * semantics of group membership and any behavior or authorization granted as a result of membership are
 * defined by the service provider. The canonical types "direct" and "indirect" are defined to describe how
 * the group membership was derived. Direct group membership indicates that the user is directly associated
 * with the group and SHOULD indicate that clients may modify membership through the "Group" resource.
 * Indirect membership indicates that user membership is transitive or dynamic and implies that clients cannot
 * modify indirect group membership through the "Group" resource but MAY modify direct group membership
 * through the "Group" resource, which may influence indirect memberships. If the SCIM service provider
 * exposes a "Group" resource, the "value" sub-attribute MUST be the "id", and the "$ref" sub-attribute must
 * be the URI of the corresponding "Group" resources to which the user belongs. Since this attribute has a
 * mutability of "readOnly", group membership changes MUST be applied via the "Group" Resource (Section 4.2).
 * This attribute has a mutability of "readOnly".
 */
@NoArgsConstructor
public class GroupNode extends MultiComplexNode
{

  @Builder
  public GroupNode(String type, Boolean primary, String display, String value, String ref)
  {
    super(type, primary, display, value, ref);
  }
}
