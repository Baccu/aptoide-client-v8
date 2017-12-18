package cm.aptoide.pt.billing.authorization;

import java.util.List;
import rx.Completable;
import rx.Observable;
import rx.Single;

public interface AuthorizationPersistence {

  Completable saveAuthorization(Authorization authorization);

  Single<Authorization> createAuthorization(String customerId, String authorizationId,
      Authorization.Status status, String metadata, String paymentMethodType);

  Completable removeAuthorizations(String customerId, String transactionId);

  Observable<List<Authorization>> getAuthorizations(String customerId);

  Completable saveAuthorizations(List<Authorization> authorizations);
}
