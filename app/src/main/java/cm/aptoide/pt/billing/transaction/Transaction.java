package cm.aptoide.pt.billing.transaction;

public class Transaction {

  private final String id;
  private final String customerId;
  private final String productId;
  private final Status status;
  private final String serviceId;

  public Transaction(String id, Status status, String customerId, String productId,
      String serviceId) {
    this.status = status;
    this.id = id;
    this.customerId = customerId;
    this.productId = productId;
    this.serviceId = serviceId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public String getProductId() {
    return productId;
  }

  public String getId() {
    return id;
  }

  public boolean isNew() {
    return Status.NEW.equals(status);
  }

  public boolean isCompleted() {
    return Status.COMPLETED.equals(status);
  }

  public boolean isPendingAuthorization() {
    return Status.PENDING_SERVICE_AUTHORIZATION.equals(status);
  }

  public boolean isProcessing() {
    return Status.PROCESSING.equals(status);
  }

  public boolean isFailed() {
    return Status.FAILED.equals(status);
  }

  public static enum Status {
    NEW, PENDING_SERVICE_AUTHORIZATION, PROCESSING, COMPLETED, FAILED
  }
}