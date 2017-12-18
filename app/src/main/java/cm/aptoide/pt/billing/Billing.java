/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 12/08/2016.
 */

package cm.aptoide.pt.billing;

import cm.aptoide.pt.billing.authorization.Authorization;
import cm.aptoide.pt.billing.authorization.AuthorizationRepository;
import cm.aptoide.pt.billing.customer.Customer;
import cm.aptoide.pt.billing.exception.PaymentFailureException;
import cm.aptoide.pt.billing.exception.ServiceNotAuthorizedException;
import cm.aptoide.pt.billing.payment.Payment;
import cm.aptoide.pt.billing.payment.PaymentMethod;
import cm.aptoide.pt.billing.payment.PaymentServiceAdapter;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.transaction.AuthorizedTransaction;
import cm.aptoide.pt.billing.transaction.Transaction;
import cm.aptoide.pt.billing.transaction.TransactionRepository;
import java.util.Collections;
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
  private final BillingSyncScheduler syncScheduler;
  private final MerchantVersionProvider versionProvider;
  private final PaymentServiceAdapter serviceAdapter;

  private Billing(String merchantPackageName, BillingService billingService,
      TransactionRepository transactionRepository, AuthorizationRepository authorizationRepository,
      CustomerPersistence customerPersistence, PurchaseTokenDecoder tokenDecoder,
      BillingSyncScheduler syncScheduler, MerchantVersionProvider versionProvider,
      PaymentServiceAdapter serviceAdapter) {
    this.transactionRepository = transactionRepository;
    this.billingService = billingService;
    this.authorizationRepository = authorizationRepository;
    this.customerPersistence = customerPersistence;
    this.tokenDecoder = tokenDecoder;
    this.merchantPackageName = merchantPackageName;
    this.syncScheduler = syncScheduler;
    this.versionProvider = versionProvider;
    this.serviceAdapter = serviceAdapter;
  }

  public Single<Merchant> getMerchant() {
    return versionProvider.getVersionCode(merchantPackageName)
        .flatMap(versionCode -> billingService.getMerchant(merchantPackageName, versionCode));
  }

  public Observable<Payment> getPayment(String sku) {
    return getMerchant().flatMapObservable(merchant -> billingService.getPaymentMethods()
        .flatMapObservable(services -> billingService.getProduct(sku, merchantPackageName)
            .flatMapObservable(product -> customerPersistence.getCustomer()
                .switchMap(customer -> {
                  if (customer.isAuthenticated()) {
                    return Observable.combineLatest(
                        transactionRepository.getTransaction(customer.getId(), product.getId()),
                        authorizationRepository.getAuthorizations(customer.getId()),
                        billingService.getPurchase(product.getId())
                            .toObservable(),
                        (transaction, authorizations, purchase) -> new Payment(merchant, customer,
                            product, transaction, purchase, services, authorizations));
                  }
                  return Observable.just(
                      new Payment(merchant, customer, product, null, null, services,
                          Collections.emptyList()));
                }))));
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

  public Completable processPayment(String serviceId, String sku, String payload) {
    return getMerchant().flatMapCompletable(merchant -> billingService.getPaymentMethods()
        .flatMapCompletable(methods -> billingService.getProduct(sku, merchantPackageName)
            .flatMapCompletable(product -> customerPersistence.getCustomer()
                .first()
                .toSingle()
                .flatMapCompletable(customer -> getPaymentMethod(methods, serviceId).flatMap(
                    selectedService -> serviceAdapter.createTransaction(selectedService.getType(),
                        serviceId, sku, payload, customer.getId(), product.getId()))
                        .flatMapCompletable(
                            transaction -> removeOldTransactions(transaction).andThen(
                                Completable.defer(() -> {
                                  if (transaction.isPendingAuthorization()) {
                                    return Completable.error(new ServiceNotAuthorizedException(
                                        "Pending service authorization."));
                                  }

                                  if (transaction.isFailed()) {
                                    return Completable.error(
                                        new PaymentFailureException("Payment failed."));
                                  }

                                  return Completable.complete();
                                })))))));
  }

  private Single<PaymentMethod> getPaymentMethod(List<PaymentMethod> methods, String methodId) {
    return Observable.from(methods)
        .first(service -> service.getId()
            .equals(methodId))
        .toSingle();
  }

  public Completable authorize(String sku, String metadata, String methodId) {
    return Single.zip(customerPersistence.getCustomer()
        .first()
        .toSingle(), billingService.getProduct(sku, merchantPackageName), (customer, product) -> {
      return Single.zip(getAuthorizedTransaction(customer, product).first()
              .toSingle(), billingService.getPaymentMethods()
              .flatMap(methods -> getPaymentMethod(methods, methodId)),
          (transaction, paymentMethod) -> {
            return serviceAdapter.authorize(paymentMethod.getType(), metadata, customer.getId(),
                transaction.getId());
          });
    })
        .flatMap(single -> single)
        .toCompletable();
  }

  public void stopSync() {
    syncScheduler.stopSyncs();
  }

  private Observable<Authorization> getAuthorization(Customer customer, Transaction transaction) {
    if (transaction.isNew()) {
      return authorizationRepository.createAuthorization(customer.getId(), transaction.getId(),
          Authorization.Status.NEW)
          .flatMapObservable(__ -> authorizationRepository.getAuthorization(customer.getId(),
              transaction.getId()));
    }
    return authorizationRepository.getAuthorization(customer.getId(), transaction.getId());
  }

  private Observable<AuthorizedTransaction> getAuthorizedTransaction(Customer customer,
      Product product) {
    return transactionRepository.getTransaction(customer.getId(), product.getId())
        .switchMap(transaction -> getAuthorization(customer, transaction).map(
            authorization -> new AuthorizedTransaction(transaction, authorization)));
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

  public static class Builder {

    private TransactionRepository transactionRepository;
    private BillingService billingService;
    private AuthorizationRepository authorizationRepository;
    private CustomerPersistence customerPersistence;
    private PurchaseTokenDecoder tokenDecoder;
    private String merchantPackageName;
    private BillingSyncScheduler syncScheduler;
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

    public Builder setSyncScheduler(BillingSyncScheduler syncScheduler) {
      this.syncScheduler = syncScheduler;
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
          authorizationRepository, customerPersistence, tokenDecoder, syncScheduler,
          versionProvider,
          new PaymentServiceAdapter(services, transactionRepository, authorizationRepository));
    }
  }
}