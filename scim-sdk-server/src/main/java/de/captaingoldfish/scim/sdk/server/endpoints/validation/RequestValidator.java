package de.captaingoldfish.scim.sdk.server.endpoints.validation;

import java.util.function.Supplier;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;


/**
 * This validator can be used to validate incoming requests for specific endpoints. In some cases it might be
 * useful to validate the given input and check if a resource is allowed to be created with the given input.
 * The schema validation is not enough in some cases and therefore this implementation can be used to validate
 * specific parts and return an error before the actual endpoint is reached. <br>
 * This feature shall be an imitation of the java enterprise custom bean validation when custom bean
 * validation implementations are used.
 *
 * @author Pascal Knueppel
 * @since 07.04.2021
 */
public interface RequestValidator<T extends ResourceNode>
{

  /**
   * validate the resource for valid input before it is reached through to the
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler#createResource(ResourceNode, Authorization)}
   * implementation
   *
   * @param resource the resource to validate
   * @param validationContext add as much errors to this context as you like. If at least one error is present
   *          the execution will abort with a
   *          {@link de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException}
   * @param requestContext the current request context
   */
  public void validateCreate(T resource, ValidationContext validationContext, Context requestContext);

  /**
   * validate the resource for valid input before it is reached through to the
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler#updateResource(ResourceNode, Authorization)}
   * implementation
   *
   * @param oldResourceSupplier extracts the old resource representation by calling the SCIM get-endpoint when
   *          not already done by the ETag implementation
   * @param newResource the new resource representation
   * @param resource the resource to validate
   * @param validationContext add as much errors to this context as you like. If at least one error is present
   *          the execution will abort with a
   *          {@link de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException}
   * @param requestContext the current request context
   */
  public void validateUpdate(Supplier<T> oldResourceSupplier,
                             T newResource,
                             ValidationContext validationContext,
                             Context requestContext);


}
