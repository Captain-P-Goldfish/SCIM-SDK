package de.captaingoldfish.scim.sdk.server.interceptor;

import java.util.function.Supplier;


/**
 * No-Op implementation of {@link Interceptor}
 *
 * @author alexandertokarev
 */
public class NoopInterceptor implements Interceptor
{

  /**
   * simply executes given supplier that will execute the code within
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler}
   */
  @Override
  public <T> T doAround(Supplier<T> resourceSupplier)
  {
    return resourceSupplier.get();
  }
}
