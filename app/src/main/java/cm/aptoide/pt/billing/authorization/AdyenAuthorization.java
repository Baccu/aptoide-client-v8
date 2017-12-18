package cm.aptoide.pt.billing.authorization;

public class AdyenAuthorization extends MetadataAuthorization {

  private String session;

  public AdyenAuthorization(String id, String customerId, Status status, String paymentMethodId,
      String session, String metadata) {
    super(id, customerId, status, paymentMethodId, metadata);
    this.session = session;
  }

  public String getSession() {
    return session;
  }
}
