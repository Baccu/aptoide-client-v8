package cm.aptoide.pt.billing.payment;

import cm.aptoide.pt.billing.PaymentService;
import cm.aptoide.pt.billing.authorization.Authorization;
import cm.aptoide.pt.billing.authorization.AuthorizationRepository;
import cm.aptoide.pt.billing.transaction.Transaction;
import cm.aptoide.pt.billing.transaction.TransactionRepository;
import java.util.Map;
import rx.Single;

public class PaymentServiceAdapter {

  private final Map<String, PaymentService> adapters;
  private final TransactionRepository transactionRepository;
  private final AuthorizationRepository authorizationRepository;

  public PaymentServiceAdapter(Map<String, PaymentService> adapters,
      TransactionRepository transactionRepository,
      AuthorizationRepository authorizationRepository) {
    this.adapters = adapters;
    this.transactionRepository = transactionRepository;
    this.authorizationRepository = authorizationRepository;
  }

  public Single<Transaction> createTransaction(String paymentMethodType, String serviceId,
      String payload, String customerId, String productId) {
    return adapters.get(paymentMethodType)
        .createTransaction(customerId, productId, serviceId, payload, transactionRepository,
            paymentMethodType);
  }

  public <T> Single<Authorization> createAuthorization(String customerId, String paymentMethodId,
      String paymentMethodType, T metadata) {
    return adapters.get(paymentMethodType)
        .createAuthorization(customerId, paymentMethodId, metadata, authorizationRepository,
            paymentMethodType);
  }
}