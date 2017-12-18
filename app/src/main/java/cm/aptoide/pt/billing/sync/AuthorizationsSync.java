package cm.aptoide.pt.billing.sync;

import cm.aptoide.pt.billing.authorization.AuthorizationPersistence;
import cm.aptoide.pt.billing.authorization.AuthorizationService;
import cm.aptoide.pt.billing.authorization.MetadataAuthorization;
import cm.aptoide.pt.sync.Sync;
import rx.Completable;

public class AuthorizationsSync extends Sync {

  private final String customerId;
  private final AuthorizationService authorizationService;
  private final AuthorizationPersistence authorizationPersistence;

  public AuthorizationsSync(String id, String customerId, AuthorizationService authorizationService,
      AuthorizationPersistence authorizationPersistence, boolean periodic, boolean exact,
      long interval, long trigger) {
    super(id, periodic, exact, trigger, interval);
    this.customerId = customerId;
    this.authorizationService = authorizationService;
    this.authorizationPersistence = authorizationPersistence;
  }

  @Override public Completable execute() {
    return syncRemoteAuthorizations().andThen(syncLocalAuthorizations());
  }

  private Completable syncRemoteAuthorizations() {
    return authorizationService.getAuthorizations(customerId)
        .flatMapCompletable(authorizations -> authorizationPersistence.saveAuthorizations(authorizations));
  }

  public Completable syncLocalAuthorizations() {
    return authorizationPersistence.getAuthorizations(customerId)
        .first()
        .flatMapIterable(authorizations -> authorizations)
        .filter(authorization -> authorization instanceof MetadataAuthorization)
        .cast(MetadataAuthorization.class)
        .filter(authorization -> authorization.isPendingSync())
        .flatMapSingle(authorization -> authorizationService.createAuthorization(customerId,
            authorization.getPaymentMethodId(), authorization.getMetadata()))
        .toList()
        .flatMapCompletable(
            authorizations -> authorizationPersistence.saveAuthorizations(authorizations))
        .toCompletable();
  }
}
