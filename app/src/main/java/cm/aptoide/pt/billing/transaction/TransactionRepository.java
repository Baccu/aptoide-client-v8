/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 16/12/2016.
 */

package cm.aptoide.pt.billing.transaction;

import java.util.List;
import rx.Completable;
import rx.Single;

public class TransactionRepository {

  private final TransactionPersistence transactionPersistence;
  private final TransactionService transactionService;

  public TransactionRepository(TransactionPersistence transactionPersistence,
      TransactionService transactionService) {
    this.transactionPersistence = transactionPersistence;
    this.transactionService = transactionService;
  }

  public Single<Transaction> createTransaction(String customerId, String productId,
      String serviceId, String payload) {
    return transactionService.createTransaction(customerId, productId, serviceId, payload)
        .flatMap(transaction -> transactionPersistence.saveTransaction(transaction)
            .andThen(Single.just(transaction)));
  }

  public Single<Transaction> createTransaction(String customerId, String productId,
      String serviceId, String payload,
      String token) {
    return transactionService.createTransaction(customerId, productId, serviceId, payload, token)
        .flatMap(transaction -> transactionPersistence.saveTransaction(transaction)
            .andThen(Single.just(transaction)));
  }

  public Single<List<Transaction>> getOtherTransactions(String customerId, String productId,
      String transactionId) {
    return transactionPersistence.getOtherTransactions(transactionId, productId, customerId);
  }

  public Completable removeTransaction(String transactionId) {
    return transactionPersistence.removeTransaction(transactionId);
  }
}