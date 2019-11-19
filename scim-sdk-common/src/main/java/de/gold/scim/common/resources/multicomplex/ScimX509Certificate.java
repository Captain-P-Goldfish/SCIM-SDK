package de.gold.scim.common.resources.multicomplex;

import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:16 <br>
 * <br>
 * A list of certificates associated with the resource (e.g., a User). Each value contains exactly one
 * DER-encoded X.509 certificate (see Section 4 of [RFC5280]), which MUST be base64 encoded per Section 4 of
 * [RFC4648]. A single value MUST NOT contain multiple certificates and so does not contain the encoding
 * "SEQUENCE OF Certificate" in any guise.
 */
@NoArgsConstructor
public class ScimX509Certificate extends MultiComplexNode
{

  @Builder
  public ScimX509Certificate(String type, Boolean primary, String display, String value, String ref)
  {
    super(type, primary, display, value, ref);
  }
}
