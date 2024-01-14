package de.captaingoldfish.scim.sdk.server.patch.workarounds;

import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;


/**
 * This class is used to fix patch-request-operations that are built in a manner so that the current
 * patch-handler is not able to process them properly. Using such an implementation will help to bring the
 * operation in a state so that it can be handled by the patch-handler implementation from the server
 *
 * @author Pascal Knueppel
 * @since 04.01.2024
 */
public abstract class PatchWorkaround
{

  /**
   * if this patch-workaround should be executed or not
   *
   * @param patchConfig the current patch-configuration
   * @param operation the operation that should be fixed if there is anything to fix
   * @return true if the method {@link #fixPatchRequestOperaton(PatchRequestOperation)} should be executed,
   *         false else
   */
  public abstract boolean shouldBeHandled(PatchConfig patchConfig,
                                          ResourceType resourceType,
                                          PatchRequestOperation operation);

  /**
   * if other workaround-handlers should be executed after this one was
   *
   * @return true if other workaround-handlers should also be executed if this one matched the criteria
   */
  public abstract boolean executeOtherHandlers();

  /**
   * must rebuild the given patch-operation into a state that can be handled by the servers patch-handler
   *
   * @param resourceType the resources definition
   * @param operation the patch request operation to fix
   * @return the fixed patch request operation
   */
  public abstract PatchRequestOperation fixPatchRequestOperaton(ResourceType resourceType,
                                                                PatchRequestOperation operation);

}

