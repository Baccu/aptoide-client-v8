/*
 * Copyright (c) 2017.
 * Modified by Marcelo Benites on 17/02/2017.
 */

package cm.aptoide.pt.billing.payment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import cm.aptoide.pt.billing.DefaultPaymentServicePersistence;
import rx.Completable;
import rx.Scheduler;
import rx.Single;

public class SharedPreferencesDefaultPaymentServicePersistence
    implements DefaultPaymentServicePersistence {

  private static final String SELECTED_SERVICE_ID = "SELECTED_SERVICE_ID";
  private final SharedPreferences preferences;
  private final Scheduler scheduler;

  public SharedPreferencesDefaultPaymentServicePersistence(SharedPreferences preferences,
      Scheduler scheduler) {
    this.preferences = preferences;
    this.scheduler = scheduler;
  }

  @Override public Single<String> getDefaultService() {
    return Single.defer(() -> {
      if (preferences.contains(SELECTED_SERVICE_ID)) {
        return Single.just(preferences.getString(SELECTED_SERVICE_ID, null));
      }
      return Single.error(new IllegalStateException("No default service stored."));
    })
        .subscribeOn(scheduler);
  }

  @SuppressLint("ApplySharedPref") @Override
  public Completable saveDefaultService(String serviceId) {
    return Completable.fromAction(() -> preferences.edit()
        .putString(SELECTED_SERVICE_ID, serviceId)
        .commit())
        .subscribeOn(scheduler);
  }
}
