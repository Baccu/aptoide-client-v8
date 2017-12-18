/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 12/08/2016.
 */

package cm.aptoide.pt.billing;

import cm.aptoide.pt.billing.authorization.AuthorizationRepository;
import cm.aptoide.pt.billing.exception.PaymentFailureException;
import cm.aptoide.pt.billing.exception.ServiceNotAuthorizedException;
import cm.aptoide.pt.billing.payment.Payment;
import cm.aptoide.pt.billing.payment.PaymentMethod;
import cm.aptoide.pt.billing.payment.PaymentServiceAdapter;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.transaction.Transaction;
import cm.aptoide.pt.billing.transaction.TransactionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rx.Completable;
import rx.Observable;
import rx.Single;

public class Billing {

  private final TransactionRepository transactionRepository;
  private final BillingService billingService;
  private final AuthorizationRepository authorizationRepository;
  private final CustomerPersistence customerPersistence;
  private final PurchaseTokenDecoder tokenDecoder;
  private final String merchantPackageName;
  private final MerchantVersionProvider versionProvider;
  private final PaymentServiceAdapter serviceAdapter;

  private Billing(String merchantPackageName, BillingService billingService,
      TransactionRepository transactionRepository, AuthorizationRepository authorizationRepository,
      CustomerPersistence customerPersistence, PurchaseTokenDecoder tokenDecoder,
      MerchantVersionProvider versionProvider, PaymentServiceAdapter serviceAdapter) {
    this.transactionRepository = transactionRepository;
    this.billingService = billingService;
    this.authorizationRepository = authorizationRepository;
    this.customerPersistence = customerPersistence;
    this.tokenDecoder = tokenDecoder;
    this.merchantPackageName = merchantPackageName;
    this.versionProvider = versionProvider;
    this.serviceAdapter = serviceAdapter;
  }

  public Single<Merchant> getMerchant() {
    return versionProvider.getVersionCode(merchantPackageName)
        .flatMap(versionCode -> billingService.getMerchant(merchantPackageName, versionCode));
  }

  public Observable<Payment> getPayment(String sku) {
    return customerPersistence.getCustomer()
        .switchMap(customer -> versionProvider.getVersionCode(merchantPackageName)
            .flatMap(merchantVersionCode -> billingService.getPayment(merchantPackageName,
                merchantVersionCode, sku, customer, ""))
            .toObservable());
  }

  public Single<List<Product>> getProducts(List<String> skus) {
    return billingService.getProducts(merchantPackageName, skus);
  }

  public Single<List<Purchase>> getPurchases() {
    return billingService.getPurchases(merchantPackageName);
  }

  public Completable consumePurchase(String purchaseToken) {
    return billingService.deletePurchase(tokenDecoder.decode(purchaseToken));
  }

  public Completable processPayment(String paymentMethodId, String sku, String payload) {
    return getPayment(sku)
                .first()
                .toSingle()
        .flatMap(payment -> getPaymentMethod(payment.getPaymentMethods(), paymentMethodId).flatMap(
            paymentMethod -> serviceAdapter.createTransaction(paymentMethod.getType(),
                paymentMethodId, payload, payment.getCustomer()
                    .getId(), payment.getProduct()
                    .getId())))
        .flatMapCompletable(
            transaction -> removeOldTransactions(transaction).andThen(Completable.defer(() -> {
              if (transaction.isPendingAuthorization()) {
                return Completable.error(
                    new ServiceNotAuthorizedException("Pending service authorization."));
              }

              if (transaction.isFailed()) {
                return Completable.error(new PaymentFailureException("Payment failed."));
              }

              return Completable.complete();
            })));
  }

  public Completable authorize(String sku, String metadata, String paymentMethodId) {
    return getPayment(sku).first()
        .toSingle()
        .flatMap(payment -> getPaymentMethod(payment.getPaymentMethods(), paymentMethodId).flatMap(
            paymentMethod -> serviceAdapter.createAuthorization(payment.getCustomer()
                .getId(), paymentMethodId, paymentMethod.getType(), metadata)))
        .toCompletable();
  }

  public void stopSync() {

  }

  private Completable removeOldTransactions(Transaction transaction) {
    return transactionRepository.getOtherTransactions(transaction.getCustomerId(),
        transaction.getProductId(), transaction.getId())
        .flatMapObservable(otherTransactions -> Observable.from(otherTransactions))
        .flatMapCompletable(
            otherTransaction -> transactionRepository.removeTransaction(otherTransaction.getId())
                .andThen(
                    authorizationRepository.removeAuthorization(otherTransaction.getCustomerId(),
                        otherTransaction.getId())))
        .toCompletable();
  }

  private Single<PaymentMethod> getPaymentMethod(List<PaymentMethod> methods, String methodId) {
    return Observable.from(methods)
        .first(service -> service.getId()
            .equals(methodId))
        .toSingle();
  }

  public static class Builder {

    private TransactionRepository transactionRepository;
    private BillingService billingService;
    private AuthorizationRepository authorizationRepository;
    private CustomerPersistence customerPersistence;
    private PurchaseTokenDecoder tokenDecoder;
    private String merchantPackageName;
    private MerchantVersionProvider versionProvider;
    private Map<String, PaymentService> services;

    public Builder() {
      this.services = new HashMap<>();
    }

    public Builder setTransactionRepository(TransactionRepository transactionRepository) {
      this.transactionRepository = transactionRepository;
      return this;
    }

    public Builder setBillingService(BillingService billingService) {
      this.billingService = billingService;
      return this;
    }

    public Builder setAuthorizationRepository(AuthorizationRepository authorizationRepository) {
      this.authorizationRepository = authorizationRepository;
      return this;
    }

    public Builder setCustomerPersistence(CustomerPersistence customerPersistence) {
      this.customerPersistence = customerPersistence;
      return this;
    }

    public Builder setPurchaseTokenDecoder(PurchaseTokenDecoder tokenDecoder) {
      this.tokenDecoder = tokenDecoder;
      return this;
    }

    public Builder setMerchantPackageName(String merchantPackageName) {
      this.merchantPackageName = merchantPackageName;
      return this;
    }

    public Builder setMerchantVersionProvider(MerchantVersionProvider versionProvider) {
      this.versionProvider = versionProvider;
      return this;
    }

    public Builder registerPaymentService(String type, PaymentService service) {
      services.put(type, service);
      return this;
    }

    public Billing build() {

      if (services.isEmpty()) {
        throw new IllegalStateException("Register at least 1 payment service");
      }

      return new Billing(merchantPackageName, billingService, transactionRepository,
          authorizationRepository, customerPersistence, tokenDecoder, versionProvider,
          new PaymentServiceAdapter(services, transactionRepository, authorizationRepository));
    }
  }
}