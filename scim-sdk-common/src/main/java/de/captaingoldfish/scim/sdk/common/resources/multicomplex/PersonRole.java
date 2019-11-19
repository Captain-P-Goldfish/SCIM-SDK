package de.captaingoldfish.scim.sdk.common.resources.multicomplex;

import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:15 <br>
 * <br>
 * A list of roles for the user that collectively represent who the user is, e.g., "Student", "Faculty". No
 * vocabulary or syntax is specified, although it is expected that a role value is a String or label
 * representing a collection of entitlements. This value has no canonical types.
 */
@NoArgsConstructor
public class PersonRole extends MultiComplexNode
{

  @Builder
  public PersonRole(String type, Boolean primary, String display, String value, String ref)
  {
    super(type, primary, display, value, ref);
  }
}
