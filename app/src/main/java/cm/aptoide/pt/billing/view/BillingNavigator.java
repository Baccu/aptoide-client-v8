package cm.aptoide.pt.billing.view;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.customtabs.CustomTabsIntent;
import cm.aptoide.pt.BuildConfig;
import cm.aptoide.pt.billing.networking.PaymentServiceMapper;
import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.view.login.PaymentLoginFragment;
import cm.aptoide.pt.billing.view.paypal.PayPalAuthorizationFragment;
import cm.aptoide.pt.billing.view.web.WebAuthorizationFragment;
import cm.aptoide.pt.navigator.ActivityNavigator;
import cm.aptoide.pt.navigator.CustomTabsNavigator;
import cm.aptoide.pt.navigator.FragmentNavigator;
import cm.aptoide.pt.navigator.Result;
import com.adyen.core.PaymentRequest;
import com.adyen.core.models.paymentdetails.CreditCardPaymentDetails;
import com.adyen.ui.fragments.CreditCardFragmentBuilder;
import com.jakewharton.rxrelay.PublishRelay;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import java.math.BigDecimal;
import rx.Observable;

public class BillingNavigator {

  private final PurchaseBundleMapper bundleMapper;
  private final ActivityNavigator activityNavigator;
  private final FragmentNavigator fragmentNavigator;
  private final String marketName;
  private final PublishRelay<CreditCardPaymentDetails> detailsRelay;
  private final CustomTabsNavigator customTabsNavigator;
  private final int customTabsToolbarColor;

  public BillingNavigator(PurchaseBundleMapper bundleMapper, ActivityNavigator activityNavigator,
      FragmentNavigator fragmentNavigator, String marketName,
      PublishRelay<CreditCardPaymentDetails> detailsRelay, CustomTabsNavigator customTabsNavigator,
      @ColorInt int customTabsToolbarColor) {
    this.bundleMapper = bundleMapper;
    this.activityNavigator = activityNavigator;
    this.fragmentNavigator = fragmentNavigator;
    this.marketName = marketName;
    this.detailsRelay = detailsRelay;
    this.customTabsNavigator = customTabsNavigator;
    this.customTabsToolbarColor = customTabsToolbarColor;
  }

  public void navigateToCustomerAuthenticationForResult(int requestCode) {
    fragmentNavigator.navigateForResult(PaymentLoginFragment.newInstance(), requestCode, true);
  }

  public Observable<Boolean> customerAuthenticationResults(int requestCode) {
    return fragmentNavigator.results(requestCode)
        .map(result -> result.getResultCode() == Activity.RESULT_OK);
  }

  public void navigateToTransactionAuthorizationView(String merchantName, PaymentService service,
      String sku) {

    final Bundle bundle = getAuthorizationBundle(merchantName, sku, service.getType());

    switch (service.getType()) {
      case PaymentServiceMapper.PAYPAL:
        fragmentNavigator.navigateTo(PayPalAuthorizationFragment.create(bundle), true);
        break;
      case PaymentServiceMapper.ADYEN:
        fragmentNavigator.navigateTo(AdyenAuthorizationFragment.create(bundle), true);
        break;
      case PaymentServiceMapper.MOL_POINTS:
      case PaymentServiceMapper.BOA_COMPRA:
      case PaymentServiceMapper.BOA_COMPRA_GOLD:
        fragmentNavigator.navigateTo(WebAuthorizationFragment.create(bundle), true);
        break;
      case PaymentServiceMapper.SANDBOX:
      default:
        throw new IllegalArgumentException(service.getType()
            + " does not require authorization. Can not navigate to authorization view.");
    }
  }

  public void popView() {
    fragmentNavigator.popBackStack();
  }

  public void navigateToPayPalForResult(int requestCode, String currency, String description,
      double amount) {

    final Bundle bundle = new Bundle();
    bundle.putParcelable(PayPalService.EXTRA_PAYPAL_CONFIGURATION,
        new PayPalConfiguration().environment(BuildConfig.PAYPAL_ENVIRONMENT)
            .clientId(BuildConfig.PAYPAL_KEY)
            .merchantName(marketName));
    bundle.putParcelable(com.paypal.android.sdk.payments.PaymentActivity.EXTRA_PAYMENT,
        new PayPalPayment(new BigDecimal(amount), currency, description,
            PayPalPayment.PAYMENT_INTENT_SALE));

    activityNavigator.navigateForResult(com.paypal.android.sdk.payments.PaymentActivity.class,
        requestCode, bundle);
  }

  public Observable<PayPalResult> payPalResults(int requestCode) {
    return activityNavigator.results(requestCode)
        .map(result -> map(result));
  }

  public void popViewWithResult(Purchase purchase) {
    activityNavigator.navigateBackWithResult(Activity.RESULT_OK, bundleMapper.map(purchase));
  }

  public void popViewWithResult(Throwable throwable) {
    activityNavigator.navigateBackWithResult(Activity.RESULT_CANCELED, bundleMapper.map(throwable));
  }

  public void popViewWithResult() {
    activityNavigator.navigateBackWithResult(Activity.RESULT_CANCELED,
        bundleMapper.mapCancellation());
  }

  private Bundle getAuthorizationBundle(String merchantName, String sku, String serviceName) {
    final Bundle bundle = new Bundle();
    bundle.putString(PaymentActivity.EXTRA_SKU, sku);
    bundle.putString(PaymentActivity.EXTRA_MERCHANT_NAME, merchantName);
    bundle.putString(PaymentActivity.EXTRA_SERVICE_NAME, serviceName);
    return bundle;
  }

  private PayPalResult map(Result result) {
    switch (result.getResultCode()) {
      case Activity.RESULT_OK:
        final PaymentConfirmation confirmation = result.getData()
            .getParcelableExtra(
                com.paypal.android.sdk.payments.PaymentActivity.EXTRA_RESULT_CONFIRMATION);
        if (confirmation != null && confirmation.getProofOfPayment() != null) {
          return new BillingNavigator.PayPalResult(BillingNavigator.PayPalResult.SUCCESS,
              confirmation.getProofOfPayment()
                  .getPaymentId());
        } else {
          return new BillingNavigator.PayPalResult(BillingNavigator.PayPalResult.ERROR, null);
        }
      case Activity.RESULT_CANCELED:
        return new BillingNavigator.PayPalResult(BillingNavigator.PayPalResult.CANCELLED, null);
      case PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID:
      default:
        return new BillingNavigator.PayPalResult(BillingNavigator.PayPalResult.ERROR, null);
    }
  }

  public void navigateToAdyenCreditCardView(PaymentRequest paymentRequest) {
    fragmentNavigator.navigateTo(
        new CreditCardFragmentBuilder().setPaymentMethod(paymentRequest.getPaymentMethod())
            .setPublicKey(paymentRequest.getPublicKey())
            .setGenerationtime(paymentRequest.getGenerationTime())
            .setAmount(paymentRequest.getAmount())
            .setShopperReference(paymentRequest.getShopperReference())
            .setCVCFieldStatus(CreditCardFragmentBuilder.CvcFieldStatus.REQUIRED)
            .setCreditCardInfoListener(details -> detailsRelay.call(details))
            .build(), false);
  }

  public Observable<CreditCardPaymentDetails> adyenResults() {
    return detailsRelay;
  }

  public void popToPaymentView() {
    fragmentNavigator.cleanBackStack();
  }

  public void navigateToUriForResult(String redirectUrl) {
    customTabsNavigator.navigateToCustomTabs(
        new CustomTabsIntent.Builder().setToolbarColor(customTabsToolbarColor)
            .build(), Uri.parse(redirectUrl));
  }

  public Observable<Uri> uriResults() {
    return customTabsNavigator.customTabResults();
  }

  public void popAdyenCreditCardView() {
    fragmentNavigator.popBackStack();
  }

  public static class PayPalResult {

    public static final int SUCCESS = 0;
    public static final int ERROR = 1;
    public static final int CANCELLED = 2;

    private final int status;
    private final String paymentConfirmationId;

    public PayPalResult(int status, String paymentConfirmationId) {
      this.status = status;
      this.paymentConfirmationId = paymentConfirmationId;
    }

    public int getStatus() {
      return status;
    }

    public String getPaymentConfirmationId() {
      return paymentConfirmationId;
    }
  }
}