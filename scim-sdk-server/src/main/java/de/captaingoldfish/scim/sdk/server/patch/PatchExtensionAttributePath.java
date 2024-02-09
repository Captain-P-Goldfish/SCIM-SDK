package de.captaingoldfish.scim.sdk.server.patch;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;


/**
 * a workaround class that is used for path-attributes that will directly address an extension
 *
 * @author Pascal Knueppel
 * @since 05.09.2022
 */
public class PatchExtensionAttributePath extends AttributePathRoot
{

  /**
   * the extension to be removed
   */
  private final Schema extensionSchema;

  public PatchExtensionAttributePath(Schema extensionSchema)
  {
    super();
    this.extensionSchema = extensionSchema;
  }

  @Override
  public String getFullName()
  {
    return extensionSchema.getNonNullId();
  }
}
