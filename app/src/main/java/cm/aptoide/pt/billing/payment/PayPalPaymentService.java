package cm.aptoide.pt.billing.payment;

import cm.aptoide.pt.billing.PaymentService;
import cm.aptoide.pt.billing.authorization.Authorization;
import cm.aptoide.pt.billing.authorization.AuthorizationRepository;
import cm.aptoide.pt.billing.transaction.Transaction;
import cm.aptoide.pt.billing.transaction.TransactionRepository;
import rx.Single;

public class PayPalPaymentService implements PaymentService<String> {

  public static final String TYPE = "PAYPAL";

  @Override public Single<Transaction> processPayment(String customerId, String productId,
      String paymentMethodId, String payload, TransactionRepository transactionRepository) {
    return transactionRepository.createTransaction(customerId, productId, paymentMethodId, payload);
  }

  @Override
  public Single<Authorization> createAuthorization(String customerId, String authorizationId,
      String payKey, AuthorizationRepository authorizationRepository) {
    return authorizationRepository.updateAuthorization(customerId, authorizationId, payKey,
        Authorization.Status.PENDING_SYNC);
  }
}
