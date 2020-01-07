package de.captaingoldfish.scim.sdk.common.resources.multicomplex;

import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:15 <br>
 * <br>
 * A list of entitlements for the user that represent a thing the user has. An entitlement may be an
 * additional right to a thing, object, or service. No vocabulary or syntax is specified; service providers
 * and clients are expected to encode sufficient information in the value so as to accurately and without
 * ambiguity determine what the user has access to. This value has no canonical types, although a type may be
 * useful as a means to scope entitlements.
 */
@NoArgsConstructor
public class Entitlement extends MultiComplexNode
{

  @Builder
  public Entitlement(String type, Boolean primary, String display, String value, String ref)
  {
    super(type, primary, display, value, ref);
  }
}
