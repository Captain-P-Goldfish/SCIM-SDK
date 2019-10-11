package de.gold.scim.resources.multicomplex;

import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:13 <br>
 * <br>
 * Email addresses for the User. The value SHOULD be specified according to [RFC5321]. Service providers
 * SHOULD canonicalize the value according to [RFC5321], e.g., "bjensen@example.com" instead of
 * "bjensen@EXAMPLE.COM". The "display" sub-attribute MAY be used to return the canonicalized representation
 * of the email value. The "type" sub-attribute is used to provide a classification meaningful to the (human)
 * user. The user interface should encourage the use of basic values of "work", "home", and "other" and MAY
 * allow additional type values to be used at the discretion of SCIM clients.
 */
@NoArgsConstructor
public class Email extends MultiComplexNode
{

  @Builder
  public Email(String type, Boolean primary, String display, String value, String ref)
  {
    super(type, primary, display, value, ref);
  }
}
