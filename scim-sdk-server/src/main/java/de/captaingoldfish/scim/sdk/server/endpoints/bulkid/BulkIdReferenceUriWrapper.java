package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import de.captaingoldfish.scim.sdk.server.utils.UriInfos;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 25.08.2022
 */
@RequiredArgsConstructor
public class BulkIdReferenceUriWrapper implements BulkIdReferenceWrapper
{

  private final UriInfos uriInfos;

  @Getter
  private final String bulkId;

  @Override
  public void replaceValueNode(String newValue)
  {
    uriInfos.setResourceId(newValue);
  }
}
