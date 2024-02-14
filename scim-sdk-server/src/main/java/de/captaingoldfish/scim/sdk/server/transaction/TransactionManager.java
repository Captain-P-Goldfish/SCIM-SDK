package de.captaingoldfish.scim.sdk.server.transaction;

import java.util.function.Supplier;


/**
 * Interface specifying a transaction execution operation
 */
public interface TransactionManager
{

  /**
   * Execute the action specified by the given supplier object within a transaction. Implementations should
   * rethrow RuntimeException thrown by the supplier and enforce a rollback in such case
   *
   * @param supplier action executed within a transaction
   * @param <T> type of result object
   * @return a result object returned by the supplier, or null if none
   */
  <T> T executeInTransaction(Supplier<T> supplier);
}
