package cm.aptoide.pt.billing.authorization;

public class MetadataAuthorization extends Authorization {

  private final String metadata;

  public MetadataAuthorization(String id, String customerId, Status status, String paymentMethodId,
      String metadata) {
    super(id, customerId, status, paymentMethodId);
    this.metadata = metadata;
  }

  public String getMetadata() {
    return metadata;
  }
}
