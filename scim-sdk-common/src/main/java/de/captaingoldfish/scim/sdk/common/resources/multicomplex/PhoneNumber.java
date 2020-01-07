package de.captaingoldfish.scim.sdk.common.resources.multicomplex;

import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:13 <br>
 * <br>
 * Phone numbers for the user. The value SHOULD be specified according to the format defined in [RFC3966],
 * e.g., 'tel:+1-201-555-0123'. Service providers SHOULD canonicalize the value according to [RFC3966] format,
 * when appropriate. The "display" sub-attribute MAY be used to return the canonicalized representation of the
 * phone number value. The sub-attribute "type" often has typical values of "work", "home", "mobile", "fax",
 * "pager", and "other" and MAY allow more types to be defined by the SCIM clients.
 */
@NoArgsConstructor
public class PhoneNumber extends MultiComplexNode
{

  @Builder
  public PhoneNumber(String type, Boolean primary, String display, String value, String ref)
  {
    super(type, primary, display, value, ref);
  }
}
