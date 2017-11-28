package cm.aptoide.pt.billing.view.login;

import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.account.AccountAnalytics;
import cm.aptoide.pt.account.FacebookSignUpAdapter;
import cm.aptoide.pt.account.FacebookSignUpException;
import cm.aptoide.pt.account.GoogleSignUpAdapter;
import cm.aptoide.pt.account.view.AccountNavigator;
import cm.aptoide.pt.billing.BillingAnalytics;
import cm.aptoide.pt.billing.view.BillingNavigator;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.orientation.ScreenOrientationManager;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.view.ThrowableToStringMapper;
import java.util.Collection;
import rx.Observable;
import rx.Scheduler;
import rx.exceptions.OnErrorNotImplementedException;

public class PaymentLoginPresenter implements Presenter {

  private static final int RESOLVE_GOOGLE_CREDENTIALS_REQUEST_CODE = 2;
  private final PaymentLoginView view;
  private final AccountNavigator accountNavigator;
  private final Collection<String> permissions;
  private final Collection<String> requiredPermissions;
  private final AptoideAccountManager accountManager;
  private final CrashReport crashReport;
  private final ThrowableToStringMapper errorMapper;
  private final Scheduler viewScheduler;
  private final ScreenOrientationManager orientationManager;
  private final AccountAnalytics accountAnalytics;
  private final BillingAnalytics billingAnalytics;
  private final BillingNavigator billingNavigator;
  private final String merchantName;
  private final String sku;
  private final String payload;

  public PaymentLoginPresenter(PaymentLoginView view, Collection<String> permissions,
      AccountNavigator accountNavigator, Collection<String> requiredPermissions,
      AptoideAccountManager accountManager, CrashReport crashReport,
      ThrowableToStringMapper errorMapper, Scheduler viewScheduler,
      ScreenOrientationManager orientationManager, AccountAnalytics accountAnalytics,
      BillingAnalytics billingAnalytics, BillingNavigator billingNavigator, String merchantName,
      String sku, String payload) {
    this.view = view;
    this.accountNavigator = accountNavigator;
    this.permissions = permissions;
    this.requiredPermissions = requiredPermissions;
    this.accountManager = accountManager;
    this.crashReport = crashReport;
    this.errorMapper = errorMapper;
    this.viewScheduler = viewScheduler;
    this.orientationManager = orientationManager;
    this.accountAnalytics = accountAnalytics;
    this.billingAnalytics = billingAnalytics;
    this.billingNavigator = billingNavigator;
    this.merchantName = merchantName;
    this.sku = sku;
    this.payload = payload;
  }

  @Override public void present() {

    onViewCreatedCheckLoginStatus();

    handleCancelEvent();

    handleFacebookSignUpResult();

    handleFacebookSignUpEvent();

    handleGrantFacebookRequiredPermissionsEvent();

    handleGoogleSignUpResult();

    handleGoogleSignUpEvent();

    handleRecoverPasswordEvent();

    handleAptoideLoginEvent();

    handleAptoideSignUpEvent();
  }

  private void handleGrantFacebookRequiredPermissionsEvent() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.grantFacebookRequiredPermissionsEvent())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
          view.showLoading();
          accountNavigator.navigateToFacebookSignUpForResult(requiredPermissions);
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void onViewCreatedCheckLoginStatus() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.RESUME))
        .flatMap(__ -> accountManager.accountStatus()
            .filter(account -> account.isLoggedIn())
            .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE)))
        .observeOn(viewScheduler)
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
          accountAnalytics.loginSuccess();
          billingAnalytics.sendCustomerAuthenticatedEvent(true);
          billingNavigator.navigateToPaymentView(merchantName, sku, payload);
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void handleAptoideSignUpEvent() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(event -> view.aptoideSignUpEvent()
            .doOnNext(__ -> {
              view.showLoading();
              orientationManager.lock();
              accountAnalytics.sendAptoideSignUpButtonPressed();
            })
            .flatMapCompletable(
                result -> accountManager.signUp(AptoideAccountManager.APTOIDE_SIGN_UP_TYPE, result)
                    .observeOn(viewScheduler)
                    .doOnTerminate(() -> {
                      view.hideLoading();
                      orientationManager.unlock();
                    })
                    .doOnError(throwable -> {
                      accountAnalytics.sendSignUpErrorEvent(AccountAnalytics.LoginMethod.APTOIDE,
                          throwable);
                      view.showError(errorMapper.map(throwable));
                      crashReport.log(throwable);
                    }))
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe();
  }

  private void handleAptoideLoginEvent() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(event -> view.aptoideLoginEvent()
            .doOnNext(__ -> {
              view.showLoading();
              orientationManager.lock();
              accountAnalytics.sendAptoideLoginButtonPressed();
            })
            .flatMapCompletable(result -> accountManager.login(result)
                .observeOn(viewScheduler)
                .doOnTerminate(() -> {
                  view.hideLoading();
                  orientationManager.unlock();
                })
                .doOnError(throwable -> {
                  accountAnalytics.sendLoginErrorEvent(AccountAnalytics.LoginMethod.APTOIDE,
                      throwable);
                  view.showError(errorMapper.map(throwable));
                  crashReport.log(throwable);
                }))
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe();
  }

  private void handleFacebookSignUpResult() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> accountNavigator.facebookSignUpResults()
            .flatMapCompletable(result -> accountManager.signUp(FacebookSignUpAdapter.TYPE, result)
                .observeOn(viewScheduler)
                .doOnTerminate(() -> view.hideLoading())
                .doOnError(throwable -> {

                  if (throwable instanceof FacebookSignUpException
                      && ((FacebookSignUpException) throwable).getCode()
                      == FacebookSignUpException.MISSING_REQUIRED_PERMISSIONS) {
                    view.showFacebookPermissionsRequiredError();
                  } else {
                    view.showError(errorMapper.map(throwable));
                    crashReport.log(throwable);
                  }
                  accountAnalytics.sendLoginErrorEvent(AccountAnalytics.LoginMethod.FACEBOOK,
                      throwable);
                }))
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe();
  }

  public void handleFacebookSignUpEvent() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.facebookSignUpEvent())
        .doOnNext(__ -> view.showLoading())
        .doOnNext(click -> accountAnalytics.sendFacebookLoginButtonPressed())
        .doOnNext(__ -> accountNavigator.navigateToFacebookSignUpForResult(permissions))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe();
  }

  private void handleGoogleSignUpEvent() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.googleSignUpEvent())
        .doOnNext(event -> view.showLoading())
        .doOnNext(event -> accountAnalytics.sendGoogleLoginButtonPressed())
        .flatMapSingle(event -> accountNavigator.navigateToGoogleSignUpForResult(
            RESOLVE_GOOGLE_CREDENTIALS_REQUEST_CODE))
        .observeOn(viewScheduler)
        .doOnNext(connectionResult -> {
          if (!connectionResult.isSuccess()) {
            view.showConnectionError(connectionResult);
            view.hideLoading();
          }
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, err -> {
          view.hideLoading();
          view.showError(errorMapper.map(err));
          crashReport.log(err);
        });
  }

  private void handleGoogleSignUpResult() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> accountNavigator.googleSignUpResults(RESOLVE_GOOGLE_CREDENTIALS_REQUEST_CODE)
            .flatMapCompletable(result -> accountManager.signUp(GoogleSignUpAdapter.TYPE, result)
                .observeOn(viewScheduler)
                .doOnTerminate(() -> view.hideLoading())
                .doOnError(throwable -> {
                  view.showError(errorMapper.map(throwable));
                  crashReport.log(throwable);
                  accountAnalytics.sendLoginErrorEvent(AccountAnalytics.LoginMethod.GOOGLE,
                      throwable);
                }))
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe();
  }

  private void handleCancelEvent() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> Observable.merge(view.backButtonEvent(), view.upNavigationEvent()))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
          billingAnalytics.sendCustomerAuthenticatedEvent(false);
          billingNavigator.popViewWithResult();
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void handleRecoverPasswordEvent() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.recoverPasswordEvent())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> accountNavigator.navigateToRecoverPasswordView(), throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }
}
