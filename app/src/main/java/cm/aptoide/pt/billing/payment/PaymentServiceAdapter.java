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

  public Single<Transaction> createTransaction(String type, String serviceId, String sku,
      String payload, String customerId, String productId) {
    return adapters.get(type)
        .processPayment(customerId, productId, serviceId, payload, transactionRepository);
  }

  public <T> Single<Authorization> authorize(String type, T metadata, String customerId,
      String transactionId) {
    return adapters.get(type)
        .createAuthorization(customerId, transactionId, metadata, authorizationRepository);
  }
}