package de.gold.scim.server.resources.complex;

import java.util.Optional;

import de.gold.scim.server.constants.AttributeNames;
import de.gold.scim.server.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 11:12 <br>
 * <br>
 * A complex type that specifies ETag configuration options. REQUIRED.
 */
public class ETagConfig extends ScimObjectNode
{

  @Builder
  public ETagConfig(Boolean supported)
  {
    super(null);
    setSupported(supported);
  }

  /**
   * A Boolean value specifying whether or not the operation is supported. REQUIRED.
   */
  public boolean isSupported()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.SUPPORTED).orElse(false);
  }

  /**
   * A Boolean value specifying whether or not the operation is supported. REQUIRED.
   */
  public void setSupported(Boolean supported)
  {
    setAttribute(AttributeNames.RFC7643.SUPPORTED, Optional.ofNullable(supported).orElse(false));
  }
}
