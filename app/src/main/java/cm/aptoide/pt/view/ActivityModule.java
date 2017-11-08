package cm.aptoide.pt.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.PageViewsAnalytics;
import cm.aptoide.pt.R;
import cm.aptoide.pt.account.view.AccountErrorMapper;
import cm.aptoide.pt.account.view.AccountNavigator;
import cm.aptoide.pt.actions.PermissionManager;
import cm.aptoide.pt.analytics.NavigationTracker;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.database.accessors.StoreAccessor;
import cm.aptoide.pt.download.DownloadFactory;
import cm.aptoide.pt.install.AutoUpdate;
import cm.aptoide.pt.install.InstallCompletedNotifier;
import cm.aptoide.pt.install.InstallManager;
import cm.aptoide.pt.install.installer.RootInstallationRetryHandler;
import cm.aptoide.pt.navigator.ActivityNavigator;
import cm.aptoide.pt.navigator.FragmentNavigator;
import cm.aptoide.pt.navigator.FragmentResultNavigator;
import cm.aptoide.pt.navigator.Result;
import cm.aptoide.pt.navigator.TabNavigator;
import cm.aptoide.pt.notification.ContentPuller;
import cm.aptoide.pt.notification.NotificationSyncScheduler;
import cm.aptoide.pt.orientation.ScreenOrientationManager;
import cm.aptoide.pt.presenter.MainPresenter;
import cm.aptoide.pt.presenter.MainView;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.repository.StoreRepository;
import cm.aptoide.pt.store.StoreUtilsProxy;
import cm.aptoide.pt.util.ApkFy;
import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.jakewharton.rxrelay.PublishRelay;
import dagger.Module;
import dagger.Provides;
import java.util.Map;
import javax.inject.Named;

import static android.content.Context.WINDOW_SERVICE;

@Module public class ActivityModule {

  private final AppCompatActivity activity;
  private final Intent intent;
  private final String marketName;
  private final String autoUpdateUrl;
  private final View view;
  private final String defaultTheme;
  private final String defaultStoreName;
  private boolean firstCreated;

  public ActivityModule(AppCompatActivity activity, Intent intent, String marketName,
      String autoUpdateUrl, View view, String defaultTheme, String defaultStoreName,
      boolean firstCreated) {
    this.activity = activity;
    this.intent = intent;
    this.marketName = marketName;
    this.autoUpdateUrl = autoUpdateUrl;
    this.view = view;
    this.firstCreated = firstCreated;
    this.defaultTheme = defaultTheme;
    this.defaultStoreName = defaultStoreName;
  }

  @ActivityScope @Provides ApkFy provideApkFy(
      @Named("secure") SharedPreferences securePreferences) {
    return new ApkFy(activity, intent, securePreferences);
  }

  @ActivityScope @Provides AutoUpdate provideAutoUpdate(
      @Named("default") InstallManager defaultInstallManager, DownloadFactory downloadFactory,
      PermissionManager permissionManager, Resources resources) {
    return new AutoUpdate((ActivityView) activity, downloadFactory, permissionManager,
        defaultInstallManager, resources, autoUpdateUrl, R.mipmap.ic_launcher, false, marketName);
  }

  @ActivityScope @Provides FragmentNavigator provideFragmentNavigator(
      Map<Integer, Result> fragmentResultMap,
      BehaviorRelay<Map<Integer, Result>> fragmentResultRelay, FragmentManager fragmentManager) {
    return new FragmentResultNavigator(fragmentManager, R.id.fragment_placeholder,
        android.R.anim.fade_in, android.R.anim.fade_out, fragmentResultMap, fragmentResultRelay);
  }

  @ActivityScope @Provides FragmentManager provideFragmentManager() {
    return activity.getSupportFragmentManager();
  }

  @ActivityScope @Provides DeepLinkManager provideDeepLinkManager(StoreUtilsProxy storeUtilsProxy,
      StoreRepository storeRepository, FragmentNavigator fragmentNavigator,
      @Named("default") SharedPreferences sharedPreferences, StoreAccessor storeAccessor,
      NavigationTracker navigationTracker, PageViewsAnalytics pageViewsAnalytics) {
    return new DeepLinkManager(storeUtilsProxy, storeRepository, fragmentNavigator,
        (TabNavigator) activity, (DeepLinkManager.DeepLinkMessages) activity, sharedPreferences,
        storeAccessor, defaultTheme, defaultStoreName, navigationTracker, pageViewsAnalytics);
  }

  @ActivityScope @Provides Presenter provideMainPresenter(
      @Named("default") InstallManager installManager,
      RootInstallationRetryHandler rootInstallationRetryHandler, ApkFy apkFy, AutoUpdate autoUpdate,
      NotificationSyncScheduler notificationSyncScheduler,
      @Named("default") SharedPreferences sharedPreferences,
      @Named("secure") SharedPreferences secureSharedPreferences,
      FragmentNavigator fragmentNavigator, DeepLinkManager deepLinkManager) {
    return new MainPresenter((MainView) view, installManager, rootInstallationRetryHandler,
        CrashReport.getInstance(), apkFy, autoUpdate, new ContentPuller(activity),
        notificationSyncScheduler,
        new InstallCompletedNotifier(PublishRelay.create(), installManager,
            CrashReport.getInstance()), sharedPreferences, secureSharedPreferences,
        fragmentNavigator, deepLinkManager, defaultStoreName, defaultTheme, firstCreated);
  }

  @ActivityScope @Provides AccountNavigator provideAccountNavigator(FragmentNavigator fragmentNavigator, AptoideAccountManager accountManager,
      CallbackManager callbackManager, GoogleApiClient googleApiClient){
    return new AccountNavigator(fragmentNavigator, accountManager,
        ((ActivityNavigator) activity), LoginManager.getInstance(), callbackManager, googleApiClient,
        PublishRelay.create(),defaultStoreName, defaultTheme, "http://m.aptoide.com/account/password-recovery");
  }

  @Provides @ActivityScope AccountErrorMapper provideAccountErrorMapper(){
    return new AccountErrorMapper(activity);
  }

  @ActivityScope @Provides ScreenOrientationManager provideScreenOrientationManager(){
    return new ScreenOrientationManager(activity, (WindowManager) activity.getSystemService(WINDOW_SERVICE));
  }

}