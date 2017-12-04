package cm.aptoide.pt.billing;

import android.os.Bundle;
import cm.aptoide.pt.analytics.Analytics;
import cm.aptoide.pt.analytics.Event;
import cm.aptoide.pt.analytics.events.FacebookEvent;
import cm.aptoide.pt.billing.payment.Payment;
import com.facebook.appevents.AppEventsLogger;

public class BillingAnalytics {

  private final Analytics analytics;
  private final AppEventsLogger facebook;

  public BillingAnalytics(Analytics analytics, AppEventsLogger facebook) {
    this.analytics = analytics;
    this.facebook = facebook;
  }

  public void sendPaymentViewShowEvent() {
    analytics.sendEvent(getFacebookPaymentEvent("Payment_Pop_Up", "Show", new Bundle()));
  }

  public void sendPaymentViewCancelEvent(Payment payment) {
    analytics.sendEvent(getFacebookPaymentEvent("Payment_Pop_Up", "Cancel", getProductBundle(
        payment.getProduct()
            .getPrice()
            .getAmount(), payment.getProduct()
            .getPrice()
            .getCurrency(), payment.getMerchant()
            .getVersionCode(), payment.getMerchant()
            .getPackageName())));
  }

  public void sendPaymentViewBuyEvent(Payment payment) {
    final Bundle bundle = getProductBundle(payment.getProduct()
        .getPrice()
        .getAmount(), payment.getProduct()
        .getPrice()
        .getCurrency(), payment.getMerchant()
        .getVersionCode(), payment.getMerchant()
        .getPackageName());
    analytics.sendEvent(getFacebookPaymentEvent("Payment_Pop_Up", "Buy", bundle));
  }

  public void sendPaymentSuccessEvent() {
    analytics.sendEvent(getFacebookPaymentEvent("Payment_Pop_Up", "Success", new Bundle()));
  }

  public void sendPaymentErrorEvent() {
    analytics.sendEvent(getFacebookPaymentEvent("Payment_Pop_Up", "Error", new Bundle()));
  }

  public void sendCustomerAuthenticatedEvent(boolean customerAuthenticated) {
    final String action;
    if (customerAuthenticated) {
      action = "Success";
    } else {
      action = "Cancel";
    }
    analytics.sendEvent(getFacebookPaymentEvent("Payment_Login", action, new Bundle()));
  }

  public void sendAuthorizationSuccessEvent(Payment payment) {
    final Bundle bundle = new Bundle();
    bundle.putString("payment_method", payment.getPaymentService()
        .getType());
    analytics.sendEvent(getFacebookPaymentEvent("Payment_Authorization_Page", "Success", bundle));
  }

  public void sendAuthorizationCancelEvent(String serviceName) {
    final Bundle bundle = new Bundle();
    bundle.putString("payment_method", serviceName);
    analytics.sendEvent(getFacebookPaymentEvent("Payment_Authorization_Page", "Cancel", bundle));
  }

  public void sendAuthorizationErrorEvent(String serviceName) {
    final Bundle bundle = new Bundle();
    bundle.putString("payment_method", serviceName);
    analytics.sendEvent(getFacebookPaymentEvent("Payment_Authorization_Page", "Error", bundle));
  }

  private Event getFacebookPaymentEvent(String eventName, String action, Bundle bundle) {
    bundle.putString("action", action);
    return new FacebookEvent(facebook, eventName, bundle);
  }

  private Bundle getProductBundle(double productAmount, String productCurrency,
      int merchantVersionCode, String merchantPackageName) {
    final Bundle bundle = new Bundle();
    bundle.putDouble("purchase_value", productAmount);
    bundle.putString("purchase_currency", productCurrency);
    bundle.putString("package_name_seller", merchantPackageName);
    bundle.putInt("package_version_code_seller", merchantVersionCode);
    return bundle;
  }
}
