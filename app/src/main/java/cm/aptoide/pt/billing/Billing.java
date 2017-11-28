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
  private final PaymentServiceSelector paymentServiceSelector;
  private final CustomerPersistence customerPersistence;
  private final PurchaseTokenDecoder tokenDecoder;
  private final String merchantName;
  private final BillingSyncScheduler syncScheduler;

  public Billing(String merchantName, BillingService billingService,
      TransactionRepository transactionRepository, AuthorizationRepository authorizationRepository,
      PaymentServiceSelector paymentServiceSelector, CustomerPersistence customerPersistence,
      PurchaseTokenDecoder tokenDecoder, BillingSyncScheduler syncScheduler) {
    this.transactionRepository = transactionRepository;
    this.billingService = billingService;
    this.authorizationRepository = authorizationRepository;
    this.paymentServiceSelector = paymentServiceSelector;
    this.customerPersistence = customerPersistence;
    this.tokenDecoder = tokenDecoder;
    this.merchantName = merchantName;
    this.syncScheduler = syncScheduler;
  }

  public Single<Merchant> getMerchant() {
    return billingService.getMerchant(merchantName);
  }

  public Observable<Payment> getPayment(String sku) {
    return getMerchant().flatMapObservable(merchant -> customerPersistence.getCustomer()
        .switchMap(customer -> {
          if (customer.isAuthenticated()) {
            return getPaymentServices().flatMapObservable(
                services -> getProduct(sku).flatMapObservable(
                    product -> getAuthorizedTransaction(customer, product).switchMap(
            authorizedTransaction -> Observable.combineLatest(getSelectedService(),
                getPurchase(product),
                (paymentService, purchase) -> new Payment(merchant, customer, product,
                    paymentService, authorizedTransaction, purchase, services)))));
          }
          return Observable.just(new Payment(merchant, customer, null, null, null, null, null));
        }));
  }

  public Single<List<Product>> getProducts(List<String> skus) {
    return billingService.getProducts(merchantName, skus);
  }

  public Single<List<Purchase>> getPurchases() {
    return billingService.getPurchases(merchantName);
  }

  public Completable consumePurchase(String purchaseToken) {
    return billingService.deletePurchase(tokenDecoder.decode(purchaseToken));
  }

  public Completable processPayment(String sku, String payload) {
    return getPayment(sku).first()
        .toSingle()
        .flatMap(payment -> {
          if (payment.getSelectedPaymentService() instanceof AdyenPaymentService) {
            return ((AdyenPaymentService) payment.getSelectedPaymentService()).getToken()
                .flatMap(token -> transactionRepository.createTransaction(payment.getCustomer()
                    .getId(), payment.getProduct()
                    .getId(), payment.getSelectedPaymentService()
                    .getId(), payload, token));
          }
          return transactionRepository.createTransaction(payment.getCustomer()
              .getId(), payment.getProduct()
              .getId(), payment.getSelectedPaymentService()
              .getId(), payload);
        })
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

  public Completable authorize(String sku, String metadata) {
    return getPayment(sku).first()
        .toSingle()
        .flatMapCompletable(payment -> authorizationRepository.updateAuthorization(
            payment.getCustomer()
                .getId(), payment.getAuthorization()
                .getId(),
                metadata, Authorization.Status.PENDING_SYNC));
  }

  public Completable selectService(String serviceId) {
    return getService(serviceId).flatMapCompletable(
        service -> paymentServiceSelector.selectService(service))
        .onErrorComplete();
  }

  public void stopSync() {
    syncScheduler.stopSyncs();
  }

  private Observable<Purchase> getPurchase(Product product) {
    return billingService.getPurchase(product.getId())
        .toObservable();
  }

  private Single<Product> getProduct(String sku) {
    return billingService.getProduct(sku, merchantName);
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

  private Observable<PaymentService> getSelectedService() {
    return getPaymentServices().flatMapObservable(
        services -> paymentServiceSelector.getSelectedService(services));
  }

  private Single<PaymentService> getService(String serviceId) {
    return getPaymentServices().flatMapObservable(payments -> Observable.from(payments)
        .filter(payment -> payment.getId()
            .equals(serviceId))
        .switchIfEmpty(
            Observable.error(new IllegalArgumentException("Payment " + serviceId + " not found."))))
        .first()
        .toSingle();
  }

  private Single<List<PaymentService>> getPaymentServices() {
    return billingService.getPaymentServices();
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