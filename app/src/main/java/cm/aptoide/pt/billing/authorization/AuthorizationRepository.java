/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 22/11/2016.
 */

package cm.aptoide.pt.billing.authorization;

import cm.aptoide.pt.billing.BillingSyncScheduler;
import java.util.List;
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
    return Completable.fromAction(() -> syncScheduler.syncAuthorization(transactionId))
        .andThen(authorizationPersistence.getAuthorization(customerId, transactionId));
  }

  public Single<Authorization> updateAuthorization(String customerId, String authorizationId, String metadata,
      Authorization.Status status) {
    return authorizationPersistence.updateAuthorization(customerId, authorizationId, status,
        metadata);
  }

  public Single<Authorization> createAuthorization(String customerId, String transactionId,
      Authorization.Status status) {
    return authorizationPersistence.createAuthorization(customerId, transactionId, status);
  }

  public Completable removeAuthorization(String customerId, String transactionId) {
    return Completable.fromAction(() -> syncScheduler.cancelAuthorizationSync(transactionId))
        .andThen(authorizationPersistence.removeAuthorizations(customerId, transactionId));
  }

  public Observable<List<Authorization>> getAuthorizations(String customerId) {
    return Completable.fromAction(() -> syncScheduler.syncAuthorizations(customerId))
        .andThen(authorizationPersistence.getAuthorizations(customerId));
  }
}