package de.captaingoldfish.scim.sdk.server.interceptor;

import de.captaingoldfish.scim.sdk.server.endpoints.Context;

import java.util.function.Supplier;


/**
 * Interface specifying an interceptor execution operation
 *
 * @author alexandertokarev
 */
public interface Interceptor
{

  /**
   * An interceptor method that allows to execute arbitrary code around the execution of the
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler} methods
   *
   * @param resourceSupplier the code of the
   *          {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler} that will be executed
   * @param <T> type of result object
   * @return a result object returned by the supplier, or null if none
   */
  <T> T doAround(Supplier<T> resourceSupplier, Context context);
}
