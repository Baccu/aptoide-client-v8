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
import cm.aptoide.pt.billing.payment.AdyenPaymentService;
import cm.aptoide.pt.billing.payment.Payment;
import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.transaction.AuthorizedTransaction;
import cm.aptoide.pt.billing.transaction.Transaction;
import cm.aptoide.pt.billing.transaction.TransactionRepository;
import java.util.List;
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

  public Billing(String merchantPackageName, BillingService billingService,
      TransactionRepository transactionRepository, AuthorizationRepository authorizationRepository,
      CustomerPersistence customerPersistence, PurchaseTokenDecoder tokenDecoder,
      BillingSyncScheduler syncScheduler, MerchantVersionProvider versionProvider) {
    this.transactionRepository = transactionRepository;
    this.billingService = billingService;
    this.authorizationRepository = authorizationRepository;
    this.customerPersistence = customerPersistence;
    this.tokenDecoder = tokenDecoder;
    this.merchantPackageName = merchantPackageName;
    this.syncScheduler = syncScheduler;
    this.versionProvider = versionProvider;
  }

  public Single<Merchant> getMerchant() {
    return versionProvider.getVersionCode(merchantPackageName)
        .flatMap(versionCode -> billingService.getMerchant(merchantPackageName, versionCode));
  }

  public Observable<Payment> getPayment(String sku) {
    return getMerchant().flatMapObservable(merchant -> billingService.getPaymentServices()
        .flatMapObservable(services -> billingService.getProduct(sku, merchantPackageName)
            .flatMapObservable(product -> customerPersistence.getCustomer()
                .switchMap(customer -> {
                  if (customer.isAuthenticated()) {
                    return getAuthorizedTransaction(customer, product).flatMapSingle(
                        authorizedTransaction -> billingService.getPurchase(product.getId())
                            .map(purchase -> new Payment(merchant, customer, product,
                                authorizedTransaction, purchase, services)));
                  }
                  return Observable.just(
                      new Payment(merchant, customer, product, null, null, services));
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
    return getMerchant().flatMapCompletable(merchant -> billingService.getPaymentServices()
        .flatMapCompletable(services -> billingService.getProduct(sku, merchantPackageName)
            .flatMapCompletable(product -> customerPersistence.getCustomer()
                .first()
                .toSingle()
                .flatMapCompletable(
                    customer -> getPaymentService(services, serviceId).flatMap(selectedService -> {

                      if (selectedService instanceof AdyenPaymentService) {
                        return ((AdyenPaymentService) selectedService).getToken()
                            .flatMap(
                                token -> transactionRepository.createTransaction(customer.getId(),
                                    product.getId(), selectedService.getId(), payload, token));
                      }
                      return transactionRepository.createTransaction(customer.getId(),
                          product.getId(), selectedService.getId(), payload);
                    })
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

  private Single<PaymentService> getPaymentService(List<PaymentService> services,
      String serviceId) {
    return Observable.from(services)
        .first(service -> service.getId()
            .equals(serviceId))
        .toSingle();
  }

  public Completable authorize(String sku, String metadata) {
    return customerPersistence.getCustomer()
        .first()
        .toSingle()
        .flatMapCompletable(customer -> billingService.getProduct(sku, merchantPackageName)
            .flatMapCompletable(product -> getAuthorizedTransaction(customer, product).first()
                .toSingle()
                .flatMapCompletable(
                    transaction -> authorizationRepository.updateAuthorization(customer.getId(),
                        transaction.getAuthorization()
                .getId(), metadata, Authorization.Status.PENDING_SYNC))));
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
}