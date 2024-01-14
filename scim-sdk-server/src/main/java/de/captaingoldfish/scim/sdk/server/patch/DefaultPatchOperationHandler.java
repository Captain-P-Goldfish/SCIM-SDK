package de.captaingoldfish.scim.sdk.server.patch;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.patch.operations.ComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.ComplexSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.ExtensionRefOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedSimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.PatchOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.SimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;


/**
 * This class represents the default patch-operation-handler for all {@link ResourceHandler}s
 *
 * @author Pascal Knueppel
 * @since 06.01.2024
 */
public class DefaultPatchOperationHandler<T extends ResourceNode> implements PatchOperationHandler<T>
{

  /**
   * the {@link ResourceNode}-type that is represented by this handler
   */
  private final Class<T> type;

  /**
   * the patch configuration of the service provider
   */
  private final PatchConfig patchConfig;

  /**
   * the endpoint-definition for the resource
   */
  private final ResourceType resourceType;

  /**
   * the current request-context
   */
  private final Context context;

  /**
   * the resource is retrieved from {@link ResourceHandler#getResource(String, List, List, Context)} and all
   * patch-operations will be applied to this object.
   */
  private T patchedResource;

  /**
   * a copy of the original resource that is being set in the method
   * {@link #getOldResourceSupplier(String, List, List, Context)}
   */
  private T oldResource;

  public DefaultPatchOperationHandler(Class<T> type,
                                      PatchConfig patchConfig,
                                      ResourceType resourceType,
                                      Context context)
  {
    this.type = type;
    this.patchConfig = patchConfig;
    this.resourceType = resourceType;
    this.context = context;
  }

  /**
   * this supplier simply calls the get-method of the {@link ResourceHandler} and stores the result twice. One
   * copy is used to apply the patch operations and the other copy is used to provide the original state of the
   * resource if needed
   *
   * @param id the id of the resource
   * @param attributes optional request-attributes
   * @param excludedAttributes optional attributes to exclude from the resource
   */
  @Override
  public Supplier<T> getOldResourceSupplier(String id,
                                            List<SchemaAttribute> attributes,
                                            List<SchemaAttribute> excludedAttributes)
  {
    return () -> {
      if (patchedResource == null)
      {
        patchedResource = (T)resourceType.getResourceHandlerImpl()
                                         .getResource(id, attributes, excludedAttributes, context);
      }
      if (oldResource == null)
      {
        oldResource = JsonHelper.copyResourceToObject(patchedResource, type);
      }
      return oldResource;
    };
  }

  /**
   * this method handles all types of patch-operations by giving each patch-operation to the
   * {@link PatchTargetHandler}
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the patch-operation that should be applied to the resource
   * @return true if an effective change was applied by the given patch-operation
   */
  private boolean handlePatchOperation(String id, PatchOperation patchOperation)
  {
    PatchTargetHandler patchTargetHandler = new PatchTargetHandler(patchConfig, resourceType, patchOperation);
    T patchedResourceNode = getPatchedResource(id);
    List<String> values = (List<String>)patchOperation.getValueStringList()
                                                      .stream()
                                                      .map(object -> String.valueOf(object))
                                                      .collect(Collectors.toList());
    return patchTargetHandler.handleOperationValues(patchedResourceNode, values);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleOperation(String id, ExtensionRefOperation patchOperation)
  {
    return handlePatchOperation(id, patchOperation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleOperation(String id, SimpleAttributeOperation patchOperation)
  {
    return handlePatchOperation(id, patchOperation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleOperation(String id, MultivaluedSimpleAttributeOperation patchOperation)
  {
    return handlePatchOperation(id, patchOperation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleOperation(String id, ComplexAttributeOperation patchOperation)
  {
    return handlePatchOperation(id, patchOperation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleOperation(String id, MultivaluedComplexAttributeOperation patchOperation)
  {
    return handlePatchOperation(id, patchOperation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleOperation(String id, ComplexSubAttributeOperation patchOperation)
  {
    return handlePatchOperation(id, patchOperation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleOperation(String id, MultivaluedComplexSubAttributeOperation patchOperation)
  {
    return handlePatchOperation(id, patchOperation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getPatchedResource(String id)
  {
    if (patchedResource == null)
    {
      patchedResource = (T)resourceType.getResourceHandlerImpl()
                                       .getResource(id, Collections.emptyList(), Collections.emptyList(), context);
    }
    return patchedResource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getUpdatedResource(String resourceId, T validatedPatchedResource, boolean wasResourceChanged)
  {
    if (wasResourceChanged)
    {
      return (T)resourceType.getResourceHandlerImpl().updateResource(validatedPatchedResource, context);
    }
    else
    {
      return validatedPatchedResource;
    }
  }
}
