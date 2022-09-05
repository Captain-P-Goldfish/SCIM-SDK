package de.captaingoldfish.scim.sdk.server.patch;

import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import lombok.Getter;


/**
 * a workaround class that is used for path-attributes that will directly address an extension
 *
 * @author Pascal Knueppel
 * @since 05.09.2022
 */
class PatchExtensionAttributePath extends AttributePathRoot
{

  /**
   * the path that addresses an extension directly with its id value
   */
  @Getter
  private final String path;

  public PatchExtensionAttributePath(String path)
  {
    super();
    this.path = path;
  }

  @Override
  public String getFullName()
  {
    return path;
  }
}
