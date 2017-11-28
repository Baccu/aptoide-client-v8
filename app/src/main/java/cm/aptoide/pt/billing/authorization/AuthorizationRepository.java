/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 22/11/2016.
 */

package cm.aptoide.pt.billing.authorization;

import cm.aptoide.pt.billing.BillingSyncScheduler;
import rx.Completable;
import rx.Observable;
import rx.Single;

public class AuthorizationRepository {

  private final BillingSyncScheduler syncScheduler;
  private final AuthorizationPersistence authorizationPersistence;

  public AuthorizationRepository(BillingSyncScheduler syncScheduler,
      AuthorizationPersistence authorizationPersistence) {
    this.authorizationPersistence = authorizationPersistence;
    this.syncScheduler = syncScheduler;
  }

  public Observable<Authorization> getAuthorization(String customerId, String transactionId) {
    return authorizationPersistence.getAuthorization(customerId, transactionId)
        .doOnSubscribe(() -> syncScheduler.syncAuthorization(transactionId));
  }

  public Completable updateAuthorization(String customerId, String authorizationId, String metadata,
      Authorization.Status status) {
    return authorizationPersistence.updateAuthorization(customerId, authorizationId, status,
        metadata)
        .toCompletable();
  }

  public Single<Authorization> createAuthorization(String customerId, String transactionId,
      Authorization.Status status) {
    return authorizationPersistence.createAuthorization(customerId, transactionId, status);
  }

  public Completable removeAuthorization(String customerId, String transactionId) {
    return authorizationPersistence.removeAuthorizations(customerId, transactionId)
        .doOnSubscribe(__ -> syncScheduler.cancelAuthorizationSync(transactionId));
  }
}