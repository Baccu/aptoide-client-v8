/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 22/11/2016.
 */

package cm.aptoide.pt.billing.authorization;

import rx.Completable;
import rx.Single;

public class AuthorizationRepository {

  private final AuthorizationPersistence authorizationPersistence;

  public AuthorizationRepository(AuthorizationPersistence authorizationPersistence) {
    this.authorizationPersistence = authorizationPersistence;
  }

  public Single<Authorization> createAuthorization(String customerId, String paymentMethodId,
      String paymentMethodType, String metadata, Authorization.Status status) {
    return authorizationPersistence.createAuthorization(customerId, paymentMethodId, status,
        metadata, paymentMethodType);
  }

  public Completable removeAuthorization(String customerId, String transactionId) {
    return authorizationPersistence.removeAuthorizations(customerId, transactionId);
  }
}