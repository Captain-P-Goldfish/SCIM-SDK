package de.gold.scim.resources.multicomplex;

import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:13 <br>
 * <br>
 * Instant messaging address for the user. No official canonicalization rules exist for all instant messaging
 * addresses, but service providers SHOULD, when appropriate, remove all whitespace and convert the address to
 * lowercase. The "type" sub-attribute SHOULD take one of the following values: "aim", "gtalk", "icq", "xmpp",
 * "msn", "skype", "qq", "yahoo", or "other" (representing currently popular IM services at the time of this
 * writing). Service providers MAY add further values if new IM services are introduced and MAY specify more
 * detailed canonicalization rules for each possible value.
 */
@NoArgsConstructor
public class Ims extends MultiComplexNode
{

  @Builder
  public Ims(String type, Boolean primary, String display, String value, String ref)
  {
    super(type, primary, display, value, ref);
  }
}
