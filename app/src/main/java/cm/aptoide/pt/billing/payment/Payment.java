package cm.aptoide.pt.billing.payment;

import cm.aptoide.pt.billing.Merchant;
import cm.aptoide.pt.billing.authorization.Authorization;
import cm.aptoide.pt.billing.customer.Customer;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.transaction.Transaction;
import java.util.List;

public class Payment {

  private final Merchant merchant;
  private final Customer customer;
  private final Product product;
  private final List<PaymentMethod> paymentMethods;

  private final String selectedPaymentMethodId;
  private final Transaction transaction;
  private final Purchase purchase;

  public Payment(Merchant merchant, Customer customer, Product product, Transaction transaction,
      Purchase purchase, List<PaymentMethod> paymentMethods, String selectedPaymentMethodId) {
    this.merchant = merchant;
    this.customer = customer;
    this.product = product;
    this.transaction = transaction;
    this.purchase = purchase;
    this.paymentMethods = paymentMethods;
    this.selectedPaymentMethodId = selectedPaymentMethodId;
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

  public PaymentMethod getSelectedPaymentMethod() {
    for (PaymentMethod paymentMethod : paymentMethods) {
      if (paymentMethod.getId()
          .equals(selectedPaymentMethodId)) {
        return paymentMethod;
      }
    }
    return null;
  }

  public Purchase getPurchase() {
    return purchase;
  }

  public List<PaymentMethod> getPaymentMethods() {
    return paymentMethods;
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

  public Authorization getSelectedAuthorization() {
    final PaymentMethod selectedPaymentMethod = getSelectedPaymentMethod();
    if (selectedPaymentMethod != null && selectedPaymentMethod.getSelectedAuthorization() != null) {
      return selectedPaymentMethod.getSelectedAuthorization();
    }
    return null;
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

  public boolean isAuthorized() {
    final PaymentMethod selectedPaymentMethod = getSelectedPaymentMethod();
    return selectedPaymentMethod != null
        && selectedPaymentMethod.getSelectedAuthorization() != null
        && selectedPaymentMethod.getSelectedAuthorization()
        .isActive();
  }
}
