package cm.aptoide.pt.billing.payment;

import cm.aptoide.pt.billing.Merchant;
import cm.aptoide.pt.billing.authorization.Authorization;
import cm.aptoide.pt.billing.customer.Customer;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.transaction.AuthorizedTransaction;
import cm.aptoide.pt.billing.transaction.Transaction;
import java.util.Collections;
import java.util.List;
import rx.Observable;

public class Payment {

  private final Merchant merchant;
  private final Customer customer;
  private final Product product;
  private final List<PaymentService> paymentServices;

  private final Transaction transaction;
  private final Purchase purchase;

  public Payment(Merchant merchant, Customer customer, Product product, Transaction transaction,
      Purchase purchase, List<PaymentService> paymentServices) {
    this.merchant = merchant;
    this.customer = customer;
    this.product = product;
    this.transaction = transaction;
    this.purchase = purchase;
    this.paymentServices = paymentServices;
  }

  public Merchant getMerchant() {
    return merchant;
  }

  public Customer getCustomer() {
    return customer;
  }

  public Product getProduct() {
    return product;
  }

  public PaymentService getPaymentService(String serviceId) {
    for (PaymentService service: paymentServices) {
      if (service.getId()
          .equals(serviceId)) {
        return service;
      }
    }
    throw new IllegalArgumentException("No service for id: " + serviceId);
  }

  public PaymentService getPaymentService() {
    if (transaction != null) {
      return getPaymentService(transaction.getServiceId());
    }
    throw new IllegalStateException(
        "No transaction for payment yet. Can not return payment service.");
  }

  public Purchase getPurchase() {
    return purchase;
  }

  public List<PaymentService> getPaymentServices() {
    return paymentServices;
  }

  public boolean isNew() {
    if (transaction.isNew() && !purchase.isCompleted()) {
      return true;
    }

    if (transaction.isCompleted() && !purchase.isCompleted()) {
      return true;
    }

    return false;
  }

  public Authorization getAuthorization() {
    if (transaction instanceof AuthorizedTransaction) {
      return ((AuthorizedTransaction) transaction).getAuthorization();
    }
    throw new IllegalStateException("Payment does not require authorization.");
  }

  public boolean isPendingAuthorization() {
    if (transaction instanceof AuthorizedTransaction) {
      return transaction.isPendingAuthorization();
    }
    throw new IllegalStateException("Payment does not require authorization.");
  }

  public boolean isProcessing() {
    return transaction.isProcessing();
  }

  public boolean isFailed() {
    return transaction.isFailed();
  }

  public boolean isCompleted() {
    return purchase.isCompleted();
  }

  public List<Authorization> getAuthorizations() {
    return Collections.emptyList();
  }
}
