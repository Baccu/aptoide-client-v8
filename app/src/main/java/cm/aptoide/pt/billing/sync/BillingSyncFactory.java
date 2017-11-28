package cm.aptoide.pt.billing.sync;

import cm.aptoide.pt.BuildConfig;
import cm.aptoide.pt.billing.CustomerPersistence;
import cm.aptoide.pt.billing.authorization.AuthorizationPersistence;
import cm.aptoide.pt.billing.authorization.AuthorizationService;
import cm.aptoide.pt.billing.authorization.LocalIdGenerator;
import cm.aptoide.pt.billing.transaction.TransactionPersistence;
import cm.aptoide.pt.billing.transaction.TransactionService;
import cm.aptoide.pt.sync.Sync;

public class BillingSyncFactory {

  private final CustomerPersistence customerPersistence;
  private final TransactionService transactionService;
  private final AuthorizationService authorizationService;
  private final TransactionPersistence transactionPersistence;
  private final AuthorizationPersistence authorizationPersistence;
  private final LocalIdGenerator localIdGenerator;

  public BillingSyncFactory(CustomerPersistence customerPersistence, TransactionService transactionService,
      AuthorizationService authorizationService, TransactionPersistence transactionPersistence,
      AuthorizationPersistence authorizationPersistence, LocalIdGenerator localIdGenerator) {
    this.customerPersistence = customerPersistence;
    this.transactionService = transactionService;
    this.authorizationService = authorizationService;
    this.transactionPersistence = transactionPersistence;
    this.authorizationPersistence = authorizationPersistence;
    this.localIdGenerator = localIdGenerator;
  }

  public Sync createAuthorizationSync(String transactionId) {
    return new AuthorizationSync("authorization" + transactionId, customerPersistence, transactionId,
        authorizationService, authorizationPersistence, true, true,
        BuildConfig.PAYMENT_AUTHORIZATION_SYNC_INTERVAL_MILLIS, 0, localIdGenerator);
  }

  public Sync createTransactionSync(String productId) {
    return new TransactionsSync("transaction" + productId, customerPersistence, transactionPersistence,
        transactionService, true, true, BuildConfig.PAYMENT_TRANSACTION_SYNC_INTERVAL_MILLIS, 0,
        productId);
  }
}
