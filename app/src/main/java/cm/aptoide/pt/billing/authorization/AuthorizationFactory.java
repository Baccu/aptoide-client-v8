package cm.aptoide.pt.billing.authorization;

import cm.aptoide.pt.billing.Price;

public class AuthorizationFactory {

  public static final String PAYPAL_SDK = "PAYPAL_SDK";
  public static final String ADYEN_SDK = "ADYEN_SDK";

  public Authorization create(String id, String customerId, String type,
      Authorization.Status status, String metadata, Price price, String description,
      String paymentMethodId, String session) {

    if (type == null) {
      return new Authorization(id, customerId, status, paymentMethodId);
    }

    switch (type) {
      case PAYPAL_SDK:
        return new PayPalAuthorization(id, customerId, status, paymentMethodId, metadata, price,
            description);
      case ADYEN_SDK:
        return new AdyenAuthorization(id, customerId, status, paymentMethodId, session, metadata);
      default:
        return new Authorization(id, customerId, status, paymentMethodId);
    }
  }
}