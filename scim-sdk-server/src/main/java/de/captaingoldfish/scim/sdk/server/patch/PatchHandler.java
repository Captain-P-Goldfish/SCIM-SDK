package de.captaingoldfish.scim.sdk.server.patch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import lombok.Getter;
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

  /**
   * this object will hold the attributes that have been present within the operations. This is used to return
   * these attributes in requests in which the attributes parameter was present.
   */
  @Getter
  private final ScimObjectNode requestedAttributes;

  /**
   * this attribute tells us if the resource was effectively changed meaning that an attribute did receive a new
   * value that differs from the value before
   */
  @Getter
  private boolean changedResource;

  public PatchHandler(ResourceType resourceType)
  {
    this.requestedAttributes = new ScimObjectNode();
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
      changeWasMade.weakCompareAndSet(false, handlePatchOp(resource, operation));
    }
    setLastModified(resource, changeWasMade);
    changedResource = changeWasMade.get();
    return resource;
  }

  /**
   * adds the attributes to the {@link #requestedAttributes} object node to return the requested attributes
   * object. This is necessary for schema validation so that the changed attributes are returned on the response
   * if the attributes parameter was used in the request
   *
   * @param operation the operation that tells us if this is a remove operation or not
   * @param path the path expression that is necessary to determine the attributes that have been modified
   */
  private void setAttributeFromPath(PatchRequestOperation operation, AttributePathRoot path)
  {
    if (operation.getOp().equals(PatchOp.REMOVE))
    {
      // in this case the attribute is not present anymore so there is nothing to return
      return;
    }
    String fullName = path.getFullName() + (path.getSubAttributeName() == null ? "" : "." + path.getSubAttributeName());
    SchemaAttribute schemaAttribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, fullName);
    if (schemaAttribute.getParent() == null)
    {
      requestedAttributes.set(schemaAttribute.getName(), new TextNode(""));
    }
    else
    {
      ScimObjectNode objectNode = new ScimObjectNode();
      objectNode.set(schemaAttribute.getName(), new TextNode(""));
      requestedAttributes.set(schemaAttribute.getParent().getName(), objectNode);
    }
  }

  /**
   * adds the attributes to the {@link #requestedAttributes} object node to return the requested attributes
   * object. This is necessary for schema validation so that the changed attributes are returned on the response
   * if the attributes parameter was used in the request
   *
   * @param operation the operation that tells us which attributes were requested to change
   */
  private void setAttributesFromResource(PatchRequestOperation operation)
  {
    if (operation.getOp().equals(PatchOp.REMOVE))
    {
      // in this case the attribute is not present anymore so there is nothing to return
      return;
    }
    JsonNode resource = JsonHelper.readJsonDocument(operation.getValues().get(0));
    resource.fields().forEachRemaining(stringJsonNodeEntry -> {
      requestedAttributes.set(stringJsonNodeEntry.getKey(), stringJsonNodeEntry.getValue());
    });
  }

  /**
   * will add the given attributes to the given resource
   *
   * @param resource the resource to which the attributes should be added
   * @param operation the operation request that contains the new attributes
   */
  private boolean handlePatchOp(ResourceNode resource, PatchRequestOperation operation)
  {
    Optional<String> target = operation.getPath();
    List<String> values = operation.getValues();
    if (!operation.getOp().equals(PatchOp.REMOVE) && (values == null || values.isEmpty()))
    {
      throw new BadRequestException("no value attributes present in patch operation", null,
                                    ScimType.RFC7644.INVALID_VALUE);
    }

    if (target.isPresent())
    {
      String path = target.get();
      if (PatchOp.REMOVE.equals(operation.getOp()))
      {
        MsAzureWorkaroundHandler msAzureWorkaroundHandler = new MsAzureWorkaroundHandler(operation.getOp(), path,
                                                                                         values);
        path = msAzureWorkaroundHandler.fixPath();
      }
      PatchTargetHandler patchTargetHandler = new PatchTargetHandler(resourceType, operation.getOp(), path);
      boolean changeWasMade = patchTargetHandler.addOperationValues(resource, values);
      setAttributeFromPath(operation, patchTargetHandler.getPath());
      return changeWasMade;
    }
    else
    {
      if (PatchOp.REMOVE.equals(operation.getOp()))
      {
        throw new BadRequestException("missing target for remove operation", null, ScimType.RFC7644.NO_TARGET);
      }
      if (values.size() > 1)
      {
        throw new BadRequestException("too many resources set in patch operation. If the target is not specified only"
                                      + " a single value must be present in the values list which represents the "
                                      + "resource itself", null, ScimType.RFC7644.INVALID_VALUE);
      }
      PatchResourceHandler patchResourceHandler = new PatchResourceHandler(resourceType, operation.getOp());
      boolean changeWasMade = patchResourceHandler.addResourceValues(resource,
                                                                     JsonHelper.readJsonDocument(values.get(0)),
                                                                     null);
      setAttributesFromResource(operation);
      return changeWasMade;
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

}
