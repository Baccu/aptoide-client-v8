package cm.aptoide.pt.billing.payment;

import cm.aptoide.pt.billing.PaymentService;
import cm.aptoide.pt.billing.authorization.Authorization;
import cm.aptoide.pt.billing.authorization.AuthorizationRepository;
import cm.aptoide.pt.billing.transaction.Transaction;
import cm.aptoide.pt.billing.transaction.TransactionRepository;
import rx.Single;

public class AdyenPaymentService implements PaymentService<String> {

  public static final String TYPE = "ADYEN";
  private final Adyen adyen;

  public AdyenPaymentService(Adyen adyen) {
    this.adyen = adyen;
  }

  @Override public Single<Transaction> createTransaction(String customerId, String productId,
      String paymentMethodId, String payload, TransactionRepository transactionRepository,
      String paymentMethodType) {
    return adyen.createToken()
        .flatMap(
            token -> transactionRepository.createTransaction(customerId, productId, paymentMethodId,
                payload, token));
  }

  @Override
  public Single<Authorization> createAuthorization(String customerId, String paymentMethodId,
      String metadata, AuthorizationRepository authorizationRepository, String paymentMethodType) {
    return authorizationRepository.createAuthorization(customerId, paymentMethodId,
        paymentMethodType, metadata,
        Authorization.Status.PENDING_SYNC);
  }
}