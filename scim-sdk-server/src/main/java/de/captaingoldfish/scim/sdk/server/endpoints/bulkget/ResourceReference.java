package de.captaingoldfish.scim.sdk.server.endpoints.bulkget;

import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;


/**
 * @author Pascal Knueppel
 * @since 30.08.2022
 */
interface ResourceReference
{

  /**
   * @return true if enough data is present to retrieve the resource if possible, false else
   */
  public boolean isResourceRetrievable();

  /**
   * the id of the resource to be retrieved
   */
  public String getResourceId();

  /**
   * the referenced resource-type. If this information is not available the API will return an error message for
   * this resource. An error will not result in an aborted operation instead the client will simply be informed
   * that this resource was not retrievable.
   */
  public ResourceType getResourceType();

  /**
   * the nodePath from the resource that points to this element. E.g. for Users
   * "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager" or for Groups "members". When using
   * the custom-feature to set resource references a reference might also access a simple reference type
   * "userId" for flat types or "tenant.id" for complex types
   */
  public String getNodePath();

}
