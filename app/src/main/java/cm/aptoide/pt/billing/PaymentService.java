package cm.aptoide.pt.billing;

import cm.aptoide.pt.billing.authorization.Authorization;
import cm.aptoide.pt.billing.authorization.AuthorizationRepository;
import cm.aptoide.pt.billing.transaction.Transaction;
import cm.aptoide.pt.billing.transaction.TransactionRepository;
import rx.Single;

public interface PaymentService<T> {

  public Single<Transaction> processPayment(String customerId, String productId,
      String paymentMethodId, String payload, TransactionRepository transactionRepository);

  public Single<Authorization> createAuthorization(String customerId, String authorizationId,
      T metadata, AuthorizationRepository authorizationRepository);
}