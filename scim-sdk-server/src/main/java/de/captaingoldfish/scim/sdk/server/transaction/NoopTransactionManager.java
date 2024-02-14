package de.captaingoldfish.scim.sdk.server.transaction;

import java.util.function.Supplier;


/**
 * No-Op implementation of transaction manager
 */
public class NoopTransactionManager implements TransactionManager
{

  @Override
  public <T> T executeInTransaction(Supplier<T> supplier)
  {
    return supplier.get();
  }
}
