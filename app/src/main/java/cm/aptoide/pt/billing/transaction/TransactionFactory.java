package cm.aptoide.pt.billing.transaction;

public class TransactionFactory {

  public Transaction create(String id, String customerId, String productId,
      Transaction.Status status, String serviceId) {
    return new Transaction(id, status, customerId, productId, serviceId);
  }
}