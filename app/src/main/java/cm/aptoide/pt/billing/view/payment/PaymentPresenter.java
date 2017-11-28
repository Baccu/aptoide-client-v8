/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 29/08/2016.
 */

package cm.aptoide.pt.billing.view.payment;

import cm.aptoide.pt.billing.Billing;
import cm.aptoide.pt.billing.BillingAnalytics;
import cm.aptoide.pt.billing.exception.ServiceNotAuthorizedException;
import cm.aptoide.pt.billing.payment.Payment;
import cm.aptoide.pt.billing.view.BillingNavigator;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import java.io.IOException;
import java.util.Set;
import rx.Completable;
import rx.android.schedulers.AndroidSchedulers;

public class PaymentPresenter implements Presenter {

  private final Set<String> purchaseErrorShown;
  private final PaymentView view;
  private final Billing billing;
  private final BillingNavigator navigator;
  private final BillingAnalytics analytics;
  private final String merchantName;
  private final String sku;
  private final String payload;

  public PaymentPresenter(PaymentView view, Billing billing, BillingNavigator navigator,
      BillingAnalytics analytics, String merchantName, String sku, String payload,
      Set<String> purchaseErrorShown) {
    this.view = view;
    this.billing = billing;
    this.navigator = navigator;
    this.analytics = analytics;
    this.merchantName = merchantName;
    this.sku = sku;
    this.payload = payload;
    this.purchaseErrorShown = purchaseErrorShown;
  }

  @Override public void present() {

    onViewCreatedShowPayment();

    handleSelectServiceEvent();

    handleCancelEvent();

    handleBuyEvent();
  }

  private void onViewCreatedShowPayment() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.RESUME))
        .doOnNext(__ -> view.showPaymentLoading())
        .flatMap(loading -> billing.getPayment(sku)
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE)))
        .observeOn(AndroidSchedulers.mainThread())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(payment -> {
          if (!payment.getCustomer()
              .isAuthenticated()) {
            navigator.navigateToCustomerAuthenticationView(merchantName, sku, payload);
          } else {
            if (payment.isNew() || payment.isPendingAuthorization()) {
              view.showMerchant(merchantName);
              view.showProduct(payment.getProduct());
              view.showPayments(payment.getPaymentServices(), payment.getSelectedPaymentService());
              view.hidePaymentLoading();
            }

            if (payment.isProcessing()) {
              view.showPaymentLoading();
            }

            if (payment.isCompleted()) {
              analytics.sendPaymentSuccessEvent();
              navigator.popViewWithResult(payment.getPurchase());
            }

            if (payment.isFailed() && !purchaseErrorShown.contains(payment.getTransaction()
                .getId())) {
              purchaseErrorShown.add(payment.getTransaction()
                  .getId());
              view.hidePaymentLoading();
              view.showUnknownError();
              analytics.sendPaymentErrorEvent();
            }
          }
        }, throwable -> navigator.popViewWithResult(throwable));
  }

  private void handleCancelEvent() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.RESUME.equals(event))
        .flatMap(__ -> view.cancelEvent()
            .flatMap(___ -> billing.getPayment(sku)
                .first())
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE)))
        .observeOn(AndroidSchedulers.mainThread())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(payment -> {
          analytics.sendPaymentViewCancelEvent(payment);
          navigator.popViewWithResult();
        }, throwable -> navigator.popViewWithResult());
  }

  private void handleSelectServiceEvent() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.RESUME.equals(event))
        .flatMap(created -> view.selectServiceEvent()
            .flatMapCompletable(serviceId -> billing.selectService(serviceId))
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE)))
        .observeOn(AndroidSchedulers.mainThread())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> navigator.popViewWithResult(throwable));
  }

  private void handleBuyEvent() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.RESUME.equals(event))
        .flatMap(__ -> view.buyEvent()
            .doOnNext(___ -> view.showBuyLoading())
            .flatMap(___ -> billing.getPayment(sku)
                .first())
            .doOnNext(payment -> analytics.sendPaymentViewBuyEvent(payment))
            .flatMapCompletable(payment -> billing.processPayment(sku, payload)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> {
                  analytics.sendAuthorizationSuccessEvent(payment);
                  view.hideBuyLoading();
                })
                .onErrorResumeNext(throwable -> navigateToAuthorizationView(payment, throwable)))
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError(throwable -> {
              view.hideBuyLoading();
              showError(throwable);
            })
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE))
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> navigator.popViewWithResult(throwable));
  }

  private Completable navigateToAuthorizationView(Payment payment, Throwable throwable) {
    if (throwable instanceof ServiceNotAuthorizedException) {
      navigator.navigateToTransactionAuthorizationView(merchantName,
          payment.getSelectedPaymentService(), sku);
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
