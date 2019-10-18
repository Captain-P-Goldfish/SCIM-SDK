package de.gold.scim.resources.complex;

import java.util.Optional;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 11:12 <br>
 * <br>
 * A complex type that specifies configuration options related to changing a password. REQUIRED.
 */
public class ChangePasswordConfig extends ScimObjectNode
{

  @Builder
  public ChangePasswordConfig(Boolean supported)
  {
    super(null);
    setSupported(supported);
  }

  /**
   * A Boolean value specifying whether or not the operation is supported. REQUIRED.
   */
  public boolean isSupported()
  {
    return getBooleanAttribute(AttributeNames.SUPPORTED).orElse(false);
  }

  /**
   * A Boolean value specifying whether or not the operation is supported. REQUIRED.
   */
  public void setSupported(Boolean supported)
  {
    setAttribute(AttributeNames.SUPPORTED, Optional.ofNullable(supported).orElse(false));
  }
}
