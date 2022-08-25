package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

/**
 * @author Pascal Knueppel
 * @since 22.08.2022
 */
public interface BulkIdReferenceWrapper
{

  /**
   * @return the bulkId that represents this wrapper
   */
  public String getBulkId();

  /**
   * will replace the valueNode with a new node that contains the new value
   *
   * @param newValue the new value to add into the parent node
   */
  public void replaceValueNode(String newValue);

}
