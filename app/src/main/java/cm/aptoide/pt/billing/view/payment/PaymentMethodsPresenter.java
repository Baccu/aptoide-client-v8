package cm.aptoide.pt.billing.view.payment;

import cm.aptoide.pt.billing.Billing;
import cm.aptoide.pt.billing.view.BillingNavigator;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import rx.Scheduler;
import rx.exceptions.OnErrorNotImplementedException;

public class PaymentMethodsPresenter implements Presenter {

  private final PaymentMethodsView view;
  private final Billing billing;
  private final String sku;
  private final Scheduler viewScheduler;
  private final BillingNavigator navigator;
  private final String merchantPackageName;
  private final String payload;

  public PaymentMethodsPresenter(PaymentMethodsView view, Billing billing, String sku,
      Scheduler viewScheduler, BillingNavigator navigator, String merchantPackageName,
      String payload) {
    this.view = view;
    this.billing = billing;
    this.sku = sku;
    this.viewScheduler = viewScheduler;
    this.navigator = navigator;
    this.merchantPackageName = merchantPackageName;
    this.payload = payload;
  }

  @Override public void present() {

    onCreateShowPaymentMethods();

    handlePaymentMethodSelectedEvent();

    handleCancelEvent();
  }

  private void handleCancelEvent() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.RESUME))
        .flatMap(__ -> view.getCancelEvent()
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE)))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(paymentMethod -> {
          navigator.popViewWithResult();
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void handlePaymentMethodSelectedEvent() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.RESUME))
        .flatMap(__ -> view.getSelectedPaymentMethodEvent()
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE)))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(paymentMethod -> {
          navigator.navigateToAuthorizationView(merchantPackageName, paymentMethod, sku);
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void onCreateShowPaymentMethods() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.RESUME))
        .doOnNext(__ -> view.showLoading())
        .flatMap(__ -> billing.getPayment(sku)
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE)))
        .observeOn(viewScheduler)
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(payment -> {

          if (payment.getAuthorizations()
              .isEmpty()) {
            if (payment.getPaymentServices()
                .isEmpty()) {
              view.showNoPaymentMethodsAvailableMessage();
            } else {
              view.showAvailablePaymentMethods(payment.getPaymentServices());
            }
            view.hideLoading();
          } else {
            navigator.navigateToPaymentView(merchantPackageName, sku, payload);
          }
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }
}
