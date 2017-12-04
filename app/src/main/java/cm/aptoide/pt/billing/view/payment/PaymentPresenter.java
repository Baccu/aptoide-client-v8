/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 29/08/2016.
 */

package cm.aptoide.pt.billing.view.payment;

import cm.aptoide.pt.billing.Billing;
import cm.aptoide.pt.billing.BillingAnalytics;
import cm.aptoide.pt.billing.exception.ServiceNotAuthorizedException;
import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.billing.view.BillingNavigator;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import java.io.IOException;
import rx.Completable;
import rx.Scheduler;

public class PaymentPresenter implements Presenter {

  private final PaymentView view;
  private final Billing billing;
  private final BillingNavigator navigator;
  private final BillingAnalytics analytics;
  private final String merchantName;
  private final String sku;
  private final String payload;
  private final Scheduler viewScheduler;

  public PaymentPresenter(PaymentView view, Billing billing, BillingNavigator navigator,
      BillingAnalytics analytics, String merchantName, String sku, String payload,
      Scheduler viewScheduler) {
    this.view = view;
    this.billing = billing;
    this.navigator = navigator;
    this.analytics = analytics;
    this.merchantName = merchantName;
    this.sku = sku;
    this.payload = payload;
    this.viewScheduler = viewScheduler;
  }

  @Override public void present() {

    onViewCreatedShowPayment();

    handleCancelEvent();

    handleBuyEvent();
  }

  private void onViewCreatedShowPayment() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.RESUME))
        .doOnNext(__ -> view.showPaymentLoading())
        .flatMap(loading -> billing.getPayment(sku)
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE)))
        .observeOn(viewScheduler)
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(payment -> {
          if (!payment.getCustomer()
              .isAuthenticated()) {
            navigator.navigateToCustomerAuthenticationView(merchantName, sku, payload);
          } else {
            if (payment.isNew() || payment.isPendingAuthorization()) {
              view.hidePaymentLoading();
            }

            if (payment.isProcessing()) {
              view.showPaymentLoading();
            }

            if (payment.isCompleted()) {
              analytics.sendPaymentSuccessEvent();
              navigator.popViewWithResult(payment.getPurchase());
            }

            if (payment.isFailed()) {
              view.hidePaymentLoading();
              view.showUnknownError();
              analytics.sendPaymentErrorEvent();
            }

            view.showMerchant(payment.getMerchant()
                .getName());
            view.showProduct(payment.getProduct());
            view.showPayments(payment.getPaymentServices());
          }
        }, throwable -> navigator.popViewWithResult(throwable));
  }

  private void handleCancelEvent() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.RESUME.equals(event))
        .flatMap(__ -> view.cancelEvent()
            .flatMap(serviceId -> billing.getPayment(sku)
                .first()
                .doOnNext(payment -> analytics.sendPaymentViewCancelEvent(payment)))
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE)))
        .observeOn(viewScheduler)
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
          navigator.popViewWithResult();
        }, throwable -> navigator.popViewWithResult());
  }

  private void handleBuyEvent() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.CREATE.equals(event))
        .flatMap(__ -> view.buyEvent()
            .doOnNext(___ -> view.showBuyLoading())
            .flatMap(serviceId -> billing.getPayment(sku)
                .first()
                .doOnNext(payment -> analytics.sendPaymentViewBuyEvent(payment))
                .flatMapCompletable(payment -> billing.processPayment(serviceId, sku, payload)
                    .observeOn(viewScheduler)
                .doOnCompleted(() -> {
                  view.hideBuyLoading();
                })
                    .onErrorResumeNext(throwable -> navigateToAuthorizationView(
                        payment.getPaymentService(serviceId), throwable))))
            .observeOn(viewScheduler)
            .doOnError(throwable -> {
              view.hideBuyLoading();
              showError(throwable);
            })
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> navigator.popViewWithResult(throwable));
  }

  private Completable navigateToAuthorizationView(PaymentService selectedService,
      Throwable throwable) {
    if (throwable instanceof ServiceNotAuthorizedException) {
      navigator.navigateToTransactionAuthorizationView(merchantName, selectedService, sku);
      return Completable.complete();
    }
    return Completable.error(throwable);
  }

  private void showError(Throwable throwable) {
    if (throwable instanceof IOException) {
      view.showNetworkError();
    } else {
      view.showUnknownError();
    }
  }
}
