package de.captaingoldfish.scim.sdk.server.schemas.custom;

import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * a complex type configuration for eTags that describes how the automatic API handling should use ETags<br>
 * <br>
 * created at: 30.05.2020
 *
 * @author Pascal Knüppel
 */
@NoArgsConstructor
public class ETagFeature extends ScimObjectNode
{

  @Builder
  public ETagFeature(Boolean enabled)
  {
    setEnabled(Optional.ofNullable(enabled).orElse(false));
  }

  /**
   * a boolean if set to false ETags will not be generated automatically on this resource endpoint. Default is
   * false.
   */
  public boolean isEnabled()
  {
    return getBooleanAttribute(AttributeNames.Custom.ETAG_ENABLED).orElse(false);
  }

  /**
   * a boolean if set to false ETags will not be generated automatically on this resource endpoint. Default is
   * true.
   */
  public void setEnabled(boolean enabled)
  {
    setAttribute(AttributeNames.Custom.ETAG_ENABLED, enabled);
  }
}
