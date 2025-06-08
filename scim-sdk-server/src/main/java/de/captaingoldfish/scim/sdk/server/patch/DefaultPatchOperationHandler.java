package de.captaingoldfish.scim.sdk.server.patch;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.features.EndpointType;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.RequestValidatorHandler;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexMultivaluedSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexSimpleSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedSimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.PatchOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveExtensionRefOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.SimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.validation.RequestResourceValidator;
import lombok.extern.slf4j.Slf4j;


/**
 * This class represents the default patch-operation-handler for all {@link ResourceHandler}s
 *
 * @author Pascal Knueppel
 * @since 06.01.2024
 */
@Slf4j
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
                                            List<SchemaAttribute> excludedAttributes,
                                            Context context)
  {
    return () -> {
      if (patchedResource == null)
      {
        patchedResource = (T)resourceType.getResourceHandlerImpl()
                                         .getResourceForUpdate(id,
                                                               attributes,
                                                               excludedAttributes,
                                                               context,
                                                               EndpointType.PATCH);
        if (patchedResource == null)
        {
          throw new ResourceNotFoundException(String.format("%s with id '%s' does not exist.",
                                                            resourceType.getName(),
                                                            id));
        }
        oldResource = JsonHelper.readJsonDocument(patchedResource.toString(), type);
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
    if (log.isTraceEnabled())
    {
      log.trace("handling '{}'-operation of type '{}' with values '{}'",
                patchOperation.getPatchOp(),
                patchOperation.getClass().getSimpleName(),
                values);
    }
    return patchTargetHandler.handleOperationValues(patchedResourceNode, values);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleOperation(String id, RemoveExtensionRefOperation patchOperation)
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
  public boolean handleOperation(String id, RemoveComplexAttributeOperation patchOperation)
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
  public boolean handleOperation(String id, MultivaluedComplexMultivaluedSubAttributeOperation patchOperation)
  {
    return handlePatchOperation(id, patchOperation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleOperation(String id, MultivaluedComplexSimpleSubAttributeOperation patchOperation)
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
      if (patchedResource == null)
      {
        throw new ResourceNotFoundException("the '" + resourceType.getName() + "' resource with id '" + id + "' does "
                                            + "not exist", null, null);
      }
      // TODO enhance this creepy copy creation
      oldResource = JsonHelper.readJsonDocument(patchedResource.toString(), type);
    }
    return patchedResource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getUpdatedResource(String resourceId,
                              T patchedResource,
                              boolean wasResourceChanged,
                              List<SchemaAttribute> attributes,
                              List<SchemaAttribute> excludedAttributes,
                              Context context)
  {
    if (wasResourceChanged)
    {
      T validatedResource;
      ServiceProvider serviceProvider = resourceType.getResourceHandlerImpl().getServiceProvider();
      RequestResourceValidator requestResourceValidator = new RequestResourceValidator(serviceProvider, resourceType,
                                                                                       HttpMethod.PATCH);
      validatedResource = (T)requestResourceValidator.validateDocument(patchedResource);
      validatedResource.setId(resourceId);
      Supplier<T> oldResourceSupplier = getOldResourceSupplier(resourceId,
                                                               Collections.emptyList(),
                                                               Collections.emptyList(),
                                                               context);
      // handle meta attribute
      {
        final Meta meta = validatedResource.getMeta().orElseGet(() -> {
          Meta newMeta = new Meta();
          validatedResource.setMeta(newMeta);
          return newMeta;
        });
        final String location = context.getResourceReferenceUrl(resourceId);
        meta.setLocation(location);
        meta.setResourceType(resourceType.getName());
        if (!meta.getCreated().isPresent())
        {
          meta.setCreated(meta.getLastModified().orElseGet(Instant::now));
        }
        if (!meta.getLastModified().isPresent())
        {
          meta.setLastModified(meta.getCreated().orElse(null));
        }
      }
      new RequestValidatorHandler(resourceType.getResourceHandlerImpl(), requestResourceValidator,
                                  context).validateUpdate((Supplier<ResourceNode>)oldResourceSupplier,
                                                          validatedResource);

      validatedResource.setId(resourceId); // this will make sure that the id is not lost after resource-validation
      return (T)resourceType.getResourceHandlerImpl().updateResource(patchedResource, context);
    }
    else
    {
      return patchedResource;
    }
  }
}
