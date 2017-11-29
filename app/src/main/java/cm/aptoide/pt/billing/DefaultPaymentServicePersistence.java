package cm.aptoide.pt.billing;

import rx.Completable;
import rx.Single;

public interface DefaultPaymentServicePersistence {

  Single<String> getDefaultService();

  Completable saveDefaultService(String serviceId);
}
