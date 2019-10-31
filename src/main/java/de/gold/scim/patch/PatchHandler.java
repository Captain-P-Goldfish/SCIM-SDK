package de.gold.scim.patch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.NotImplementedException;
import de.gold.scim.request.PatchOpRequest;
import de.gold.scim.request.PatchRequestOperation;
import de.gold.scim.resources.ResourceNode;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 09:40 <br>
 * <br>
 * this class is used to resolve patch operations on resources
 */
@Slf4j
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
  public <T extends ResourceNode> T patchResource(T resource, PatchOpRequest patchOpRequest)
  {
    AtomicBoolean changeWasMade = new AtomicBoolean(false);
    for ( PatchRequestOperation operation : patchOpRequest.getOperations() )
    {
      switch (operation.getOp())
      {
        case ADD:
        case REPLACE:
          changeWasMade.weakCompareAndSet(false, addOrReplaceValues(resource, operation));
          break;
        case REMOVE:
          changeWasMade.weakCompareAndSet(false, removeValues(resource, operation));
          break;
      }
    }
    setLastModified(resource, changeWasMade);
    return resource;
  }

  /**
   * will add the given attributes to the given resource
   *
   * @param resource the resource to which the attributes should be added
   * @param operation the operation request that contains the new attributes
   */
  private boolean addOrReplaceValues(ResourceNode resource, PatchRequestOperation operation)
  {
    Optional<String> target = operation.getPath();
    List<String> values = operation.getValues();
    if (values == null || values.isEmpty())
    {
      throw new BadRequestException("no value attributes present in patch operation", null,
                                    ScimType.RFC7644.INVALID_VALUE);
    }

    if (target.isPresent())
    {
      PatchTargetHandler patchAddToTarget = new PatchTargetHandler(resourceType, operation.getOp(), target.get());
      return patchAddToTarget.addOperationValues(resource, operation.getValues());
    }
    else
    {
      if (values.size() > 1)
      {
        throw new BadRequestException("too many resources set in patch operation. If the target is not specified only"
                                      + " a single value must be present in the values list which represents the "
                                      + "resource itself", null, ScimType.RFC7644.INVALID_VALUE);
      }
      PatchResourceHandler patchResourceHandler = new PatchResourceHandler(resourceType, operation.getOp());
      return patchResourceHandler.addResourceValues(resource, JsonHelper.readJsonDocument(values.get(0)), null);
    }
  }



  /**
   * overrides the lastModified value if a change was made
   *
   * @param resource the resource of which the lastModified value should be changed
   * @param changeWasMade if the lastModified value should be changed or not
   */
  private void setLastModified(ResourceNode resource, AtomicBoolean changeWasMade)
  {
    if (changeWasMade.get())
    {
      Optional<Meta> metaOptional = resource.getMeta();
      if (metaOptional.isPresent())
      {
        metaOptional.get().setLastModified(LocalDateTime.now());
      }
      else
      {
        Meta meta = Meta.builder().lastModified(LocalDateTime.now()).build();
        resource.setMeta(meta);
      }
    }
  }



  private boolean removeValues(ResourceNode resource, PatchRequestOperation operation)
  {
    throw new NotImplementedException("not yet");
  }

  private boolean replaceValues(ResourceNode resource, PatchRequestOperation operation)
  {
    throw new NotImplementedException("not yet");
  }

}
