/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 22/11/2016.
 */

package cm.aptoide.pt.billing.sync;

import cm.aptoide.pt.billing.CustomerPersistence;
import cm.aptoide.pt.billing.transaction.TransactionPersistence;
import cm.aptoide.pt.billing.transaction.TransactionService;
import cm.aptoide.pt.sync.Sync;
import rx.Completable;

public class TransactionsSync extends Sync {

  private final CustomerPersistence customerPersistence;
  private final TransactionPersistence transactionPersistence;
  private final TransactionService transactionService;
  private final String productId;

  public TransactionsSync(String id, CustomerPersistence customerPersistence,
      TransactionPersistence transactionPersistence, TransactionService transactionService,
      boolean periodic, boolean exact, long interval, long trigger, String productId) {
    super(id, periodic, exact, trigger, interval);
    this.customerPersistence = customerPersistence;
    this.transactionPersistence = transactionPersistence;
    this.transactionService = transactionService;
    this.productId = productId;
  }

  @Override public Completable execute() {
    return customerPersistence.getCustomer()
        .first().toSingle()
        .flatMap(customer -> transactionService.getTransaction(customer.getId(), productId))
        .flatMapCompletable(transaction -> transactionPersistence.saveTransaction(transaction));
  }
}
