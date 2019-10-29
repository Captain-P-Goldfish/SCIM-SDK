package de.gold.scim.patch;

import java.util.Objects;

import de.gold.scim.exceptions.NotImplementedException;
import de.gold.scim.request.PatchOpRequest;
import de.gold.scim.request.PatchRequestOperation;
import de.gold.scim.resources.ResourceNode;
import de.gold.scim.schemas.ResourceType;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 09:40 <br>
 * <br>
 * this class is used to resolve patch operations on resources
 */
public class PatchHandler
{

  /**
   * this resource type is used to get the attribute definitions of the values from the patch operations
   */
  private final ResourceType resourceType;

  public PatchHandler(ResourceType resourceType)
  {
    this.resourceType = Objects.requireNonNull(resourceType);
  }

  /**
   * this method will execute the patch operation on the given resource
   *
   * @param resource the resource representation that should be patched
   * @param patchOpRequest the patch operation that should be executed on the resource
   * @return the patched resource
   */
  public ResourceNode patchResource(ResourceNode resource, PatchOpRequest patchOpRequest)
  {
    for ( PatchRequestOperation operation : patchOpRequest.getOperations() )
    {
      switch (operation.getOp())
      {
        case ADD:
          addValues(resource, operation);
          break;
        case REPLACE:
          replaceValues(resource, operation);
          break;
        case REMOVE:
          removeValues(resource, operation);
          break;
      }
    }
    return resource;
  }

  /**
   * will add the given attributes to the given resource
   *
   * @param resource the resource to which the attributes should be added
   * @param operation the operation request that contains the new attributes
   */
  private void addValues(ResourceNode resource, PatchRequestOperation operation)
  {

  }

  /**
   * verifies that the add operation is correctly build and throws an exception if there are errors found
   *
   * @param operation the operation to validate
   */
  private void validateAddOperation(PatchRequestOperation operation)
  {

  }

  private void removeValues(ResourceNode resource, PatchRequestOperation operation)
  {
    throw new NotImplementedException("not yet");
  }

  private void replaceValues(ResourceNode resource, PatchRequestOperation operation)
  {
    throw new NotImplementedException("not yet");
  }

}
