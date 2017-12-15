package cm.aptoide.pt.billing.authorization;

import java.util.List;
import rx.Completable;
import rx.Observable;
import rx.Single;

public interface AuthorizationPersistence {

  Completable saveAuthorization(Authorization authorization);

  Observable<Authorization> getAuthorization(String customerId, String transactionId);

  Single<Authorization> updateAuthorization(String customerId, String authorizationId,
      Authorization.Status status, String metadata);

  Single<Authorization> createAuthorization(String customerId, String transactionId,
      Authorization.Status status);

  Completable removeAuthorizations(String customerId, String transactionId);

  Observable<List<Authorization>> getAuthorizations(String customerId);

  Completable saveAuthorizations(List<Authorization> authorizations);
}
